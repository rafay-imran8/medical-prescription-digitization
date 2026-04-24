# 🏗️ System Architecture & Data Flow

## 📐 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUTTER MOBILE APP                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ Home Screen  │→→│ Image Upload │→→│Results Screen│           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
│         ↓                  ↓                  ↑                  │
│    [Riverpod State Management]                                  │
│    [Image Picker Service]  [API Service]                       │
└────────────┬─────────────────────────────────┬──────────────────┘
             │                                 │
             ↓ HTTP POST (multipart/form-data) ↓
┌─────────────────────────────────────────────────────────────────┐
│              FASTAPI BACKEND (Port 8000)                        │
│  [4-Phase Analysis Pipeline]                                    │
│  ├─ Phase 1: OCR (YOLO + TrOCR)                                │
│  ├─ Phase 2: LLM Cleaning                                      │
│  ├─ Phase 3: RxNorm Normalization                              │
│  └─ Phase 4: Drug Interactions (DrugBank)                      │
│                                                                 │
│  Returns: PrescriptionApiResponse (JSON)                       │
└────────────┬─────────────────────────┬──────────────────────────┘
             │                         │
             ↓ DB Query               ↓ Optional: HTTP POST
┌─────────────────────────────────────────────────────────────────┐
│           SPRING BOOT API (Port 8080)                           │
│  ┌─────────────────────────────────────────┐                   │
│  │ /api/patient/prescriptions/upload       │                   │
│  │ - Authenticate user (JWT)               │                   │
│  │ - Call FastAPI                          │                   │
│  │ - Save to database                      │                   │
│  │ - Return PrescriptionResponse           │                   │
│  └─────────────────────────────────────────┘                   │
└────────────┬──────────────────────────────────────────────────┬─┘
             │                                                   │
             ↓ JDBC                                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              PostgreSQL DATABASE                                │
│  ├─ users (authentication)                                      │
│  ├─ patients (patient records)                                 │
│  ├─ doctors (doctor records)                                   │
│  ├─ prescriptions (prescription metadata)                      │
│  ├─ prescription_medicines (extracted medicines)              │
│  ├─ drug_interactions (detected interactions)                 │
│  ├─ processing_logs (audit trail)                            │
│  └─ llm_corrections (LLM modifications)                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Data Flow - User Uploads Prescription

### Step 1: Image Selection
```
User selects image via Camera/Gallery
         ↓
ImagePickerService.pickImage()
         ↓
Returns XFile (with file path)
         ↓
Update selectedImageProvider state
         ↓
Show loading dialog
```

### Step 2: Upload to FastAPI
```
prescriptionProvider.analyzePrescription(imagePath)
         ↓
Create MultipartFile from image
         ↓
POST /analyze-prescription
Host: 10.0.2.2:8000 (Android emulator)
Content-Type: multipart/form-data
         ↓
FastAPI receives image file
         ↓
Save to /uploads directory temporarily
```

### Step 3: FastAPI Processing (4 Phases)

#### Phase 1: OCR Extraction
```
Image → YOLO Model (Field Detection)
  ├─ Detect: Medicine Name, Dosage, Frequency, etc.
  ├─ Extract bounding boxes
  └─ Pass to TrOCR for text recognition

TrOCR (Transformer-based OCR)
  ├─ Convert image regions to text
  ├─ Handle different handwriting styles
  └─ Apply confidence filtering (conf_threshold=0.10)

Output:
{
  "prescription": [
    {
      "Medicine_Name": "Aspirin",
      "Dosage": "500mg",
      "Frequency": "Twice daily",
      ...
    }
  ],
  "patient_info": {...},
  "vitals": {...},
  ...
}
```

#### Phase 2: LLM Cleaning
```
Raw OCR output → EnhancedMedicalLLMProcessor
  ├─ Model: FLAN-T5-XL (11B parameters)
  ├─ Corrections:
  │  ├─ Spelling corrections
  │  ├─ Standard terminology
  │  ├─ Unit normalization (mg, ml, etc.)
  │  └─ Duplicate detection
  ├─ Preserve original values
  └─ Output confidence scores

Output: Cleaned prescription dict with
  - Corrected field values
  - LLM audit trail
  - Confidence metrics
```

