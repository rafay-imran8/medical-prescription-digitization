//package com.prescription.dto;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class PrescriptionResponse {
//
//    @JsonProperty("id")
//    private Long prescriptionId;
//    private String patientName;
//    private String doctorName;
//    private LocalDateTime createdAt;
//    private String status;
//    private String imageUrl;
//
//    // ── Normalized medicines from Phase 3 ─────────────────────────────────
//    private List<MedicineResult> medicines;
//
//    // ── Phase 4a: Drug-Drug Interactions ──────────────────────────────────
//    private List<DrugInteractionResult> drugInteractions;
//
//    // ── Phase 4b: Drug→Disease warnings ──────────────────────────────────
//    private List<DrugDiseaseWarning> drugDiseaseWarnings;
//
//    // ── Summary counts for UI badges ──────────────────────────────────────
//    private int highSeverityCount;
//    private int moderateSeverityCount;
//
//    // =========================================================================
//    // Nested DTOs
//    // =========================================================================
//
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @Builder
//    public static class MedicineResult {
//        private String originalName;
//        private String normalizedName;
//        private String rxcui;
//        private String medicineType;
//        private String dosage;
//        private String frequency;
//        private String duration;
//        private String quantity;
//        private double confidence;
//        private String method;     // exact_match | supplement_map | no_match | safety_blocked
//        private String status;     // completed | failed
//    }
//
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @Builder
//    public static class DrugInteractionResult {
//        private String drug1Name;
//        private String drug1Rxcui;
//        private String drug2Name;
//        private String drug2Rxcui;
//        private String severity;      // HIGH | MODERATE | LOW | UNKNOWN
//        private String description;
//        private String source;        // DRUGBANK
//    }
//
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @Builder
//    public static class DrugDiseaseWarning {
//        private String drugName;
//        private String rxcui;
//        private String indicationText;
//        private List<String> meshTerms;
//    }
//}


package com.prescription.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {

    @JsonProperty("id")
    private Long prescriptionId;
    private String patientName;
    private String doctorName;
    private LocalDateTime createdAt;
    private String status;
    private String imageUrl;

    // ── Normalized medicines from Phase 3 ─────────────────────────────────
    private List<MedicineResult> medicines;

    // ── Phase 4a: Drug-Drug Interactions ──────────────────────────────────
    private List<DrugInteractionResult> drugInteractions;

    // ── Phase 4b: Drug→Disease warnings ───────────────────────────────────
    private List<DrugDiseaseWarning> drugDiseaseWarnings;

    // ── Duplicate active ingredient notices ───────────────────────────────
    @JsonProperty("duplicateIngredientWarnings")
    private List<DuplicateIngredientWarning> duplicateIngredientWarnings;

    // ── Summary counts for UI badges ──────────────────────────────────────
    private int highSeverityCount;
    private int moderateSeverityCount;

    // =========================================================================
    // Nested DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicineResult {
        private String originalName;
        private String normalizedName;
        private String rxcui;
        private String medicineType;
        private String dosage;
        private String frequency;
        private String duration;
        private String quantity;
        private double confidence;
        private String method;     // exact_match | supplement_map | no_match | safety_blocked
        private String status;     // completed | failed
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DrugInteractionResult {
        private String drug1Name;
        private String drug1Rxcui;
        private String drug2Name;
        private String drug2Rxcui;
        private String severity;      // HIGH | MODERATE | LOW | UNKNOWN
        private String description;
        private String source;        // DRUGBANK
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DrugDiseaseWarning {
        private String drugName;
        private String rxcui;
        private String indicationText;
        private List<String> meshTerms;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DuplicateIngredientWarning {
        private String rxcui;
        private List<String> medicines;
        private String message;
    }
}
