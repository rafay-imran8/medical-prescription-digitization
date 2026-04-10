import os
os.environ["HF_HUB_DISABLE_XET"] = "1"
os.environ['HF_HUB_ENABLE_HF_TRANSFER'] = "0"

import json
import re
import warnings
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
from datetime import datetime
import torch
from transformers import (
    AutoTokenizer,
    AutoModelForSeq2SeqLM,
    pipeline
)

warnings.filterwarnings('ignore')


# ============================================================================
# MODEL CONFIGURATION
# ============================================================================

MODELS = {
    "flan_t5_xl": {
        "name": "google/flan-t5-xl",  # 3B params - BEST balance
        "type": "seq2seq",
        "size": "3B",
        "best_for": "Medical text normalization"
    },
    "flan_t5_large": {
        "name": "google/flan-t5-large",  # 780M params
        "type": "seq2seq",
        "size": "780M",
        "best_for": "Faster processing"
    }
}


# The 16 core fields we need to extract
REQUIRED_FIELDS = {
    "patient_info": ["Patient_Name", "Age", "Gender", "Date"],
    "vitals": ["Weight", "Temperature", "Blood_Pressure"],
    "clinical_info": ["Diagnosis", "Patient_History"],
    "prescription": ["Medicine_Name", "Medicine_Type", "Dosage", 
                     "Frequency", "Duration_to_take_med", "Quantity"]
}


# ============================================================================
# FLAN-T5 OPTIMIZED PROMPTS
# ============================================================================

class PromptBuilder:
    """
    Build prompts optimized for FLAN-T5 instruction-following.
    
    Key principles:
    1. Clear task definition
    2. Explicit role and boundaries
    3. Structured input/output
    4. Examples for few-shot learning
    """
    
    @staticmethod
    def build_diagnosis_prompt(diagnosis_raw: str) -> str:
        """
        MOST IMPORTANT: Clean diagnosis field.
        
        FLAN-T5 works best with:
        - Clear instruction
        - Input/output format
        - Examples
        """
        prompt = f"""You are a medical text correction specialist. Your task is to clean OCR-extracted diagnosis text from handwritten prescriptions.

INPUT FORMAT: The diagnosis contains pipe symbols (|) which represent line breaks. It may have spelling errors and garbage symbols.

RULES:
1. Remove all pipe symbols (|)
2. Fix spelling errors in medical terms
3. Remove garbage text and symbols (#, ", etc.)
4. Output comma-separated diagnoses
5. Use proper medical terminology
6. Keep it concise
7. DO NOT add information not in the input

EXAMPLES:
Input: "Prophylanis of | chickenpon | fewter # \\" | Hann Hahn"
Output: Prophylaxis of chickenpox, Fever, Pain

Input: "bone weakness | inflammation | pain"
Output: Bone weakness, Inflammation, Pain

Input: "hypertension | diabetus type 2"
Output: Hypertension, Diabetes type 2

Now clean this diagnosis:
Input: "{diagnosis_raw}"
Output:"""
        
        return prompt
    
#     @staticmethod
#     def build_medicine_name_prompt(name_raw: str) -> str:
#         """Clean medicine name."""
#         prompt = f"""You are a pharmaceutical name correction specialist.

# TASK: Fix OCR errors in medicine names from handwritten prescriptions.

# RULES:
# 1. Fix common OCR errors (6→D, l→I, etc.)
# 2. Use standard drug name capitalization
# 3. Keep formulation suffixes (Plus, Extra, DS, etc.)
# 4. Output only the corrected name
# 5. DO NOT add dosage or other info

# EXAMPLES:
# Input: "calpal 6 Plus"
# Output: Calpol D Plus

# Input: "Varivac"
# Output: Varivax

# Input: "Panadol Extra"
# Output: Panadol Extra

# Input: "Augmentin 625"
# Output: Augmentin 625

# Now correct this medicine name:
# Input: "{name_raw}"
# Output:"""
        
