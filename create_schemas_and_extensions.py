# create_schemas_and_extensions.py
from db_connection import get_connection, return_connection

def setup_database():

    conn = get_connection()
  
    cur = conn.cursor()
    
    try:
        print("Creating PostgreSQL extensions...")
        cur.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm;")
        cur.execute("CREATE EXTENSION IF NOT EXISTS unaccent;")
        cur.execute("CREATE EXTENSION IF NOT EXISTS btree_gin;")
        print("✓ Extensions created")
        
        print("\nCreating schemas...")
        schemas = ["app_schema", "rxnorm_schema", "umls_schema"]
        for schema in schemas:
            cur.execute(f"CREATE SCHEMA IF NOT EXISTS {schema};")
            print(f"✓ Schema '{schema}' created")
        
        conn.commit()
        print("\n✓ Database setup completed!")
        
    except Exception as e:
        conn.rollback()
        print(f"✗ Error: {e}")
        raise
    finally:
        cur.close()
        return_connection(conn)

print("Script started!")
if __name__ == "__main__":
    print("Main block executing...")
    from config import DB_CONFIG
    from db_connection import DatabaseConnection
    
    print("Initializing connection pool...")
    DatabaseConnection.initialize_pool(**DB_CONFIG)
    setup_database()
    DatabaseConnection.close_all_connections()
    print("Done!")