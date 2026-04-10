import re
import csv
import json
from datetime import datetime
from db_connection import get_connection, return_connection
# After phase3_result is ready, before building final_prescription

# ── DUPLICATE RXCUI CHECK ─────────────────────────────────────────────
from collections import defaultdict




class DrugNormalizer:
    """
    Phase 3: Drug name normalization using RxNorm.

    Pipeline per medicine:
        1. LOCAL_BRAND_MAP  (hardcoded, Pakistan-specific)
        2. RxNorm EXACT match only (no fuzzy, no LLM)
        3. DANGEROUS_CLASSES safety block
        4. Medicine-type consistency check
        5. Fail cleanly → log to unmatched_drugs.csv
    """

    # =========================================================================
    # LOCAL BRAND MAP — Pakistan regional brands → RxNorm generic names
    # =========================================================================
    LOCAL_BRAND_MAP = {

        # ── INN NAMES RXNORM STORES DIFFERENTLY ──────────────────────────────
        "paracetamol":          "acetaminophen",
        "salbutamol":           "albuterol",
        "adrenaline":           "epinephrine",
        "noradrenaline":        "norepinephrine",
        "frusemide":            "furosemide",
        "lignocaine":           "lidocaine",
        "pethidine":            "meperidine",
        "amoxycillin":          "amoxicillin",
        "fluace":               "influenza vaccine",

        # ── ANALGESICS / ANTIPYRETICS ─────────────────────────────────────────
        "panadol":              "acetaminophen",
        "calpol":               "acetaminophen",
        "calpal":               "acetaminophen",
        "disprol":              "acetaminophen",
        "tempra":               "acetaminophen",
        "febrol":               "acetaminophen",
        "nuberol":              "acetaminophen",
        "codogesic":            "acetaminophen",
        "wilgesic":             "acetaminophen",

        # NSAIDs
        "brufen":               "ibuprofen",
        "actifen":              "ibuprofen",
        "nurofen":              "ibuprofen",
        "froben":               "flurbiprofen",
        "ansaid":               "flurbiprofen",
        "artifen":              "diclofenac",
        "dicloran":             "diclofenac",
        "voren":                "diclofenac",
        "voltaren":             "diclofenac",
        "cataflam":             "diclofenac",
        "nims":                 "nimesulide",
        "unix":                 "nimesulide",
        "synflex":              "naproxen",
        "naprosyn":             "naproxen",
        "flexin":               "naproxen",
        "brexin":               "piroxicam",
        "feldene":              "piroxicam",
        "ponstan":              "mefenamic acid",
        "toradol":              "ketorolac",
        "tramal":               "tramadol",
        "ultram":               "tramadol",

        # ── ANTIBIOTICS: ORAL ─────────────────────────────────────────────────
        "amoxil":               "amoxicillin",
        "wymox":                "amoxicillin",
        "moxatag":              "amoxicillin",
        "augmentin":            "amoxicillin clavulanate",
        "calamox":              "amoxicillin clavulanate",
        "clavam":               "amoxicillin clavulanate",
        "velosef":              "cephradine",
        "cefiget":              "cefixime",
        "cefspan":              "cefixime",
        "cefim":                "cefixime",
        "suprax":               "cefixime",
        "keflex":               "cephalexin",
        "sporidex":             "cephalexin",

        # Fluoroquinolones
        "ciprin":               "ciprofloxacin",
        "novidat":              "ciprofloxacin",
        "hiflox":               "ciprofloxacin",
        "mercip":               "ciprofloxacin",
        "cifran":               "ciprofloxacin",
        "ciproxin":             "ciprofloxacin",
        "leflox":               "levofloxacin",
        "levo":                 "levofloxacin",
        "bexus":                "levofloxacin",
        "tavanic":              "levofloxacin",
        "veprox":               "levofloxacin",
        "oflobid":              "ofloxacin",
        "avelox":               "moxifloxacin",
        "moxiget":              "moxifloxacin",

        # Macrolides
        "zithromax":            "azithromycin",
        "zithrocin":            "azithromycin",
        "zetamax":              "azithromycin",
        "zmax":                 "azithromycin",
        "azitec":               "azithromycin",
        "atm":                  "azithromycin",
        "klaricid":             "clarithromycin",
        "rithmo":               "clarithromycin",
        "neoklar":              "clarithromycin",
        "klacid":               "clarithromycin",
        "erythrocin":           "erythromycin",
        "ilosone":              "erythromycin",

        # Nitroimidazoles
        "flagyl":               "metronidazole",
        "abozole":              "metronidazole",
        "metrozine":            "metronidazole",

        # Clindamycin / Tetracyclines / Sulfonamides
        "dalacin":              "clindamycin",
        "vibramycin":           "doxycycline",
        "doxylin":              "doxycycline",
        "septran":              "trimethoprim sulfamethoxazole",
        "bactrim":              "trimethoprim sulfamethoxazole",

        # ── ANTIBIOTICS: PARENTERAL ───────────────────────────────────────────
        "nebcin":               "tobramycin",
        "tobi":                 "tobramycin",
        "tobrex":               "tobramycin",
        "genticyn":             "gentamicin",
        "garamycin":            "gentamicin",
        "grasil":               "gentamicin",
        "rocephin":             "ceftriaxone",
        "oxidil":               "ceftriaxone",
        "inocef":               "ceftriaxone",
        "cefotax":              "cefotaxime",
        "baxim":                "cefuroxime",
        "zinacef":              "cefuroxime",
        "zefamax":              "cefuroxime",
        "fortum":               "ceftazidime",
        "tazocin":              "piperacillin tazobactam",
        "tanzo":                "piperacillin tazobactam",
        "tienam":               "imipenem cilastatin",
        "meronem":              "meropenem",
        "targocid":             "teicoplanin",
        "ecasil":               "teicoplanin",
        "ampiclox":             "ampicillin cloxacillin",

        # ── ANTITUBERCULARS ───────────────────────────────────────────────────
        "myrin":                "isoniazid rifampicin ethambutol pyrazinamide",
        "rimcure":              "isoniazid rifampicin ethambutol",
        "rimactane":            "rifampicin",
        "rimactal":             "rifampicin",
        "inh":                  "isoniazid",

        # ── GASTROINTESTINAL ──────────────────────────────────────────────────
        "risek":                "omeprazole",
        "losec":                "omeprazole",
        "omega":                "omeprazole",
        "omez":                 "omeprazole",
        "nexium":               "esomeprazole",
        "controloc":            "pantoprazole",
        "pantor":               "pantoprazole",
        "somac":                "pantoprazole",
        "zantac":               "ranitidine",
        "aciloc":               "ranitidine",
        "tagamet":              "cimetidine",
        "motilium":             "domperidone",
        "maxolon":              "metoclopramide",
        "primperan":            "metoclopramide",
        "imodium":              "loperamide",
        "buscopan":             "hyoscine butylbromide",

        # ── CARDIOVASCULAR ────────────────────────────────────────────────────
        "zestril":              "lisinopril",
        "ziscar":               "lisinopril",
        "lispril":              "lisinopril",
        "capoten":              "captopril",
        "renitec":              "enalapril",
        "vasotec":              "enalapril",
        "cozaar":               "losartan",
        "losacar":              "losartan",
        "diovan":               "valsartan",
        "micardis":             "telmisartan",
        "pritor":               "telmisartan",
        "tenormin":             "atenolol",
        "aten":                 "atenolol",
        "concor":               "bisoprolol",
        "carvedil":             "carvedilol",
        "inderal":              "propranolol",
        "norvasc":              "amlodipine",
        "amlopin":              "amlodipine",
        "isoptin":              "verapamil",
        "adalat":               "nifedipine",
        "lipitor":              "atorvastatin",
        "sortis":               "atorvastatin",
        "zocor":                "simvastatin",
        "crestor":              "rosuvastatin",
        "disprin":              "aspirin",
        "plavix":               "clopidogrel",
        "clodrel":              "clopidogrel",
        "clexane":              "enoxaparin",
        "lasix":                "furosemide",
        "frusenex":             "furosemide",
        "aldactone":            "spironolactone",
        "isoket":               "isosorbide dinitrate",
        "nitrostat":            "nitroglycerin",

        # ── DIABETES ──────────────────────────────────────────────────────────
        "glucophage":           "metformin",
        "amaryl":               "glimepiride",
        "diamicron":            "gliclazide",
        "daonil":               "glibenclamide",
        "mixtard":              "insulin human",
        "actrapid":             "insulin human",
        "lantus":               "insulin glargine",
        "januvia":              "sitagliptin",
        "galvus":               "vildagliptin",

        # ── RESPIRATORY ───────────────────────────────────────────────────────
        "ventolin":             "albuterol",
        "asthalin":             "albuterol",
        "salmeter":             "salmeterol",
        "serevent":             "salmeterol",
        "spiriva":              "tiotropium",
        "atrovent":             "ipratropium",
        "pulmicort":            "budesonide",
        "flixotide":            "fluticasone",
        "benadryl":             "diphenhydramine",
        "sudafed":              "pseudoephedrine",
        "bisolvon":             "bromhexine",
        "mucosolvan":           "ambroxol",

        # ── ALLERGY / ANTIHISTAMINES ──────────────────────────────────────────
        "piriton":              "chlorpheniramine",
        "avil":                 "pheniramine",
        "phenergan":            "promethazine",
        "clarityne":            "loratadine",
        "claritin":             "loratadine",
        "zyrtec":               "cetirizine",
        "alerid":               "cetirizine",
        "xyzal":                "levocetirizine",
        "telfast":              "fexofenadine",

        # ── CNS / NEUROLOGICAL ────────────────────────────────────────────────
        "rivotril":             "clonazepam",
        "epilim":               "sodium valproate",
        "encorate":             "sodium valproate",
        "tegretol":             "carbamazepine",
        "dilantin":             "phenytoin",
        "epitoin":              "phenytoin",
        "neurontin":            "gabapentin",
        "lyrica":               "pregabalin",
        "prozac":               "fluoxetine",
        "depran":               "fluoxetine",
        "zoloft":               "sertraline",
        "serlift":              "sertraline",
        "risnia":               "risperidone",
        "risperdal":            "risperidone",
        "xanax":                "alprazolam",
        "lexotanil":            "bromazepam",
        "valium":               "diazepam",
        "imigran":              "sumatriptan",

        # ── MUSCULOSKELETAL ───────────────────────────────────────────────────
        "muscoril":             "thiocolchicoside",
        "thiolax":              "thiocolchicoside",
        "ternelin":             "tizanidine",
        "movax":                "tizanidine",

        # ── STEROIDS ──────────────────────────────────────────────────────────
        "deltacortil":          "prednisolone",
        "dexona":               "dexamethasone",
        "decadron":             "dexamethasone",
        "tobradex":             "tobramycin dexamethasone",

        # ── VITAMINS / SUPPLEMENTS ────────────────────────────────────────────
        "bisleri":              "ferrous sulfate folic acid",
        "neurobion":            "vitamin b complex",
        "becosules":            "vitamin b complex",
        "vitacal":              "calcium carbonate",
        "shelcal":              "calcium carbonate",
        "calcimax":             "calcium carbonate vitamin d",
        "ostocalcium":          "calcium carbonate vitamin d",
        "fefol":                "ferrous sulfate folic acid",
        "hemfer":               "ferrous sulfate",
        "ferlin":               "ferrous sulfate",
        "zincon":               "zinc sulfate",
        "wellwoman":            "multivitamin mineral",
        "wellman":              "multivitamin mineral",
        "supradyn":             "multivitamin mineral",
        "dayamin":              "multivitamin mineral",
        "zincovit":             "zinc multivitamin",
        "centrum":              "multivitamin mineral",
        "pharmaton":            "multivitamin mineral",

        # ── VACCINES ──────────────────────────────────────────────────────────
        "varivac":              "varicella virus vaccine live",
        "varilrix":             "varicella virus vaccine live",
        "varilix":              "varicella virus vaccine live",
        "engerix":              "hepatitis b vaccine",
        "euvax":                "hepatitis b vaccine",

        # ── ANTIFUNGALS ───────────────────────────────────────────────────────
        "diflucan":             "fluconazole",
        "flucoral":             "fluconazole",
        "nizoral":              "ketoconazole",
        "funazol":              "ketoconazole",
        "ketovate":             "ketoconazole",
        "canesten":             "clotrimazole",
        "lamisil":              "terbinafine",

        # ── ANTIPARASITICS ────────────────────────────────────────────────────
        "zentel":               "albendazole",
        "eskazole":             "albendazole",
        "andazol":              "albendazole",
        "almex":                "albendazole",
        "vermox":               "mebendazole",
        "mebex":                "mebendazole",
        "stromectol":           "ivermectin",
        "ketress":              "levamisole",
        "decaris":              "levamisole",
        "ergamisol":            "levamisole",

        # ── OPHTHALMOLOGY ─────────────────────────────────────────────────────
        "maxitrol":             "neomycin polymyxin dexamethasone",
        "timoptic":             "timolol",

        # ── THYROID ───────────────────────────────────────────────────────────
        "eltroxin":             "levothyroxine",
        "synthroid":            "levothyroxine",
        "euthyrox":             "levothyroxine",

        # ── UROLOGY ───────────────────────────────────────────────────────────
        "flomax":               "tamsulosin",
        "prostamide":           "tamsulosin",
        "proscar":              "finasteride",
        "propecia":             "finasteride",
    }

    # =========================================================================
    # DANGEROUS DRUG CLASSES
    # =========================================================================
    DANGEROUS_CLASSES = {
        # Chemotherapy
        "fluorouracil", "methotrexate", "cyclophosphamide",
        "vincristine", "doxorubicin", "cisplatin", "carboplatin",
        "bleomycin", "paclitaxel", "docetaxel", "tamoxifen",
        "thalidomide", "isotretinoin", "azathioprine",
        "cyclosporine", "tacrolimus",
        # Narrow therapeutic index / high risk
        "warfarin", "digoxin", "lithium", "phenytoin",
        "colchicine", "clozapine",
        # MAOIs
        "phenelzine", "tranylcypromine", "selegiline",
        # Insulins
        "insulin glargine", "insulin human",
    }

    # =========================================================================
    # CHEMO DRUGS — never in outpatient oral/syrup form
    # =========================================================================
    CHEMO_DRUGS = {
        "fluorouracil", "methotrexate", "tamoxifen", "paclitaxel",
        "doxorubicin", "cisplatin", "cyclophosphamide", "bleomycin",
        "vincristine", "docetaxel", "thalidomide", "azathioprine",
    }

    # =========================================================================
    # SUPPLEMENT GENERICS — won't exist in RxNorm IN/PIN
    # Return mapped name directly without RxNorm lookup
    # =========================================================================
    SUPPLEMENT_GENERICS = {
        "multivitamin mineral",
        "vitamin b complex",
        "calcium carbonate vitamin d",
        "ferrous sulfate folic acid",
        "zinc multivitamin",
        "influenza vaccine",
        "hepatitis b vaccine",
        "varicella virus vaccine live",
    }

    # =========================================================================
    # SAFETY METHODS
    # =========================================================================

    @staticmethod
    def strip_dosage(name: str) -> str:
        """
        'Zetamax 1000mg' → 'Zetamax'
        'calpal 6 Plus'  → 'calpal 6 Plus'  (no unit = don't strip)
        'Panadol 500 mg' → 'Panadol'
        """
        # Only strip numbers that are followed by a known unit OR at end of string
        return re.sub(
            r'\s+\d+\s*(mg|ml|mcg|g|iu|units?|tab|cap)(\b|$)',
            '', name, flags=re.IGNORECASE
        ).strip()

    @staticmethod
    def is_dangerous_mismatch(original_name: str, normalized_name: str) -> bool:
        """
        Block if a brand resolved to a high-risk drug with no
        plausible character-level connection.
        """
        normalized_lower = normalized_name.lower() if normalized_name else ""
        original_lower   = original_name.lower() if original_name else normalized_lower

        for dangerous in DrugNormalizer.DANGEROUS_CLASSES:
            if dangerous in normalized_lower:
                if (original_lower[:4] not in dangerous and
                        dangerous[:4] not in original_lower):
                    print(f"  🚨 SAFETY BLOCK: '{original_name}' → '{normalized_name}' rejected")
                    return True
        return False

    @staticmethod
    def type_consistency_check(medicine_type: str, normalized_name: str) -> bool:
        """
        Returns True (= reject) if a chemo drug appeared as oral/syrup.
        """
        if not normalized_name or not medicine_type:
            return False
        norm_lower = normalized_name.lower()
        type_lower = medicine_type.lower()
        if type_lower in ('syrup', 'capsule', 'tablet', 'cap', 'tab', 'syp'):
            for chemo in DrugNormalizer.CHEMO_DRUGS:
                if chemo in norm_lower:
                    print(f"  🚨 TYPE BLOCK: {medicine_type} + {normalized_name} rejected")
                    return True
        return False

    # =========================================================================
    # CORE RXNORM LOOKUP — exact match only, no fuzzy, no LLM
    # =========================================================================

    @staticmethod
    def normalize_drug(medicine_name: str, cur, original_name: str = None) -> dict:
        """
        Exact-match a generic name against RxNorm IN/PIN entries.
        No fuzzy matching — wrong confident matches are more dangerous
        than honest failures.
        """
        cur.execute("""
            SELECT rxcui, str, sab, tty
            FROM rxnorm_schema.rxnconso
            WHERE LOWER(str) = LOWER(%s)
              AND sab = 'RXNORM'
              AND tty IN ('IN', 'PIN')
            LIMIT 5
        """, (medicine_name,))

        exact_matches = cur.fetchall()

        if exact_matches:
            check_name = original_name or medicine_name
            if DrugNormalizer.is_dangerous_mismatch(check_name, exact_matches[0][1]):
                return {
                    'rxcui':           None,
                    'normalized_name': None,
                    'confidence':      0.0,
                    'method':          'safety_blocked',
                    'alternatives':    []
                }
            return {
                'rxcui':           exact_matches[0][0],
                'normalized_name': exact_matches[0][1],
                'confidence':      1.0,
                'method':          'exact_match',
                'alternatives':    [
                    {'rxcui': r[0], 'name': r[1], 'tty': r[3]}
                    for r in exact_matches[1:]
                ]
            }

        # No fuzzy — fail cleanly
        return {
            'rxcui':           None,
            'normalized_name': None,
            'confidence':      0.0,
            'method':          'no_match',
            'alternatives':    []
        }

    # =========================================================================
    # MAIN ENTRY POINT
    # =========================================================================

    @staticmethod
    def process_prescription(llm_output: dict) -> dict:
        """
        Normalize all medicines in a Phase 2 prescription output.

        Pipeline per medicine:
            Step 0 → LOCAL_BRAND_MAP (brand → known generic)
                     Supplements returned directly (no RxNorm)
            Step 1 → RxNorm exact match on clean name (if map missed)
            Step 2 → DANGEROUS_CLASSES check   (inside normalize_drug)
            Step 3 → Type consistency check
            Step 4 → Fail cleanly, log to unmatched_drugs.csv
        """
        conn = get_connection()
        cur  = conn.cursor()

        try:
            results = []

            for medicine in llm_output['prescription']:
                medicine_name = medicine.get('Medicine_Name')
                medicine_type = medicine.get('Medicine_Type', '')

                # ── Guard: no medicine name ───────────────────────────────
                if not medicine_name:
                    results.append({
                        'original_name':        None,
                        'medicine_type':        medicine_type,
                        'dosage':               medicine.get('Dosage'),
                        'frequency':            medicine.get('Frequency'),
                        'duration_to_take_med': medicine.get('Duration_to_take_med'),
                        'quantity':             medicine.get('Quantity'),
                        'rxcui':                None,
                        'normalized_name':      None,
                        'confidence':           0.0,
                        'method':               'no_name',
                        'status':               'failed',
                        'alternatives':         []
                    })
                    continue

                norm_result = None

                # Strip dosage suffix once, reuse everywhere
                clean_name   = DrugNormalizer.strip_dosage(medicine_name)
                medicine_key = re.sub(r'[^a-z]', '', clean_name.lower().split()[0])

                print(f"  key='{medicine_key}' in_map={medicine_key in DrugNormalizer.LOCAL_BRAND_MAP}")

                # ── Step 0: LOCAL_BRAND_MAP ───────────────────────────────
                if medicine_key in DrugNormalizer.LOCAL_BRAND_MAP:
                    mapped_generic = DrugNormalizer.LOCAL_BRAND_MAP[medicine_key]
                    print(f"  📖 Map hit: {medicine_name} → {mapped_generic}") 

                    if mapped_generic.lower().strip() in DrugNormalizer.SUPPLEMENT_GENERICS:
                        # Supplements won't exist in RxNorm IN/PIN
                        # Return directly without DB lookup
                        norm_result = {
                            'rxcui':           None,
                            'normalized_name': mapped_generic,
                            'confidence':      1.0,
                            'method':          'supplement_map',
                            'alternatives':    []
                        }
                        print(f"  💊 Supplement: {medicine_name} → {mapped_generic}")
                    else:
                        local_result = DrugNormalizer.normalize_drug(
                            mapped_generic, cur, original_name=medicine_name
                        )
                        if local_result['rxcui']:
                            norm_result = local_result
                            print(f"  ✅ Matched: {medicine_name} → {mapped_generic} "
                                  f"(rxcui={local_result['rxcui']})")

                # ── Step 1: RxNorm exact on clean name (if map missed) ────
                if not norm_result:
                    direct_result = DrugNormalizer.normalize_drug(
                        clean_name, cur, original_name=medicine_name
                    )
                    if direct_result['rxcui']:
                        norm_result = direct_result
                        print(f"  ✅ Direct RxNorm: {clean_name} "
                              f"(rxcui={direct_result['rxcui']})")

                # ── Step 2: Type consistency check ────────────────────────
                if norm_result and norm_result['rxcui']:
                    if DrugNormalizer.type_consistency_check(
                        medicine_type, norm_result['normalized_name']
                    ):
                        norm_result = {
                            'rxcui':           None,
                            'normalized_name': None,
                            'confidence':      0.0,
                            'method':          'type_mismatch_blocked',
                            'alternatives':    []
                        }

                # ── Step 3: Fail cleanly ──────────────────────────────────
                if not norm_result:
                    norm_result = {
                        'rxcui':           None,
                        'normalized_name': None,
                        'confidence':      0.0,
                        'method':          'no_match',
                        'alternatives':    []
                    }

                results.append({
                    'original_name':        medicine_name,
                    'medicine_type':        medicine_type,
                    'dosage':               medicine.get('Dosage'),
                    'frequency':            medicine.get('Frequency'),
                    'duration_to_take_med': medicine.get('Duration_to_take_med'),
                    'quantity':             medicine.get('Quantity'),
                    'rxcui':                norm_result['rxcui'],
                    'normalized_name':      norm_result['normalized_name'],
                    'confidence':           norm_result['confidence'],
                    'method':               norm_result['method'],
                    'status':               'completed' if (
                                                norm_result['rxcui'] or
                                                norm_result['method'] == 'supplement_map'
                                            ) else 'failed',
                    'alternatives':         norm_result['alternatives']
                })

            # ── Log unmatched for future map expansion ────────────────────
            unmatched = [
                r['original_name'] for r in results
                if r['status'] == 'failed' and r['original_name']
                and r['method'] not in ('safety_blocked', 'type_mismatch_blocked')
            ]
            if unmatched:
                with open('unmatched_drugs.csv', 'a', newline='') as f:
                    writer = csv.writer(f)
                    for name in unmatched:
                        writer.writerow([name, datetime.now().isoformat()])
                        print(f"  ❌ Unmatched (logged): {name}")

            return {'normalization_results': results}

        except Exception as e:
            print(f"Error in DrugNormalizer.process_prescription: {e}")
            raise

        finally:
            cur.close()
            return_connection(conn)