#         return prompt
   

    @staticmethod
    def build_medicine_name_prompt(name_raw: str) -> str:
        """Clean medicine name safely (dataset-agnostic, no hallucination)."""
        prompt = f"""You are an OCR post-processing assistant.

    TASK:
    Clean OCR noise from a medicine name.

    IMPORTANT:
    You are NOT a pharmacist.
    You are NOT allowed to guess or replace drug names.
    You must ONLY fix character-level OCR mistakes.

    STRICT RULES:
    1. Fix small spelling/character errors only (1–2 characters max)
    2. Fix capitalization
    3. Remove stray punctuation or dots
    4. DO NOT change numbers
    5. DO NOT convert numbers ↔ letters
    6. DO NOT add or remove words
    7. DO NOT substitute with a different brand or known drug
    8. DO NOT infer or guess the correct medicine
    9. If unsure, return the original text unchanged
    10. Output ONLY the cleaned text

    GOOD BEHAVIOR EXAMPLES:
    Input: "amoxcillin"
    Output: Amoxicillin

    Input: "paracetmol 500"
    Output: Paracetamol 500

    Input: "panadol extra ."
    Output: Panadol Extra

    Input: "ceftriaxne inj"
    Output: Ceftriaxone inj

    Input: "xyzabc"
    Output: xyzabc

    BAD BEHAVIOR (NEVER DO THIS):
    Input: "abc 6"
    Output: abc D        ← illegal number change

    Input: "unknownmed"
    Output: Paracetamol  ← guessing is forbidden

    Now clean this medicine name:
    Input: "{name_raw}"
    Output:"""

        return prompt

    
    @staticmethod
    def build_frequency_prompt(freq_raw: str) -> str:
        """
        Clean frequency field - CRITICAL.
        
        This is the most challenging field due to OCR garbage.
        """
        prompt = f"""You are a prescription frequency interpreter.

TASK: Clean OCR-garbled frequency instructions from prescriptions.

COMMON PATTERNS:
- "1+1+1" = three times daily (morning, noon, evening)
- "1+0+1" = twice daily (morning, evening)
- "1+1" = twice daily
- "2x3" = 2 tablets, 3 times daily
- "Once daily" or "Twice daily" = clear instructions

OCR ERRORS TO FIX:
- "# # #" → "1+1+1"
- "u1 joy 1" → "Once daily" (u1=once, joy=garbage)
- "dar d' 1s ' 1" → "1+1+1" (garbled pattern)
- "T" or "t" often means "+"
- "I" or "l" often means "1"

IMPORTANT:
- If input is "7 days" or "X days", output "duration" (this will be moved to Duration field)
- If completely unclear, output "unclear - review needed"
- DO NOT guess if pattern is ambiguous

EXAMPLES:
Input: "# # # Facefs"
Output: 1+1+1

Input: "u1 joy 1"
Output: Once daily

Input: "1T1 3 days"
Output: 1+1

Input: "7 days"
Output: duration

Input: "dar d' 1s ' 1"
Output: 1+1+1

Now clean this frequency:
Input: "{freq_raw}"
Output:"""
        
        return prompt
    
    @staticmethod
    def build_patient_history_prompt(history_raw: str) -> str:
        """Clean patient history."""
        prompt = f"""You are a medical history text cleaner.

TASK: Clean patient history from OCR output.

RULES:
1. If text is "N A", "NA", "N/A", "nil", "none" → output "None"
2. Fix spelling errors in medical conditions
3. Remove trailing periods and artifacts
4. Keep it brief
5. DO NOT add information

EXAMPLES:
Input: "N A."
Output: None

Input: "diabetus, hypertention"
Output: Diabetes, Hypertension

Input: "leukemia ."
Output: Leukemia

Now clean this history:
Input: "{history_raw}"
Output:"""
        
        return prompt
    
    @staticmethod
    def build_patient_name_prompt(name_raw: str) -> str:
        """Clean patient name."""
        prompt = f"""You are a name text cleaner.

TASK: Clean patient name from OCR.

RULES:
1. Fix capitalization (proper case)
2. Remove trailing periods and artifacts
3. Keep unusual names as-is
4. Output only the name

EXAMPLES:
Input: "john doe ."
Output: John Doe

Input: "Anum"
Output: Anum

Input: "MOHAMMED ALI ."
Output: Mohammed Ali

Now clean this name:
Input: "{name_raw}"
Output:"""
        
        return prompt

# ============================================================================
# FLAN-T5 PROCESSOR
# ============================================================================

