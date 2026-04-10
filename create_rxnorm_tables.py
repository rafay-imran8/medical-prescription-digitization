# create_rxnorm_tables.py
from db_connection import get_connection, return_connection

def create_rxnorm_tables():
    """
    Create RxNorm tables - these won't have JPA entities
    """
    conn = get_connection()
    cur = conn.cursor()
    
    try:
        print("Creating RxNorm tables...")
        
        cur.execute("""
            CREATE TABLE IF NOT EXISTS rxnorm_schema.rxnconso (
                id SERIAL PRIMARY KEY,
                rxaui VARCHAR(20) NOT NULL,
                rxcui VARCHAR(20) NOT NULL,
                str TEXT NOT NULL,
                sab VARCHAR(20) NOT NULL,
                tty VARCHAR(20),
                CONSTRAINT unique_rxaui UNIQUE(rxaui)
            );
        """)
        print("✓ rxnconso")
        
        cur.execute("""
            CREATE TABLE IF NOT EXISTS rxnorm_schema.rxnrel (
                id SERIAL PRIMARY KEY,
                rxaui1 VARCHAR(20) NOT NULL,
                rel VARCHAR(20) NOT NULL,
                rxaui2 VARCHAR(20) NOT NULL,
                sab VARCHAR(20),
                rela VARCHAR(50)
            );
        """)
        print("✓ rxnrel")
        
        cur.execute("""
            CREATE TABLE IF NOT EXISTS rxnorm_schema.rxnsat (
                id SERIAL PRIMARY KEY,
                rxcui VARCHAR(20) NOT NULL,
                atn VARCHAR(100) NOT NULL,
                atv TEXT,
                sab VARCHAR(20)
            );
        """)
        print("✓ rxnsat")
        
        cur.execute("""
            CREATE TABLE IF NOT EXISTS rxnorm_schema.rxnsty (
                id SERIAL PRIMARY KEY,
                rxcui VARCHAR(20) NOT NULL,
                tui VARCHAR(20),
                stn VARCHAR(20),
                sty TEXT,
                atui VARCHAR(20)
            );
        """)
        print("✓ rxnsty")
        
        conn.commit()
        print("\n✓ All RxNorm tables created!")
        
    except Exception as e:
        conn.rollback()
        print(f"✗ Error: {e}")
        raise
    finally:
        cur.close()
        return_connection(conn)

if __name__ == "__main__":
    from config import DB_CONFIG
    from db_connection import DatabaseConnection
    
    DatabaseConnection.initialize_pool(**DB_CONFIG)
    create_rxnorm_tables()
    DatabaseConnection.close_all_connections()