#### Phase 3: Drug Normalization
```
Cleaned medicines → DrugNormalizer
  ├─ Database: RxNorm (UMLS)
  ├─ Matching strategies:
  │  ├─ Exact match (highest priority)
  │  ├─ Fuzzy match (string similarity)
  │  ├─ Supplement map (OTC products)
  │  └─ Safety block (counterfeit detection)
  ├─ Map to RxCUI (RxNorm Concept Unique Identifier)
  ├─ Extract standard drug name
  └─ Assign confidence score (0-1)

Output: Per medicine:
{
  "rxcui": "207106",
  "normalized_name": "Aspirin 500 MG Oral Tablet",
  "confidence": 0.95,
  "method": "exact_match",
  "status": "completed"
}
```

#### Phase 4: Interaction & Disease Checking
```
Normalized medicines → run_phase4(medicines)
  ├─ Database: DrugBank (>14000 drugs)
  ├─ For each medicine pair:
  │  ├─ Check drug-drug interactions
  │  │  ├─ Severity: HIGH | MODERATE | LOW | UNKNOWN
  │  │  ├─ Get clinical description
  │  │  └─ Source: DRUGBANK
  │  │
  │  └─ Check disease contraindications
  │     ├─ Indications (when drug should be used)
  │     ├─ Contraindications (when NOT to use)
  │     └─ MeSH terms (medical subject headings)
  │
  └─ Generate duplicate warnings:
      ├─ Group medicines by RxCUI
      ├─ Flag if same drug given twice
      └─ Suggest consulting doctor

Output:
{
  "drug_interactions": [
    {
      "drug1_name": "Warfarin",
      "drug2_name": "Aspirin",
      "severity": "HIGH",
      "description": "Increased bleeding risk...",
      "source": "DRUGBANK"
    }
  ],
  "drug_disease_warnings": [...],
  "duplicate_ingredient_warnings": [...],
  "high_severity_count": 1,
  "moderate_severity_count": 2
}
```

### Step 4: Response to Flutter

```
FastAPI Response:
{
  "request_id": "abc123...",
  "processing_time_sec": 45.2,
  "result": {
    "patient_info": {...},
    "vitals": {...},
    "prescription": [...medicines with normalization...],
    "drug_interactions": [...],
    "drug_disease_warnings": [...],
    "duplicate_ingredient_warnings": [...],
    "high_severity_count": 1,
    "moderate_severity_count": 2
  }
}

↓ Flutter receives and deserializes
↓ PrescriptionApiResponse.fromJson()
↓ Update prescriptionProvider state
↓ Navigate to ResultsScreen
```

### Step 5: Display Results

```
Results Screen renders:
├─ Summary Card (counts, severity badges)
├─ Medicines Section
│  └─ MedicineCard × N
│     ├─ Original name
│     ├─ Normalized name
│     ├─ RxCUI badge
│     ├─ Confidence score (color-coded)
│     └─ Details (dosage, frequency, duration, quantity)
│
├─ Warnings Section (if any)
│  ├─ Drug Interactions
│  │  └─ DrugInteractionCard × M
│  │     ├─ Severity level (color-coded)
│  │     ├─ Drug pair
│  │     └─ Description
│  │
│  ├─ Disease Warnings
│  │  └─ Cards with indication/contraindication text
│  │
│  └─ Duplicate Ingredient Warnings
│     └─ DuplicateWarningCard with suggested action
│
└─ Info Card (disclaimer)
```

---

## 🗄️ Database Schema

### Core User Management
```sql
users
├─ user_id (PK)
├─ email (UNIQUE)
├─ password_hash
├─ full_name
├─ role (PATIENT | DOCTOR | ADMIN | ANALYST)
├─ is_verified (BOOLEAN)
└─ timestamps

patients
├─ patient_id (PK)
├─ user_id (FK → users)
├─ patient_unique_id (UNIQUE)
├─ patient_name
├─ age
├─ gender
└─ contact info

doctors
├─ doctor_id (PK)
├─ user_id (FK → users)
├─ doctor_unique_id (UNIQUE)
├─ specialization
├─ license_number
└─ contact info
```

