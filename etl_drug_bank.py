"""
DrugBank XML → PostgreSQL ETL
==============================
Single-pass iterative parse (lxml.iterparse).
Uses your existing DatabaseConnection pool (Amazon RDS, .env credentials).

Populates three tables inside drugbank_schema:
  drugbank_schema.drug_vocabulary      — RxCUI, name, synonyms, groups
  drugbank_schema.drug_interactions    — DDI with inferred severity
  drugbank_schema.drug_disease         — indications + MeSH terms

Usage:
    python drugbank_etl.py --xml /path/to/full_database.xml

Requirements:
    pip install lxml psycopg2-binary tqdm python-dotenv
    (db_connection.py must be on the Python path)

.env keys expected:
    DB_HOST, DB_NAME, DB_USER, DB_PASSWORD, DB_PORT (default 5432)
"""

import argparse
import os
import re
import time

from dotenv import load_dotenv
from lxml import etree
from psycopg2.extras import execute_batch
from tqdm import tqdm

from db_connection import DatabaseConnection, get_connection, return_connection

# ---------------------------------------------------------------------------
# Bootstrap — pool init from .env (same pattern as your RxNorm ETL)
# ---------------------------------------------------------------------------
load_dotenv()

DatabaseConnection.initialize_pool(
    host=os.getenv("DB_HOST"),
    database=os.getenv("DB_NAME"),
    user=os.getenv("DB_USER"),
    password=os.getenv("DB_PASSWORD"),
    port=int(os.getenv("DB_PORT", 5432)),
)

# ---------------------------------------------------------------------------
# DrugBank XML namespace
# ---------------------------------------------------------------------------
NS = "http://www.drugbank.ca"
T  = lambda tag: f"{{{NS}}}{tag}"

# ---------------------------------------------------------------------------
# DDL — idempotent, safe to re-run
# ---------------------------------------------------------------------------
DDL = """
CREATE SCHEMA IF NOT EXISTS drugbank_schema;

CREATE TABLE IF NOT EXISTS drugbank_schema.drug_vocabulary (
    drugbank_id  TEXT PRIMARY KEY,
    name         TEXT NOT NULL,
    rxcui        TEXT,
    drug_type    TEXT,
    state        TEXT,
    synonyms     TEXT[],
    groups       TEXT[]
);

CREATE INDEX IF NOT EXISTS idx_dv_rxcui ON drugbank_schema.drug_vocabulary (rxcui);
CREATE INDEX IF NOT EXISTS idx_dv_name  ON drugbank_schema.drug_vocabulary (lower(name));

CREATE TABLE IF NOT EXISTS drugbank_schema.drug_interactions (
    id           SERIAL PRIMARY KEY,
    drug1_db_id  TEXT NOT NULL,
    drug1_rxcui  TEXT,
    drug2_db_id  TEXT NOT NULL,
    drug2_rxcui  TEXT,
    drug2_name   TEXT,
    severity     TEXT,
    description  TEXT
);

CREATE INDEX IF NOT EXISTS idx_ddi_d1rxcui ON drugbank_schema.drug_interactions (drug1_rxcui);
CREATE INDEX IF NOT EXISTS idx_ddi_d2rxcui ON drugbank_schema.drug_interactions (drug2_rxcui);
CREATE INDEX IF NOT EXISTS idx_ddi_d1db    ON drugbank_schema.drug_interactions (drug1_db_id);

CREATE TABLE IF NOT EXISTS drugbank_schema.drug_disease (
    id               SERIAL PRIMARY KEY,
    drugbank_id      TEXT NOT NULL,
    rxcui            TEXT,
    drug_name        TEXT,
    indication_text  TEXT,
    mesh_terms       TEXT[]
);

CREATE INDEX IF NOT EXISTS idx_dd_rxcui ON drugbank_schema.drug_disease (rxcui);
CREATE INDEX IF NOT EXISTS idx_dd_dbid  ON drugbank_schema.drug_disease (drugbank_id);
"""

BACKFILL_SQL = """
UPDATE drugbank_schema.drug_interactions ddi
SET    drug2_rxcui = dv.rxcui
FROM   drugbank_schema.drug_vocabulary dv
WHERE  ddi.drug2_db_id  = dv.drugbank_id
  AND  ddi.drug2_rxcui IS NULL
  AND  dv.rxcui        IS NOT NULL;
"""

