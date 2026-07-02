"""
FastAPI AI Microservice
----------------------
Single prescription image processing pipeline:
Phase 1: OCR (YOLO + TrOCR)
Phase 2: LLM Cleaning
Phase 3: Drug Normalization (RxNorm)
Phase 4: Drug Interactions (DrugBank)

Called by Spring Boot backend.
"""

# =========================================================
# Imports
# =========================================================
import uuid
import shutil
import time
from pathlib import Path
from typing import List
from collections import defaultdict
import torch
from contextlib import asynccontextmanager

from fastapi import FastAPI, UploadFile, File, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.concurrency import run_in_threadpool
from pydantic import BaseModel


# =========================================================
# Your Internal Imports
# =========================================================
from OCR_System import YOLOEngine, TrOCREngine, extract_fields_raw
from LLM_After_OCR import EnhancedMedicalLLMProcessor
from phase3_drug_normalization import DrugNormalizer
# FIX: import run_phase4, not check_interactions_with_db (old function is gone)
from phase4_simple_interactions import run_phase4

from db_connection import DatabaseConnection, get_connection, return_connection
from config import DB_CONFIG


# =========================================================
# Paths / Storage
# =========================================================
UPLOAD_DIR = Path("./uploads")
UPLOAD_DIR.mkdir(exist_ok=True, parents=True)

VIS_DIR = UPLOAD_DIR / "visualizations"
VIS_DIR.mkdir(exist_ok=True, parents=True)

YOLO_MODEL_PATH = "./best.pt"


