package com.prescription.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePrescriptionRequest {
    private Long patientId;
    private LocalDate prescriptionDate;
    private String diagnosis;
    private String patientHistory;

    // Vitals
    private String weight;
    private String temperature;
    private String bloodPressure;

    // Medicines
    private List<MedicineRequest> medicines;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MedicineRequest {
        private String medicineName;
        private String medicineType;
        private String dosage;
        private String frequency;
        private String duration;
        private String quantity;
    }
}