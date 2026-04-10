# 04_create_indexes.py
from db_connection import get_connection, return_connection

def create_indexes():
    """
    Create optimized indexes for all tables
    Run this AFTER loading RxNorm data for better performance
    """
    conn = get_connection()
    cur = conn.cursor()
    
    try:
        print("Creating indexes for RxNorm tables...")
        
        # RXNCONSO indexes - Critical for Phase 3 drug name matching
        indexes = [
            # Primary lookup indexes
            ("idx_rxnconso_rxcui", "rxnorm_schema.rxnconso", "(rxcui)"),
            ("idx_rxnconso_rxaui", "rxnorm_schema.rxnconso", "(rxaui)"),
            ("idx_rxnconso_sab", "rxnorm_schema.rxnconso", "(sab)"),
            ("idx_rxnconso_tty", "rxnorm_schema.rxnconso", "(tty)"),
            
            # Text search indexes for fuzzy matching
            ("idx_rxnconso_str_lower", "rxnorm_schema.rxnconso", "(LOWER(str))"),
            ("idx_rxnconso_str_trgm", "rxnorm_schema.rxnconso", "USING gin(str gin_trgm_ops)"),
            
            # Composite indexes for common queries
            ("idx_rxnconso_rxcui_sab", "rxnorm_schema.rxnconso", "(rxcui, sab)"),
            ("idx_rxnconso_sab_tty", "rxnorm_schema.rxnconso", "(sab, tty)"),
            
            # RXNREL indexes
            ("idx_rxnrel_rxaui1", "rxnorm_schema.rxnrel", "(rxaui1)"),
            ("idx_rxnrel_rxaui2", "rxnorm_schema.rxnrel", "(rxaui2)"),
            ("idx_rxnrel_rel", "rxnorm_schema.rxnrel", "(rel)"),
            
            # RXNSAT indexes
            ("idx_rxnsat_rxcui", "rxnorm_schema.rxnsat", "(rxcui)"),
            ("idx_rxnsat_atn", "rxnorm_schema.rxnsat", "(atn)"),
            ("idx_rxnsat_rxcui_atn", "rxnorm_schema.rxnsat", "(rxcui, atn)"),
            
            # RXNSTY indexes
            ("idx_rxnsty_rxcui", "rxnorm_schema.rxnsty", "(rxcui)"),
        ]
        
        for idx_name, table, columns in indexes:
            cur.execute(f"CREATE INDEX IF NOT EXISTS {idx_name} ON {table} {columns};")
            print(f"✓ {idx_name}")
        
        print("\nCreating indexes for application tables...")
        
        # Application table indexes
        app_indexes = [
            ("idx_prescriptions_date", "app_schema.prescriptions", "(prescription_date)"),
            ("idx_prescriptions_status", "app_schema.prescriptions", "(processing_status)"),
            ("idx_prescriptions_created", "app_schema.prescriptions", "(created_at DESC)"),
            
            ("idx_medicines_prescription_id", "app_schema.prescription_medicines", "(prescription_id)"),
            ("idx_medicines_rxcui", "app_schema.prescription_medicines", "(rxcui)"),
            ("idx_medicines_name_trgm", "app_schema.prescription_medicines", "USING gin(medicine_name gin_trgm_ops)"),
            ("idx_medicines_norm_status", "app_schema.prescription_medicines", "(normalization_status)"),
            ("idx_medicines_val_status", "app_schema.prescription_medicines", "(validation_status)"),
            
            ("idx_logs_prescription_id", "app_schema.processing_logs", "(prescription_id)"),
            ("idx_logs_phase", "app_schema.processing_logs", "(phase)"),
            ("idx_logs_created", "app_schema.processing_logs", "(created_at DESC)"),
            
            ("idx_corrections_prescription_id", "app_schema.llm_corrections", "(prescription_id)"),
        ]
        
        for idx_name, table, columns in app_indexes:
            cur.execute(f"CREATE INDEX IF NOT EXISTS {idx_name} ON {table} {columns};")
            print(f"✓ {idx_name}")
        
        conn.commit()
        print("\n✓ All indexes created successfully!")
        
    except Exception as e:
        conn.rollback()
        print(f"✗ Error creating indexes: {e}")
        raise
    finally:
        cur.close()
        return_connection(conn)

if __name__ == "__main__":
    from config import DB_CONFIG
    from db_connection import DatabaseConnection
    
    DatabaseConnection.initialize_pool(**DB_CONFIG)
    create_indexes()
    DatabaseConnection.close_all_connections()