# ---------------------------------------------------------------------------
# Severity inference (DrugBank has no structured severity field)
# ---------------------------------------------------------------------------
_HIGH     = re.compile(r'\b(life.?threatening|fatal|severe|serious|major)\b', re.I)
_MODERATE = re.compile(r'\b(moderate|significant|increase.{0,30}risk)\b', re.I)
_LOW      = re.compile(r'\b(minor|mild|slight|small)\b', re.I)

def infer_severity(desc: str) -> str:
    if not desc:
        return "UNKNOWN"
    if _HIGH.search(desc):     return "HIGH"
    if _MODERATE.search(desc): return "MODERATE"
    if _LOW.search(desc):      return "LOW"
    return "UNKNOWN"

# ---------------------------------------------------------------------------
# XML helpers
# ---------------------------------------------------------------------------
def _text(el, tag) -> str:
    child = el.find(T(tag))
    return (child.text or "").strip() if child is not None else ""

# ---------------------------------------------------------------------------
# Per-<drug> extraction
# ---------------------------------------------------------------------------
def parse_drug(drug_el):
    drug_type = drug_el.get("type", "")

    primary_id = ""
    for db_id_el in drug_el.findall(T("drugbank-id")):
        if db_id_el.get("primary") == "true":
            primary_id = (db_id_el.text or "").strip()
            break
    if not primary_id:
        return None, [], None

    name       = _text(drug_el, "name")
    state      = _text(drug_el, "state")
    indication = _text(drug_el, "indication")

    # groups
    groups = []
    grp_el = drug_el.find(T("groups"))
    if grp_el is not None:
        groups = [(g.text or "").strip() for g in grp_el.findall(T("group"))]

    # synonyms
    synonyms = []
    syn_el = drug_el.find(T("synonyms"))
    if syn_el is not None:
        synonyms = [(s.text or "").strip() for s in syn_el.findall(T("synonym")) if s.text]

    # RxCUI
    rxcui = ""
    ext_el = drug_el.find(T("external-identifiers"))
    if ext_el is not None:
        for ext in ext_el.findall(T("external-identifier")):
            if _text(ext, "resource") == "RxCUI":
                rxcui = _text(ext, "identifier")
                break

    # MeSH categories
    mesh_terms = []
    cats_el = drug_el.find(T("categories"))
    if cats_el is not None:
        for cat_el in cats_el.findall(T("category")):
            t = _text(cat_el, "category")
            if t:
                mesh_terms.append(t)

    # DDI rows
    ddi_rows = []
    ddis_el = drug_el.find(T("drug-interactions"))
    if ddis_el is not None:
        for ddi in ddis_el.findall(T("drug-interaction")):
            desc = _text(ddi, "description")
            ddi_rows.append({
                "drug1_db_id": primary_id,
                "drug1_rxcui": rxcui or None,
                "drug2_db_id": _text(ddi, "drugbank-id"),
                "drug2_name":  _text(ddi, "name"),
                "severity":    infer_severity(desc),
                "description": desc,
            })

    vocab_row = {
        "drugbank_id": primary_id,
        "name":        name,
        "rxcui":       rxcui or None,
        "drug_type":   drug_type,
        "state":       state,
        "synonyms":    synonyms,
        "groups":      groups,
    }

    disease_row = {
        "drugbank_id":     primary_id,
        "rxcui":           rxcui or None,
        "drug_name":       name,
        "indication_text": indication or None,
        "mesh_terms":      mesh_terms,
    } if (indication or mesh_terms) else None

    return vocab_row, ddi_rows, disease_row

# ---------------------------------------------------------------------------
# Batch inserters
# ---------------------------------------------------------------------------
def _insert_vocab(cur, rows):
    execute_batch(cur, """
        INSERT INTO drugbank_schema.drug_vocabulary
            (drugbank_id, name, rxcui, drug_type, state, synonyms, groups)
        VALUES
            (%(drugbank_id)s, %(name)s, %(rxcui)s, %(drug_type)s,
             %(state)s, %(synonyms)s, %(groups)s)
        ON CONFLICT (drugbank_id) DO UPDATE SET
            name     = EXCLUDED.name,
            rxcui    = EXCLUDED.rxcui,
            synonyms = EXCLUDED.synonyms,
            groups   = EXCLUDED.groups
    """, rows, page_size=500)