# =========================================================
# Lifespan (Startup / Shutdown)
# =========================================================
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Load DB pool + GPU models ONCE at startup.
    Cleanup on shutdown.
    """

    # ---------------- STARTUP ----------------
    DatabaseConnection.initialize_pool(**DB_CONFIG)
    print("✅ Database connection pool initialized")

    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"🔧 Loading models on {device}...")

    app.state.yolo_engine = YOLOEngine(
        model_path=str(YOLO_MODEL_PATH),
        device=device
    )

    app.state.ocr_engine = TrOCREngine(device=device)

    app.state.llm_processor = EnhancedMedicalLLMProcessor(
        model_choice="flan_t5_xl",
        device=device
    )

    print("✅ All AI models loaded successfully")

    yield

    # ---------------- SHUTDOWN ----------------
    DatabaseConnection.close_all_connections()
    print("🛑 Database connections closed")


# =========================================================
# FastAPI App
# =========================================================
app = FastAPI(
    title="Prescription AI Microservice",
    description="GPU-backed AI service for prescription understanding",
    version="1.0.0",
    lifespan=lifespan
)


# =========================================================
# CORS (Spring Boot on 8080)
# =========================================================
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # restrict in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# =========================================================
# Pydantic Models
# =========================================================
class MedicineInput(BaseModel):
    rxcui: str
    medicine_name: str


class InteractionCheckRequest(BaseModel):
    medicines: List[MedicineInput]


# =========================================================
# Utilities
# =========================================================
def save_upload_file(upload_file: UploadFile, destination: Path) -> Path:
    with destination.open("wb") as buffer:
        shutil.copyfileobj(upload_file.file, buffer)
    upload_file.file.close()
    return destination


# =========================================================
# Health Endpoints
# =========================================================
@app.get("/")
async def root(request: Request):
    return {
        "service": "Prescription AI Microservice",
        "status": "running",
        "models_loaded": {
            "yolo": hasattr(request.app.state, "yolo_engine"),
            "ocr": hasattr(request.app.state, "ocr_engine"),
            "llm": hasattr(request.app.state, "llm_processor"),
        },
    }


@app.get("/health")
async def health():
    conn = None
    try:
        conn = get_connection()
        db_status = "connected"
    except Exception:
        db_status = "disconnected"
    finally:
        if conn:
            return_connection(conn)

    return {"status": "healthy", "database": db_status}


# =========================================================
# MAIN PIPELINE ENDPOINT
# =========================================================
@app.post("/analyze-prescription")
async def analyze_prescription(
    request: Request,
    image: UploadFile = File(...)
):
    """
    Complete pipeline for SINGLE prescription image:
    1. OCR (YOLO + TrOCR)
    2. LLM Cleaning
    3. RxNorm Normalization
    4. Drug Interaction + Disease Check (DrugBank)

    Returns structured JSON - NO DATABASE INSERTION
    Spring Boot handles all database operations.
    """

    start_time = time.time()
    request_id = uuid.uuid4().hex

    # get models from app.state (no globals)
    yolo_engine = request.app.state.yolo_engine
    ocr_engine = request.app.state.ocr_engine
    llm_processor = request.app.state.llm_processor

    # Validate image
    if not image.filename.lower().endswith((".png", ".jpg", ".jpeg")):
        raise HTTPException(
            status_code=400,
            detail="Only PNG/JPG images are supported"
        )

    # Save image temporarily
    image_id = uuid.uuid4().hex
    saved_path = save_upload_file(
        image,
        UPLOAD_DIR / f"{image_id}_{image.filename}"
    )

    try:
        # ========================================
        # PHASE 1: OCR (YOLO + TrOCR)
        # ========================================
        print(f"\n[PHASE 1] Running OCR on {image.filename}...")

        phase1_data = await run_in_threadpool(
            extract_fields_raw,
            image_path=str(saved_path),
            yolo_engine=yolo_engine,
            ocr_engine=ocr_engine,
            conf_threshold=0.10,
            save_visualization=True,
            viz_output_dir=VIS_DIR
        )

        # ========================================
        # PHASE 2: LLM CLEANING
        # ========================================
        print(f"\n[PHASE 2] Running LLM cleaning...")

        phase2_data = await run_in_threadpool(
            llm_processor.process_prescription,
            phase1_data
        )

        # ========================================
        # PHASE 3: RXNORM NORMALIZATION
        # ========================================
        print(f"\n[PHASE 3] Running RxNorm normalization...")

        phase3_result = await run_in_threadpool(
            DrugNormalizer.process_prescription,
            phase2_data
        )

        # Build medicines list for Phase 4 — only those with a real RxCUI
        medicines_for_interaction = []
        for med in phase3_result.get("normalization_results", []):
            if med.get("rxcui") and med.get("rxcui") != "NOT_FOUND":
                medicines_for_interaction.append({
                    "rxcui":         med["rxcui"],
                    "medicine_name": med["original_name"],
                })

        rxcui_groups = defaultdict(list)
        for med in phase3_result.get("normalization_results", []):
            if med.get("rxcui"):
                rxcui_groups[med["rxcui"]].append(med["original_name"])

        duplicate_ingredient_warnings = []
        for rxcui, names in rxcui_groups.items():
            if len(names) > 1:
                duplicate_ingredient_warnings.append({
                    "rxcui": rxcui,
                    "medicines": names,
                    "message": f"{' and '.join(names)} both contain the same active ingredient. Consider taking only one or consult your doctor."
                })

        # ========================================
        # PHASE 4: DRUG INTERACTIONS + DISEASE
        # ========================================
        print(f"\n[PHASE 4] Checking drug interactions & disease warnings...")

        if len(medicines_for_interaction) >= 2:
            # FIX: call run_phase4, not check_interactions_with_db
            phase4_result = await run_in_threadpool(
                run_phase4,
                medicines_for_interaction
            )
        else:
            # FIX: zero-value structure matching run_phase4 output keys
            phase4_result = {
                "drug_interactions":       [],
                "drug_disease_warnings":   [],
                "high_severity_count":     0,
                "moderate_severity_count": 0,
            }

        # ========================================
        # BUILD FINAL RESPONSE
        # ========================================
        processing_time = time.time() - start_time

        final_prescription = []
        for idx, med in enumerate(phase2_data.get("prescription", [])):
            medicine_entry = {
                "Medicine_Name":         med.get("Medicine_Name"),
                "Medicine_Type":         med.get("Medicine_Type"),
                "Dosage":                med.get("Dosage"),
                "Frequency":             med.get("Frequency"),
                "Duration_to_take_med":  med.get("Duration_to_take_med"),
                "Quantity":              med.get("Quantity"),
            }

            if idx < len(phase3_result.get("normalization_results", [])):
                norm = phase3_result["normalization_results"][idx]
                medicine_entry["rxcui"]                    = norm.get("rxcui")
                medicine_entry["normalized_name"]          = norm.get("normalized_name")
                medicine_entry["normalization_confidence"] = norm.get("confidence")
                medicine_entry["normalization_status"]     = norm.get("status")
                medicine_entry["normalization_method"]     = norm.get("method")

            final_prescription.append(medicine_entry)

        # FIX: map phase4 keys correctly — drug_interactions / drug_disease_warnings
        result = {
            "patient_info":   phase2_data.get("patient_info", {}),
            "vitals":         phase2_data.get("vitals", {}),
            "clinical_info":  phase2_data.get("clinical_info", {}),
            "prescription":   final_prescription,
            "audit":          phase2_data.get("audit", {}),

            # Phase 4 outputs — Spring Boot reads these and maps to PrescriptionResponse
            "drug_interactions":       phase4_result["drug_interactions"],
            "drug_disease_warnings":   phase4_result["drug_disease_warnings"],
            "high_severity_count":     phase4_result["high_severity_count"],
            "moderate_severity_count": phase4_result["moderate_severity_count"],
            "duplicate_ingredient_warnings": duplicate_ingredient_warnings,
            
        }

        print(f"\n✅ Processing complete in {processing_time:.2f}s")

        return {
            "request_id":          request_id,
            "processing_time_sec": round(processing_time, 2),
            "result":              result,
        }

    except Exception as e:
        print(f"\n❌ Error processing prescription: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Processing failed: {str(e)}"
        )

    finally:
        try:
            if saved_path.exists():
                saved_path.unlink()
        except Exception:
            pass


# =========================================================
# Standalone Interaction Endpoint
# =========================================================
@app.post("/check-interactions")
async def check_interactions(request: InteractionCheckRequest):
    """
    Standalone endpoint for interaction check without full pipeline.
    FIX: now calls run_phase4 instead of deleted check_interactions_with_db.
    """
    if len(request.medicines) < 2:
        return {
            "drug_interactions":       [],
            "drug_disease_warnings":   [],
            "high_severity_count":     0,
            "moderate_severity_count": 0,
        }

    # FIX: convert Pydantic models to plain dicts with key run_phase4 expects
    medicines = [
        {"rxcui": m.rxcui, "medicine_name": m.medicine_name}
        for m in request.medicines
    ]

    return await run_in_threadpool(run_phase4, medicines)


# =========================================================
# Entry Point
# =========================================================
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=False  # IMPORTANT: must be False when using CUDA
    )