package com.prescription.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PrescriptionResponse {
    private Long prescriptionId;
    private String prescriptionType;
    private LocalDate prescriptionDate;
    private String diagnosis;
    private String patientHistory;
    private String weight;
    private String temperature;
    private String bloodPressure;
    private String processingStatus;
    private LocalDateTime createdAt;
    private String imagePath;

    private PatientInfo patientInfo;
    private DoctorInfo doctorInfo;
    private List<MedicineInfo> medicines;
    private List<InteractionInfo> interactions;
    private ImageInfo imageInfo;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PatientInfo {
        private Long patientId;
        private String patientUniqueId;
        private String patientName;
        private Integer age;
        private String gender;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DoctorInfo {
        private Long doctorId;
        private String doctorUniqueId;
        private String doctorName;
        private String specialization;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MedicineInfo {
        private Long medicineId;
        private String medicineName;
        private String medicineType;
        private String dosage;
        private String frequency;
        private String duration;
        private String quantity;
        private String rxcui;
        private String normalizedName;  // RxNorm matched name
        private String normalizationStatus;  // "completed", "failed", "not_found"
        private Double normalizationConfidence;  // 0.0 to 1.0
        private String normalizationMethod;  // "exact_match", "fuzzy_match", "no_match"
        private List<RxNormAlternative> alternatives;  // Alternative matches
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RxNormAlternative {
        private String rxcui;
        private String name;
        private String tty;  // Term type
        private Double similarity;  // For fuzzy matches
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InteractionInfo {
        private String drug1Name;
        private String drug2Name;
        private String severity;
        private String description;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageInfo {
        private String storedFilename;
        private String originalFilename;
        private Long fileSizeBytes;
        private LocalDateTime uploadedAt;
    }
}