# =============================================================================
# TEST
# =============================================================================
if __name__ == "__main__":
    from config import DB_CONFIG
    from db_connection import DatabaseConnection

    DatabaseConnection.initialize_pool(**DB_CONFIG)

    sample_prescription = {
        "patient_info": {
            "Patient_Name": "Anum",
            "Age": "42",
            "Gender": "female",
            "Date": "12-02-2026"
        },
        "vitals": {
            "Weight": "59",
            "Temperature": "102",
            "Blood_Pressure": "133/89"
        },
        "clinical_info": {
            "Diagnosis": "Prophylaxis of chickenpox, Fever, Pain",
            "Patient_History": "None"
        },
        "prescription": [
            {
                "Medicine_Name": "calpal 6 Plus",
                "Medicine_Type": "capsule",
                "Dosage": None,
                "Frequency": None,
                "Duration_to_take_med": "7 days",
                "Quantity": None
            },
            {
                "Medicine_Name": "Panadol Extra",
                "Medicine_Type": "capsule",
                "Dosage": None,
                "Frequency": "1+1+1",
                "Duration_to_take_med": "0.7",
                "Quantity": None
            },
            {
                "Medicine_Name": "Bisleri",
                "Medicine_Type": "capsule",
                "Dosage": None,
                "Frequency": "1+0+1",
                "Duration_to_take_med": "30 days",
                "Quantity": None
            },
            {
                "Medicine_Name": "Ketress",
                "Medicine_Type": "syrup",
                "Dosage": "500 mg",
                "Frequency": "1+0+1",
                "Duration_to_take_med": "30 days",
                "Quantity": None
            },
            {
                "Medicine_Name": "Wellwoman",
                "Medicine_Type": "tablet",
                "Dosage": None,
                "Frequency": "1+0+1",
                "Duration_to_take_med": None,
                "Quantity": None
            }
        ]
    }

    result = DrugNormalizer.process_prescription(sample_prescription)
    print(json.dumps(result, indent=2))

    DatabaseConnection.close_all_connections()