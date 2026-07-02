package com.prescription.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PrescriptionSummaryDTO {
    private Long id;
    private LocalDate prescriptionDate;
    private String status;
    private String prescriptionType;
    private String diagnosis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer medicineCount;
    private Integer interactionCount;

    public PrescriptionSummaryDTO(Long id, LocalDate prescriptionDate,
                                  String status, String prescriptionType,
                                  String diagnosis, LocalDateTime createdAt,
                                  LocalDateTime updatedAt,
                                  Long medicineCount, Long interactionCount) {
        this.id = id;
        this.prescriptionDate = prescriptionDate;
        this.status = status;
        this.prescriptionType = prescriptionType;
        this.diagnosis = diagnosis;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.medicineCount = medicineCount.intValue();
        this.interactionCount = interactionCount.intValue();
    }
}