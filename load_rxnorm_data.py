import time
from pathlib import Path
from dotenv import load_dotenv
import os
from psycopg2.extras import execute_batch

from db_connection import (
    DatabaseConnection,
    get_connection,
    return_connection
)

# --------------------------------------------------
# ENV + POOL INIT
# --------------------------------------------------
load_dotenv()

DatabaseConnection.initialize_pool(
    host=os.getenv("DB_HOST"),
    database=os.getenv("DB_NAME"),
    user=os.getenv("DB_USER"),
    password=os.getenv("DB_PASSWORD"),
    port=int(os.getenv("DB_PORT", 5432))
)

RXNORM_DATA_DIR = Path("/home/ocrdetection/Downloads/fyp/DB/data")
BATCH_SIZE = 1000  # REDUCED from 5000 - prevents timeout
COMMIT_EVERY = 50000  # Commit every 50k rows to keep connection alive

# --------------------------------------------------
# RXNCONSO
# --------------------------------------------------
def load_rxnconso():
    conn = get_connection()
    cur = conn.cursor()
    filepath = RXNORM_DATA_DIR / "RXNCONSO.RRF"

    print("\n" + "=" * 60)
    print("Loading RXNCONSO")
    print("=" * 60)

    batch, total = [], 0
    start = time.time()

    try:
        with open(filepath, encoding="utf-8", errors="replace") as f:
            for line in f:
                fields = line.rstrip().split("|")
                if len(fields) >= 15:
                    batch.append((
                        fields[7],   # rxaui
                        fields[0],   # rxcui
                        fields[14],  # str
                        fields[11],  # sab
                        fields[12],  # tty
                    ))
                    total += 1

                    if len(batch) >= BATCH_SIZE:
                        execute_batch(cur, """
                            INSERT INTO rxnorm_schema.rxnconso
                            (rxaui, rxcui, str, sab, tty)
                            VALUES (%s, %s, %s, %s, %s)
                            ON CONFLICT (rxaui) DO NOTHING
                        """, batch, page_size=100)
                        batch.clear()
                        
                        # Commit every COMMIT_EVERY rows to keep connection alive
                        if total % COMMIT_EVERY == 0:
                            conn.commit()
                            print(f"  {total:,} rows processed (committed)")

        if batch:
            execute_batch(cur, """
                INSERT INTO rxnorm_schema.rxnconso
                (rxaui, rxcui, str, sab, tty)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (rxaui) DO NOTHING
            """, batch, page_size=100)
        
        conn.commit()  # Final commit
        print(f"✓ RXNCONSO done: {total:,} rows in {time.time() - start:.2f}s")

    except Exception as e:
        print(f"✗ Error: {e}")
        conn.rollback()
        raise
    finally:
        cur.close()
        return_connection(conn)


# --------------------------------------------------
# RXNREL (FIXED with smaller batches)
# --------------------------------------------------
def load_rxnrel():
    conn = get_connection()
    cur = conn.cursor()
    filepath = RXNORM_DATA_DIR / "RXNREL.RRF"

    print("\n" + "=" * 60)
    print("Loading RXNREL")
    print("=" * 60)

    batch, total = [], 0
    start = time.time()

    try:
        with open(filepath, encoding="utf-8", errors="replace") as f:
            for line in f:
                fields = line.rstrip().split("|")
                if len(fields) >= 8:
                    batch.append((
                        fields[0],  # rxaui1
                        fields[3],  # rel
                        fields[4],  # rxaui2
                        fields[10] if len(fields) > 10 else None,  # sab
                        fields[7] if len(fields) > 7 else None,   # rela
                    ))
                    total += 1

                    if len(batch) >= BATCH_SIZE:
                        execute_batch(cur, """
                            INSERT INTO rxnorm_schema.rxnrel
                            (rxaui1, rel, rxaui2, sab, rela)
                            VALUES (%s, %s, %s, %s, %s)
                        """, batch, page_size=100)
                        batch.clear()
                        
                        # CRITICAL: Commit frequently for large table
                        if total % COMMIT_EVERY == 0:
                            conn.commit()
                            print(f"  {total:,} rows processed (committed)")

        if batch:
            execute_batch(cur, """
                INSERT INTO rxnorm_schema.rxnrel
                (rxaui1, rel, rxaui2, sab, rela)
                VALUES (%s, %s, %s, %s, %s)
            """, batch, page_size=100)
        
        conn.commit()  # Final commit
        print(f"✓ RXNREL done: {total:,} rows in {time.time() - start:.2f}s")

    except Exception as e:
        print(f"✗ Error: {e}")
        try:
            conn.rollback()
        except:
            pass  # Connection already closed
        raise
    finally:
        cur.close()
        return_connection(conn)