class EnhancedMedicalLLMProcessor:
    """
    Process OCR output with FLAN-T5 for intelligent cleaning.
    
    Optimizations:
    - Batch generation where possible
    - Prompt caching
    - Efficient tokenization
    """
    
    def __init__(self, model_choice: str = "flan_t5_xl", device: str = "cuda"):
        self.model_choice = model_choice
        self.device = device if torch.cuda.is_available() else "cpu"
        
        print(f"[INFO] Loading {MODELS[model_choice]['name']} on {self.device}...")
        
        model_name = MODELS[model_choice]["name"]
        
        # Load tokenizer and model
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForSeq2SeqLM.from_pretrained(
            model_name,
            torch_dtype=torch.float16 if self.device == "cuda" else torch.float32,
            device_map="auto" if self.device == "cuda" else None
        )
        
        if self.device == "cpu":
            self.model = self.model.to(self.device)
        
        self.model.eval()
        
        print(f"[INFO] Model loaded successfully")
        
        # Initialize prompt builder
        self.prompts = PromptBuilder()
        
        # Track corrections for audit
        self.corrections_log = []
    
    def generate(self, prompt: str, max_length: int = 128) -> str:
        """
        Generate response from FLAN-T5.
        
        Args:
            prompt: Input prompt
            max_length: Maximum output length
        
        Returns:
            Generated text
        """
        inputs = self.tokenizer(
            prompt,
            return_tensors="pt",
            max_length=512,
            truncation=True
        ).to(self.device)
        
        with torch.no_grad():
            outputs = self.model.generate(
                **inputs,
                max_length=max_length,
                num_beams=4,  # Beam search for better quality
                early_stopping=True,
                temperature=0.7,  # Some randomness for creativity
                do_sample=False  # Deterministic for consistency
            )
        
        result = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        return result.strip()
    
    def clean_diagnosis(self, diagnosis_raw: str) -> str:
        """Clean diagnosis field with LLM."""
        if not diagnosis_raw or diagnosis_raw.strip() == "":
            return None
        
        print(f"\n[LLM] Cleaning Diagnosis...")
        print(f"  Input: {diagnosis_raw}")
        
        prompt = self.prompts.build_diagnosis_prompt(diagnosis_raw)
        cleaned = self.generate(prompt, max_length=150)
        
        print(f"  Output: {cleaned}")
        
        # Log correction
        if cleaned != diagnosis_raw:
            self.corrections_log.append({
                "field": "Diagnosis",
                "original": diagnosis_raw,
                "corrected": cleaned
            })
        
        return cleaned
    
    def clean_medicine_name(self, name_raw: str) -> str:
        """Clean medicine name with LLM."""
        if not name_raw or name_raw.strip() == "":
            return None
        
        prompt = self.prompts.build_medicine_name_prompt(name_raw)
        cleaned = self.generate(prompt, max_length=50)
        
        # Log correction
        if cleaned != name_raw:
            self.corrections_log.append({
                "field": "Medicine_Name",
                "original": name_raw,
                "corrected": cleaned
            })
        
        return cleaned
    
    
    def clean_frequency(self, freq_raw: str) -> Tuple[Optional[str], Optional[str]]:
        """
        Clean frequency field with LLM.
        
        Returns:
            Tuple of (frequency, duration)
            - If output is "duration", returns (None, freq_raw)
            - Otherwise returns (cleaned_freq, None)
        """
        if not freq_raw or freq_raw.strip() == "":
            return None, None
        
        if re.fullmatch(r'\d+\+\d+(\+\d+)?', freq_raw.strip()):
            return freq_raw.strip(), None  # e.g. "1+1+1", "1+0+1"
        if re.search(r'^\d+\s*days?$', freq_raw, re.IGNORECASE):
            days = re.search(r'(\d+)', freq_raw).group(1)
            return None, f"{days} days"
        if re.search(r'(once|twice|thrice)\s*(daily|a\s*day)?', freq_raw, re.IGNORECASE):
            return freq_raw.strip(), None
        
        prompt = self.prompts.build_frequency_prompt(freq_raw)
        cleaned = self.generate(prompt, max_length=50)
        
        # Check if this is actually a duration
        if "duration" in cleaned.lower() or re.search(r'^\d+\s*days?$', freq_raw, re.IGNORECASE):
            # Extract duration
            duration_match = re.search(r'(\d+)\s*days?', freq_raw, re.IGNORECASE)
            if duration_match:
                duration = f"{duration_match.group(1)} days"
                self.corrections_log.append({
                    "field": "Frequency → Duration",
                    "original": freq_raw,
                    "corrected": f"Moved to Duration: {duration}"
                })
                return None, duration
        
        # Log correction
        if cleaned != freq_raw and "unclear" not in cleaned.lower():
            self.corrections_log.append({
                "field": "Frequency",
                "original": freq_raw,
                "corrected": cleaned
            })
        
        # If unclear, return None to flag for review
        if "unclear" in cleaned.lower():
            return None, None
        
        return cleaned, None
    
    def clean_patient_history(self, history_raw: str) -> str:
        """Clean patient history with LLM."""
        if not history_raw or history_raw.strip() == "":
            return None
        
        prompt = self.prompts.build_patient_history_prompt(history_raw)
        cleaned = self.generate(prompt, max_length=100)
        
        # Log correction
        if cleaned != history_raw:
            self.corrections_log.append({
                "field": "Patient_History",
                "original": history_raw,
                "corrected": cleaned
            })
        
        return cleaned
    
    def clean_patient_name(self, name_raw: str) -> str:
        """Clean patient name with LLM."""
        if not name_raw or name_raw.strip() == "":
            return None
        
        prompt = self.prompts.build_patient_name_prompt(name_raw)
        cleaned = self.generate(prompt, max_length=50)
        
        return cleaned
    
    def process_prescription(self, phase1_data: Dict) -> Dict:
        """
        Main processing function: Convert Phase 1 to Phase 2.
        
        Returns clean dictionary with ONLY the 16 core fields.
        """
        print("\n" + "="*80)
        print("PHASE 2: LLM POST-PROCESSING")
        print("="*80)
        
        # Reset corrections log
        self.corrections_log = []
        
        output = {
            "patient_info": {},
            "vitals": {},
            "clinical_info": {},
            "prescription": [],
            "audit": {
                "llm_corrections": [],
                "processing_timestamp": datetime.now().isoformat()
            }
        }
        
        # ====================================================================
        # 1. PATIENT INFO (4 fields)
        # ====================================================================
        patient_raw = phase1_data.get("patient_info", {})
        
        # Patient_Name - clean with LLM
        name_raw = patient_raw.get("Patient_Name", {}).get("value", "")
        if name_raw:
            output["patient_info"]["Patient_Name"] = self.clean_patient_name(name_raw)
        else:
            output["patient_info"]["Patient_Name"] = None
        
        # Age, Gender, Date - already cleaned by normalizer
        output["patient_info"]["Age"] = patient_raw.get("Age", {}).get("value")
        output["patient_info"]["Gender"] = patient_raw.get("Gender", {}).get("value")
        output["patient_info"]["Date"] = patient_raw.get("Date", {}).get("value")
        
        # ====================================================================
        # 2. VITALS (3 fields) - Already cleaned by normalizer
        # ====================================================================
        vitals_raw = phase1_data.get("vitals", {})
        
        output["vitals"]["Weight"] = vitals_raw.get("Weight", {}).get("value")
        output["vitals"]["Temperature"] = vitals_raw.get("Temperature", {}).get("value")
        output["vitals"]["Blood_Pressure"] = vitals_raw.get("Blood_Pressure", {}).get("value")
        
        # ====================================================================
        # 3. CLINICAL INFO (2 fields)
        # ====================================================================
        clinical_raw = phase1_data.get("clinical_info", {})
        
        # Diagnosis - CRITICAL CLEANING
        diagnosis_raw = clinical_raw.get("Diagnosis", {}).get("value", "")
        if diagnosis_raw:
            output["clinical_info"]["Diagnosis"] = self.clean_diagnosis(diagnosis_raw)
        else:
            output["clinical_info"]["Diagnosis"] = None
        
        # Patient History
        history_raw = clinical_raw.get("Patient_History", {}).get("value", "")
        if history_raw:
            output["clinical_info"]["Patient_History"] = self.clean_patient_history(history_raw)
        else:
            output["clinical_info"]["Patient_History"] = None
        
        # ====================================================================
        # 4. PRESCRIPTIONS (7 fields per medicine)
        # ====================================================================
        prescriptions_raw = phase1_data.get("prescription", [])
        
        for idx, med_raw in enumerate(prescriptions_raw, 1):
            if not med_raw:  # Skip empty blocks
                continue
            
            print(f"\n[LLM] Processing Medicine #{idx}...")
            
            medicine_entry = {}
            
            # Medicine_Name - clean with LLM
            name_raw = med_raw.get("Medicine_Name", {}).get("value", "")
            if name_raw:
                medicine_entry["Medicine_Name"] = self.clean_medicine_name(name_raw)
                print(f"  Medicine: {name_raw} → {medicine_entry['Medicine_Name']}")
            else:
                medicine_entry["Medicine_Name"] = None
            
            # Medicine_Type - already normalized
            medicine_entry["Medicine_Type"] = med_raw.get("Medicine_Type", {}).get("value")
            
            # Dosage - keep as-is (numbers are critical)
            medicine_entry["Dosage"] = med_raw.get("Dosage", {}).get("value")
            
            # Frequency - clean with LLM
            freq_raw = med_raw.get("Frequency", {}).get("value", "")
            duration_raw = med_raw.get("Duration_to_take_med", {}).get("value", "")
            
            if freq_raw:
                freq_cleaned, duration_extracted = self.clean_frequency(freq_raw)
                medicine_entry["Frequency"] = freq_cleaned
                
                # If duration was extracted from frequency, use it
                if duration_extracted:
                    medicine_entry["Duration_to_take_med"] = duration_extracted
                elif duration_raw:
                    medicine_entry["Duration_to_take_med"] = duration_raw
                else:
                    medicine_entry["Duration_to_take_med"] = None
            else:
                medicine_entry["Frequency"] = None
                medicine_entry["Duration_to_take_med"] = duration_raw if duration_raw else None
            
            # Quantity - keep as-is
            medicine_entry["Quantity"] = med_raw.get("Quantity", {}).get("value")
            
            output["prescription"].append(medicine_entry)
        
        # ====================================================================
        # 5. AUDIT LOG
        # ====================================================================
        output["audit"]["llm_corrections"] = self.corrections_log
        output["audit"]["total_corrections"] = len(self.corrections_log)
        
        print("\n" + "="*80)
        print(f"PHASE 2 COMPLETE - {len(self.corrections_log)} corrections made")
        print("="*80)
        
        return output


