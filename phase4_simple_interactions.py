"""
Phase 4 — Drug Interaction + Drug-Disease Lookup
=================================================
Replaces the old phase4_simple_interactions.py.
Queries drugbank_schema (loaded by drugbank_etl.py) instead of any
hard-coded interaction list or external API call.

Input:  list of {rxcui, medicine_name} dicts from Phase 3
Output: {
    drug_interactions:    [ {drug1_name, drug2_name, severity, description, source} ],
    drug_disease_warnings:[ {drug_name, rxcui, indication_text, mesh_terms} ],
    high_severity_count:  int,
    moderate_severity_count: int
}

Usage (called from FastAPI endpoint via run_in_threadpool):
    result = run_phase4(medicines_for_interaction)
"""

import itertools
from db_connection import get_connection, return_connection


# ---------------------------------------------------------------------------
# Core lookup — drug-drug interactions
# FIX: removed di.source from SELECT — column does not exist in ETL DDL.
#      Source is hardcoded as "DRUGBANK" in the result dict below.
# ---------------------------------------------------------------------------
DDI_QUERY = """
SELECT
    di.drug1_rxcui,
    di.drug2_rxcui,
    di.drug2_name,
    di.severity,
    di.description
FROM drugbank_schema.drug_interactions di
WHERE
    (di.drug1_rxcui = %s AND di.drug2_rxcui = %s)
    OR
    (di.drug1_rxcui = %s AND di.drug2_rxcui = %s)
ORDER BY
    CASE di.severity
        WHEN 'HIGH'     THEN 1
        WHEN 'MODERATE' THEN 2
        WHEN 'LOW'      THEN 3
        ELSE 4
    END
LIMIT 5;
"""

# ---------------------------------------------------------------------------
# Core lookup — drug-disease indications
# ---------------------------------------------------------------------------
DISEASE_QUERY = """
SELECT
    dd.rxcui,
    dd.drug_name,
    dd.indication_text,
    dd.mesh_terms
FROM drugbank_schema.drug_disease dd
WHERE dd.rxcui = %s
  AND (dd.indication_text IS NOT NULL OR dd.mesh_terms IS NOT NULL)
LIMIT 1;
"""

# ---------------------------------------------------------------------------
# Reverse lookup: rxcui → canonical drug name from drug_vocabulary
# (used when drug2_name is missing in the interactions table)
# ---------------------------------------------------------------------------
NAME_QUERY = """
SELECT name FROM drugbank_schema.drug_vocabulary WHERE rxcui = %s LIMIT 1;
"""


def _get_drug_name(cur, rxcui: str, fallback: str) -> str:
    """Resolve a display name for a rxcui, falling back to provided string."""
    if not rxcui:
        return fallback
    cur.execute(NAME_QUERY, (rxcui,))
    row = cur.fetchone()
    return row[0] if row else fallback


def run_phase4(medicines: list) -> dict:
    """
    medicines: [{"rxcui": "12345", "medicine_name": "Aspirin"}, ...]
    Returns the Phase 4 result dict.
    """
    conn = get_connection()
    cur  = conn.cursor()

    drug_interactions     = []
    drug_disease_warnings = []
    seen_pairs            = set()   # deduplicate A-B / B-A

    try:
        # ── 1. Pairwise DDI lookup ────────────────────────────────────────
        valid = [m for m in medicines if m.get("rxcui")]

        for med_a, med_b in itertools.combinations(valid, 2):
            rxcui_a = med_a["rxcui"]
            rxcui_b = med_b["rxcui"]

            pair_key = tuple(sorted([rxcui_a, rxcui_b]))
            if pair_key in seen_pairs:
                continue
            seen_pairs.add(pair_key)

            cur.execute(DDI_QUERY, (rxcui_a, rxcui_b, rxcui_b, rxcui_a))
            rows = cur.fetchall()

            seen_descriptions = set()

            for row in rows:
                # FIX: unpack 5 columns (source removed from SELECT)
                db1_rxcui, db2_rxcui, db2_name, severity, description = row

                # Normalize description to catch duplicate rows
                desc_key = (tuple(sorted([db1_rxcui, db2_rxcui])), description or "")
                if desc_key in seen_descriptions:
                    continue
                seen_descriptions.add(desc_key)

                # Resolve display names — prefer pipeline names over DB names
                name_a = med_a["medicine_name"]
                name_b = med_b["medicine_name"]

                # Align drug1/drug2 display names to match DB row direction
                if db1_rxcui == rxcui_a:
                    d1_name, d1_rxcui = name_a, rxcui_a
                    d2_name, d2_rxcui = name_b, rxcui_b
                else:
                    d1_name, d1_rxcui = name_b, rxcui_b
                    d2_name, d2_rxcui = name_a, rxcui_a

                drug_interactions.append({
                    "drug1_name":   d1_name,
                    "drug1_rxcui":  d1_rxcui,
                    "drug2_name":   d2_name,
                    "drug2_rxcui":  d2_rxcui,
                    "severity":     severity    or "UNKNOWN",
                    "description":  description or "",
                    "source":       "DRUGBANK",   # FIX: hardcoded, no DB column
                })

        # ── 2. Drug-disease lookup (one per medicine) ─────────────────────
        for med in valid:
            cur.execute(DISEASE_QUERY, (med["rxcui"],))
            row = cur.fetchone()
            if row:
                rxcui, drug_name, indication_text, mesh_terms = row
                drug_disease_warnings.append({
                    "drug_name":       med["medicine_name"],
                    "rxcui":           rxcui,
                    "indication_text": indication_text or "",
                    "mesh_terms":      list(mesh_terms) if mesh_terms else [],
                })

    except Exception as e:
        print(f"[Phase 4] DB error: {e}")
        conn.rollback()
        raise

    finally:
        cur.close()
        return_connection(conn)

    # ── 3. Severity summary counts ────────────────────────────────────────
    high_count     = sum(1 for i in drug_interactions if i["severity"] == "HIGH")
    moderate_count = sum(1 for i in drug_interactions if i["severity"] == "MODERATE")

    print(f"[Phase 4] DDI pairs found: {len(drug_interactions)} "
          f"(HIGH={high_count}, MODERATE={moderate_count})")
    print(f"[Phase 4] Drug-disease records: {len(drug_disease_warnings)}")

    return {
        "drug_interactions":       drug_interactions,
        "drug_disease_warnings":   drug_disease_warnings,
        "high_severity_count":     high_count,
        "moderate_severity_count": moderate_count,
    }