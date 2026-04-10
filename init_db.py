from db_connection import DatabaseConnection, get_connection, return_connection
import os
from dotenv import load_dotenv

load_dotenv()

# ==========================================================
# 1️⃣ INITIALIZE POOL (NO HARDCODING)
# ==========================================================
def init_db_pool():
    DatabaseConnection.initialize_pool(
        host=os.getenv("DB_HOST"),
        database=os.getenv("DB_NAME"),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        port=int(os.getenv("DB_PORT", 5432))
    )


# ==========================================================
# 2️⃣ FULL SCHEMA SQL
# ==========================================================
SCHEMA_SQL = """
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;

CREATE SCHEMA IF NOT EXISTS rxnorm_schema;
CREATE SCHEMA IF NOT EXISTS app_schema;

CREATE TABLE IF NOT EXISTS app_schema.users (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN', 'ANALYST')),
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_schema.doctors (
    doctor_id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE NOT NULL REFERENCES app_schema.users(user_id) ON DELETE CASCADE,
    doctor_unique_id VARCHAR(20) UNIQUE NOT NULL,
    specialization VARCHAR(255),
    license_number VARCHAR(100),
    phone VARCHAR(20),
    clinic_address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_schema.patients (
    patient_id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE REFERENCES app_schema.users(user_id) ON DELETE CASCADE,
    patient_unique_id VARCHAR(20) UNIQUE NOT NULL,
    patient_name VARCHAR(255) NOT NULL,
    age INTEGER,
    gender VARCHAR(20),
    contact_number VARCHAR(20),
    email VARCHAR(255),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_schema.patient_doctor_access (
    access_id SERIAL PRIMARY KEY,
    patient_id INTEGER NOT NULL REFERENCES app_schema.patients(patient_id) ON DELETE CASCADE,
    doctor_id INTEGER NOT NULL REFERENCES app_schema.doctors(doctor_id) ON DELETE CASCADE,
    access_granted BOOLEAN DEFAULT FALSE,
    access_requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    access_granted_at TIMESTAMP,
    access_expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE(patient_id, doctor_id)
);

CREATE TABLE IF NOT EXISTS app_schema.access_tokens (
    token_id SERIAL PRIMARY KEY,
    access_id INTEGER NOT NULL REFERENCES app_schema.patient_doctor_access(access_id) ON DELETE CASCADE,
    token VARCHAR(64) UNIQUE NOT NULL,
    email_sent_to VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    is_valid BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_schema.prescriptions (
    prescription_id SERIAL PRIMARY KEY,
    patient_id INTEGER NOT NULL REFERENCES app_schema.patients(patient_id) ON DELETE CASCADE,
    doctor_id INTEGER REFERENCES app_schema.doctors(doctor_id) ON DELETE SET NULL,
    prescription_type VARCHAR(20) DEFAULT 'SCANNED' CHECK (prescription_type IN ('SCANNED', 'DIGITAL')),
    prescription_date DATE,
    diagnosis TEXT,
    patient_history TEXT,
    weight VARCHAR(20),
    temperature VARCHAR(20),
    blood_pressure VARCHAR(20),
    raw_ocr_json JSONB,
    llm_corrected_json JSONB,
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    image_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_schema.prescription_images (
    image_id SERIAL PRIMARY KEY,
    prescription_id INTEGER NOT NULL REFERENCES app_schema.prescriptions(prescription_id) ON DELETE CASCADE,
    original_filename VARCHAR(255),
    stored_filename VARCHAR(255) UNIQUE NOT NULL,
    file_path TEXT NOT NULL,
    file_size_bytes BIGINT,
    mime_type VARCHAR(50),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_schema.prescription_medicines (
    medicine_id SERIAL PRIMARY KEY,
    prescription_id INTEGER NOT NULL REFERENCES app_schema.prescriptions(prescription_id) ON DELETE CASCADE,
    medicine_name VARCHAR(255) NOT NULL,
    medicine_type VARCHAR(100),
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    duration VARCHAR(100),
    quantity VARCHAR(100),
    rxcui VARCHAR(20),
    normalization_status VARCHAR(50) DEFAULT 'PENDING',
    normalization_confidence DECIMAL(5,2),
    validation_status VARCHAR(50) DEFAULT 'PENDING',
    validation_errors TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_schema.processing_logs (
    log_id SERIAL PRIMARY KEY,
    prescription_id INTEGER REFERENCES app_schema.prescriptions(prescription_id) ON DELETE CASCADE,
    phase VARCHAR(50),
    status VARCHAR(50),
    message TEXT,
    error_details TEXT,
    processing_time_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_schema.llm_corrections (
    correction_id SERIAL PRIMARY KEY,
    prescription_id INTEGER REFERENCES app_schema.prescriptions(prescription_id) ON DELETE CASCADE,
    field_name VARCHAR(100),
    original_value TEXT,
    corrected_value TEXT,
    correction_type VARCHAR(50),
    confidence_score DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_schema.drug_interactions (
    interaction_id SERIAL PRIMARY KEY,
    prescription_id INTEGER REFERENCES app_schema.prescriptions(prescription_id) ON DELETE CASCADE,
    drug1_rxcui VARCHAR(20),
    drug1_name VARCHAR(255),
    drug2_rxcui VARCHAR(20),
    drug2_name VARCHAR(255),
    severity VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_schema.users (email, password_hash, full_name, role, is_active, is_verified)
VALUES (
    'admin@prescriptionsystem.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzpLRw.1Wu',
    'System Administrator',
    'ADMIN',
    TRUE,
    TRUE
)
ON CONFLICT (email) DO NOTHING;
"""


# ==========================================================
# 3️⃣ RUN SETUP USING POOL
# ==========================================================
def run_setup():
    init_db_pool()

    conn = None
    try:
        conn = get_connection()
        cur = conn.cursor()

        print("🚀 Running database setup...")
        cur.execute(SCHEMA_SQL)
        conn.commit()

        print("✅ Schema created successfully")

        cur.execute("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'app_schema'
            ORDER BY table_name
        """)
        tables = cur.fetchall()

        print("📦 Tables:")
        for t in tables:
            print(" -", t[0])

    except Exception as e:
        if conn:
            conn.rollback()
        print("❌ Setup failed:", e)
        raise
    finally:
        if conn:
            return_connection(conn)
        DatabaseConnection.close_all_connections()
        print("🔌 Pool closed")


if __name__ == "__main__":
    run_setup()