# --------------------------------------------------
# RXNSAT
# --------------------------------------------------
def load_rxnsat():
    conn = get_connection()
    cur = conn.cursor()
    filepath = RXNORM_DATA_DIR / "RXNSAT.RRF"

    if not filepath.exists():
        print("⚠ RXNSAT missing, skipping")
        return_connection(conn)
        return

    print("\n" + "=" * 60)
    print("Loading RXNSAT")
    print("=" * 60)

    batch, total = [], 0
    start = time.time()

    try:
        with open(filepath, encoding="utf-8", errors="replace") as f:
            for line in f:
                fields = line.rstrip().split("|")
                if len(fields) >= 11:
                    batch.append((
                        fields[0],   # rxcui
                        fields[8],   # atn
                        fields[10],  # atv
                        fields[9],   # sab
                    ))
                    total += 1

                    if len(batch) >= BATCH_SIZE:
                        execute_batch(cur, """
                            INSERT INTO rxnorm_schema.rxnsat
                            (rxcui, atn, atv, sab)
                            VALUES (%s, %s, %s, %s)
                        """, batch, page_size=100)
                        batch.clear()
                        
                        if total % COMMIT_EVERY == 0:
                            conn.commit()
                            print(f"  {total:,} rows processed (committed)")

        if batch:
            execute_batch(cur, """
                INSERT INTO rxnorm_schema.rxnsat
                (rxcui, atn, atv, sab)
                VALUES (%s, %s, %s, %s)
            """, batch, page_size=100)
        
        conn.commit()
        print(f"✓ RXNSAT done: {total:,} rows in {time.time() - start:.2f}s")

    except Exception as e:
        print(f"✗ Error: {e}")
        conn.rollback()
        raise
    finally:
        cur.close()
        return_connection(conn)


# --------------------------------------------------
# RXNSTY
# --------------------------------------------------
def load_rxnsty():
    conn = get_connection()
    cur = conn.cursor()
    filepath = RXNORM_DATA_DIR / "RXNSTY.RRF"

    print("\n" + "=" * 60)
    print("Loading RXNSTY")
    print("=" * 60)

    batch, total = [], 0
    start = time.time()

    try:
        with open(filepath, encoding="utf-8", errors="replace") as f:
            for line in f:
                fields = line.rstrip().split("|")
                if len(fields) >= 5:
                    batch.append((
                        fields[0], fields[1], fields[2],
                        fields[3], fields[4]
                    ))
                    total += 1

                    if len(batch) >= BATCH_SIZE:
                        execute_batch(cur, """
                            INSERT INTO rxnorm_schema.rxnsty
                            (rxcui, tui, stn, sty, atui)
                            VALUES (%s, %s, %s, %s, %s)
                        """, batch, page_size=100)
                        batch.clear()
                        
                        if total % COMMIT_EVERY == 0:
                            conn.commit()

        if batch:
            execute_batch(cur, """
                INSERT INTO rxnorm_schema.rxnsty
                (rxcui, tui, stn, sty, atui)
                VALUES (%s, %s, %s, %s, %s)
            """, batch, page_size=100)
        
        conn.commit()
        print(f"✓ RXNSTY done: {total:,} rows in {time.time() - start:.2f}s")

    except Exception as e:
        print(f"✗ Error: {e}")
        conn.rollback()
        raise
    finally:
        cur.close()
        return_connection(conn)


# --------------------------------------------------
# VERIFY
# --------------------------------------------------
def verify_all():
    conn = get_connection()
    cur = conn.cursor()

    print("\nFINAL VERIFICATION")
    print("=" * 60)

    for table in ["rxnconso", "rxnrel", "rxnsat", "rxnsty"]:
        cur.execute(f"SELECT COUNT(*) FROM rxnorm_schema.{table}")
        print(f"{table:10s}: {cur.fetchone()[0]:,}")

    cur.close()
    return_connection(conn)


# --------------------------------------------------
# MAIN
# --------------------------------------------------
def main():
    print("\n" + "="*60)
    print("RXNORM DATA LOADER - OPTIMIZED")
    print("="*60)
    print(f"Batch size: {BATCH_SIZE}")
    print(f"Commit every: {COMMIT_EVERY:,} rows")
    print("="*60)
    
    try:
        load_rxnconso()
        load_rxnrel()
        load_rxnsat()
        load_rxnsty()
        verify_all()
        print("\n✅ RXNORM LOAD COMPLETE")

    finally:
        DatabaseConnection.close_all_connections()


if __name__ == "__main__":
    main()