### Prescription Data
```sql
prescriptions
├─ prescription_id (PK)
├─ patient_id (FK)
├─ doctor_id (FK, optional)
├─ prescription_date
├─ diagnosis (TEXT)
├─ patient_history (TEXT)
├─ vitals (weight, temp, BP)
├─ raw_ocr_json (JSONB) ← Phase 1 output
├─ llm_corrected_json (JSONB) ← Phase 2 output
├─ processing_status (PENDING | COMPLETED | FAILED)
└─ timestamps

prescription_medicines
├─ medicine_id (PK)
├─ prescription_id (FK)
├─ medicine_name
├─ medicine_type
├─ dosage
├─ frequency
├─ duration
├─ quantity
├─ rxcui (RxNorm ID) ← Phase 3
├─ normalization_status
└─ normalization_confidence

prescription_images
├─ image_id (PK)
├─ prescription_id (FK)
├─ original_filename
├─ stored_filename
├─ file_path
└─ file_size
```

### Analysis Data
```sql
drug_interactions
├─ interaction_id (PK)
├─ prescription_id (FK)
├─ drug1_rxcui
├─ drug1_name
├─ drug2_rxcui
├─ drug2_name
├─ severity (HIGH | MODERATE | LOW | UNKNOWN)
├─ description (TEXT)
└─ created_at

processing_logs
├─ log_id (PK)
├─ prescription_id (FK)
├─ phase (Phase 1 | 2 | 3 | 4)
├─ status (SUCCESS | FAILED)
├─ message
├─ error_details
└─ processing_time_ms

llm_corrections
├─ correction_id (PK)
├─ prescription_id (FK)
├─ field_name
├─ original_value
├─ corrected_value
├─ confidence_score
└─ created_at
```

---

## 🔐 Security Architecture

### Authentication Flow (Spring Boot)
```
User Login
    ↓
POST /api/auth/login (email, password)
    ↓
Verify credentials against users table
    ↓
Generate JWT token (valid 24 hours)
    ↓
Return token + user info
    ↓
Flutter stores in SharedPreferences
    ↓
Subsequent requests include: Authorization: Bearer <JWT>
    ↓
Spring Security validates token
```

### Direct FastAPI (For Flutter)
```
No authentication required for FastAPI
├─ Image upload to /analyze-prescription
├─ Service runs locally or on internal network
└─ Assumes trusted network (development/internal)

Production: Add API key or JWT to FastAPI
```

---

## 📊 Data Model Relationships

```
User (1) ─────→ (1) Patient
  │
  └─────────────→ (M) Prescription ←────── (M) Doctor
                  │
                  ├─→ (M) PrescriptionMedicine
                  │     │
                  │     └─→ RxCUI (External: RxNorm)
                  │
                  ├─→ (M) PrescriptionImage
                  │
                  ├─→ (M) DrugInteraction
                  │
                  ├─→ (M) ProcessingLog
                  │
                  └─→ (M) LLMCorrection
```

---

## ⚡ Performance Considerations

### FastAPI Optimization
- **YOLO Model Inference**: ~2-5 seconds (GPU), ~10-15 seconds (CPU)
- **TrOCR Extraction**: ~5-10 seconds per medicine
- **LLM Processing**: ~10-20 seconds for 5-10 medicines
- **RxNorm Lookup**: ~1-2 seconds per medicine
- **DrugBank Interaction Check**: ~5-10 seconds for 10+ medicines

**Total Time**: 30-60 seconds typical, up to 120 seconds worst case

### Database Optimization
- Indexes on frequently queried fields
- JSONB compression for raw_ocr_json
- Partitioning prescriptions by date
- Caching RxNorm data in memory

### Flutter Optimization
- Image compression before upload
- Lazy loading of interaction details
- Pagination for large medicine lists
- Local caching of results

---

## 🔄 State Management Flow (Riverpod)