def _insert_ddi(cur, rows):
    execute_batch(cur, """
        INSERT INTO drugbank_schema.drug_interactions
            (drug1_db_id, drug1_rxcui, drug2_db_id, drug2_name, severity, description)
        VALUES
            (%(drug1_db_id)s, %(drug1_rxcui)s, %(drug2_db_id)s,
             %(drug2_name)s,  %(severity)s,    %(description)s)
    """, rows, page_size=500)


def _insert_disease(cur, rows):
    execute_batch(cur, """
        INSERT INTO drugbank_schema.drug_disease
            (drugbank_id, rxcui, drug_name, indication_text, mesh_terms)
        VALUES
            (%(drugbank_id)s, %(rxcui)s, %(drug_name)s,
             %(indication_text)s, %(mesh_terms)s)
    """, rows, page_size=500)

# ---------------------------------------------------------------------------
# Flush helper — gets/returns connection from your pool, rolls back on error
# ---------------------------------------------------------------------------
def _flush(vocab_buf, ddi_buf, disease_buf):
    conn = get_connection()
    try:
        cur = conn.cursor()
        if vocab_buf:   _insert_vocab(cur, vocab_buf)
        if ddi_buf:     _insert_ddi(cur, ddi_buf)
        if disease_buf: _insert_disease(cur, disease_buf)
        conn.commit()
        cur.close()
    except Exception as e:
        conn.rollback()
        raise RuntimeError(f"Flush failed: {e}") from e
    finally:
        return_connection(conn)  # always return to pool, even on error

# ---------------------------------------------------------------------------
# Main ETL
# ---------------------------------------------------------------------------
def run_etl(xml_path: str, batch_size: int = 1000):

    # ── Schema setup ─────────────────────────────────────────────────────────
    print("▶  Creating schema / tables if not present …")
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(DDL)
        conn.commit()
        cur.close()
    finally:
        return_connection(conn)

    # ── Stream XML ───────────────────────────────────────────────────────────
    print(f"▶  Streaming {xml_path} …")
    vocab_buf, ddi_buf, disease_buf = [], [], []
    drug_count  = 0
    flush_count = 0

    context = etree.iterparse(
        xml_path,
        events=("end",),
        tag=T("drug"),
        recover=True,       # tolerates minor XML malformation
    )

    with tqdm(unit=" drugs", desc="Parsing DrugBank") as pbar:
        for _event, drug_el in context:

            # Skip nested <drug> tags that live inside <drug-interactions>
            parent = drug_el.getparent()
            if parent is not None and parent.tag == T("drug-interactions"):
                drug_el.clear()
                continue

            vocab_row, ddi_rows, disease_row = parse_drug(drug_el)

            if vocab_row:
                vocab_buf.append(vocab_row)
                ddi_buf.extend(ddi_rows)
                if disease_row:
                    disease_buf.append(disease_row)
                drug_count += 1
                pbar.update(1)

            # ── Critical: free element memory immediately ──────────────────
            drug_el.clear()
            while drug_el.getprevious() is not None:
                del drug_el.getparent()[0]

            # ── Flush every batch_size drugs ───────────────────────────────
            if drug_count % batch_size == 0:
                _flush(vocab_buf, ddi_buf, disease_buf)
                vocab_buf.clear()
                ddi_buf.clear()
                disease_buf.clear()
                flush_count += 1
                # Small courtesy sleep every 50 flushes — keeps RDS happy
                if flush_count % 50 == 0:
                    time.sleep(0.2)

    # Final flush for remainder
    if vocab_buf or ddi_buf or disease_buf:
        _flush(vocab_buf, ddi_buf, disease_buf)

    print(f"✔  Loaded {drug_count:,} drugs.")

    # ── Back-fill drug2_rxcui via drug_vocabulary join ───────────────────────
    print("▶  Back-filling drug2_rxcui …")
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(BACKFILL_SQL)
        conn.commit()
        cur.close()
        print("✔  Back-fill done.")
    finally:
        return_connection(conn)

    DatabaseConnection.close_all_connections()
    print("✔  Pool closed. ETL complete.")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="DrugBank XML → RDS PostgreSQL ETL")
    parser.add_argument("--xml",   required=True,  help="Path to DrugBank full_database.xml")
    parser.add_argument("--batch", type=int, default=1000,
                        help="Drugs per DB flush (default 1000 — matches your RxNorm ETL)")
    args = parser.parse_args()

    run_etl(args.xml, args.batch)