# ============================================================================
# BATCH PROCESSING
# ============================================================================

def process_batch_phase2(
    phase1_output_dir: str,
    phase2_output_dir: str,
    model_choice: str = "flan_t5_xl"
):
    """Process Phase 1 outputs through LLM."""
    phase1_dir = Path(phase1_output_dir)
    phase2_dir = Path(phase2_output_dir)
    phase2_dir.mkdir(parents=True, exist_ok=True)
    
    json_files = sorted(list(phase1_dir.glob("*_phase1*.json")))
    
    if not json_files:
        print(f"[ERROR] No Phase 1 JSON files found in {phase1_dir}")
        return
    
    print(f"\n{'='*80}")
    print(f"PHASE 2: ENHANCED LLM POST-PROCESSING - {len(json_files)} files")
    print(f"Model: {MODELS[model_choice]['name']}")
    print(f"{'='*80}\n")
    
    # Load model once
    processor = EnhancedMedicalLLMProcessor(model_choice=model_choice, device="cuda")
    
    for idx, json_path in enumerate(json_files, 1):
        print(f"\n[{idx}/{len(json_files)}] Processing: {json_path.name}")
        
        try:
            with open(json_path, 'r') as f:
                phase1_data = json.load(f)
            
            phase2_data = processor.process_prescription(phase1_data)
            
            output_path = phase2_dir / json_path.name.replace("phase1", "phase2")
            with open(output_path, 'w') as f:
                json.dump(phase2_data, f, indent=2)
            
            corrections_count = len(phase2_data["audit"].get("llm_corrections", []))
            print(f"[SUCCESS] {corrections_count} corrections | {output_path.name}")
            
        except Exception as e:
            print(f"[ERROR] {json_path.name}: {e}")
            import traceback
            traceback.print_exc()

# ============================================================================
# USAGE
# ============================================================================
if __name__ == "__main__":
    process_batch_phase2(
        phase1_output_dir="./output_phase1_raw",
        phase2_output_dir="./output_phase2_clean",
        model_choice="flan_t5_xl"  # or "flan_t5_large" for faster processing
    )