```
prescriptionProvider (StateNotifier)
    ├─ state: PrescriptionState
    │  ├─ isLoading: bool
    │  ├─ result: PrescriptionApiResponse?
    │  ├─ error: String?
    │  └─ uploadProgress: double
    │
    └─ methods:
       ├─ analyzePrescription(path) → calls API service
       ├─ reset() → clear state
       └─ clearError() → remove error message

selectedImageProvider (StateProvider)
    └─ state: XFile? (currently selected image)

prescriptionSummaryProvider (Provider)
    └─ derives from prescriptionProvider
       ├─ totalMedicines
       ├─ totalInteractions
       ├─ highSeverity
       └─ hasDuplicateWarnings

hasWarningsProvider (Provider)
    └─ true if any warnings exist
```

---

## 🚀 Request/Response Examples

### FastAPI Upload Request
```http
POST /analyze-prescription HTTP/1.1
Host: 10.0.2.2:8000
Content-Type: multipart/form-data; boundary=----FormBoundary

------FormBoundary
Content-Disposition: form-data; name="image"; filename="prescription.jpg"
Content-Type: image/jpeg

[binary image data]
------FormBoundary--
```

### FastAPI Response
```json
{
  "request_id": "a1b2c3d4e5f6...",
  "processing_time_sec": 47.3,
  "result": {
    "patient_info": {
      "age": 45,
      "gender": "Male"
    },
    "vitals": {
      "blood_pressure": "120/80",
      "weight": "75kg",
      "temperature": "37°C"
    },
    "prescription": [
      {
        "Medicine_Name": "Aspirin",
        "Dosage": "500mg",
        "Frequency": "Twice daily",
        "Duration_to_take_med": "10 days",
        "Quantity": "20 tablets",
        "rxcui": "207106",
        "normalized_name": "Aspirin 500 MG Oral Tablet",
        "normalization_confidence": 0.95,
        "normalization_status": "completed",
        "normalization_method": "exact_match"
      }
    ],
    "drug_interactions": [
      {
        "drug1_name": "Aspirin",
        "drug1_rxcui": "207106",
        "drug2_name": "Warfarin",
        "drug2_rxcui": "11124",
        "severity": "HIGH",
        "description": "Aspirin may increase the anticoagulant...",
        "source": "DRUGBANK"
      }
    ],
    "drug_disease_warnings": [],
    "duplicate_ingredient_warnings": [],
    "high_severity_count": 1,
    "moderate_severity_count": 0
  }
}
```

---

## 🔍 Debugging & Monitoring

### Flutter Logs
```bash
flutter logs

# Expected logs:
# 🚀 [REQUEST] POST http://10.0.2.2:8000/analyze-prescription
# 📊 Upload Progress: 45.2%
# ✅ [RESPONSE] 200 - http://10.0.2.2:8000/analyze-prescription
# ✅ Upload successful
```

### FastAPI Logs
```
INFO:     127.0.0.1:12345 - "POST /analyze-prescription HTTP/1.1" 200 OK
[PHASE 1] Running OCR on prescription.jpg...
[PHASE 2] Running LLM cleaning...
[PHASE 3] Running RxNorm normalization...
[PHASE 4] Checking drug interactions...
✅ Processing complete in 47.30s
```

### Spring Boot Logs
```
2024-04-24 10:30:00 INFO  Upload request received from user: 42
2024-04-24 10:30:00 INFO  Patient found: 42
2024-04-24 10:30:45 INFO  Calling FastAPI service...
2024-04-24 10:30:50 INFO  Saving prescription to database...
2024-04-24 10:30:51 INFO  Prescription processed successfully: 123
```

---

## 📈 Scaling Considerations

### Horizontal Scaling
- FastAPI: Can run multiple instances with load balancer
- Spring Boot: Can run multiple instances
- Database: Master-replica setup for read scaling

### Vertical Scaling
- Add GPU for faster AI processing
- Increase RAM for model caching
- SSD storage for prescription images

### Optimization Points
- Cache RxNorm and DrugBank data in memory
- Batch process similar images
- Asynchronous task queue for non-critical operations
- CDN for prescription images

---

This architecture ensures:
✅ Clean separation of concerns
✅ Scalable design
✅ Comprehensive error handling
✅ Audit trail for medical compliance
✅ Secure data handling
✅ Responsive user experience
