# config.py
import os
from dotenv import load_dotenv

load_dotenv()

DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'your-rds-endpoint.rds.amazonaws.com'),
    'database': os.getenv('DB_NAME', 'prescription_db'),
    'user': os.getenv('DB_USER', 'admin'),
    'password': os.getenv('DB_PASSWORD', 'your-password'),
    'port': int(os.getenv('DB_PORT', 5432))
}

FIELD_GROUPS = {
    "patient_info": ["Patient_Name", "Age", "Gender", "Date"],
    "vitals": ["Weight", "Temperature", "Blood_Pressure"],
    "clinical_info": ["Diagnosis", "Patient_History"],
    "prescription": ["Medicine_Name", "Medicine_Type", "Dosage", 
                     "Frequency", "Duration_to_take_med", "Quantity"]
}