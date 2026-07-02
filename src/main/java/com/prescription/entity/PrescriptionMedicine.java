package com.prescription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescription_medicines", schema = "app_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionMedicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medicine_id")
    private Long medicineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "medicine_name", nullable = false, length = 255)
    private String medicineName;

    @Column(name = "medicine_type", length = 100)
    private String medicineType;

    @Column(length = 100)
    private String dosage;

    @Column(name = "validation_status", length = 50)
    private String validationStatus;

    @Column(name = "validation_errors", columnDefinition = "text")
    private String validationErrors;

    @Column(length = 100)
    private String frequency;

    @Column(length = 100)
    private String duration;

    @Column(length = 50)
    private String quantity;

    @Column(length = 50)
    private String rxcui;

    @Column(name = "normalized_name", length = 255)
    private String normalizedName;

    @Column(name = "normalization_status", length = 50)
    private String normalizationStatus;

    @Column(name = "normalization_confidence", precision = 5, scale = 3)
    private BigDecimal normalizationConfidence;

    @Column(name = "normalization_method", length = 50)
    private String normalizationMethod;  // NEW: "exact_match", "fuzzy_match", "no_match"

    @Type(JsonBinaryType.class)
    @Column(name = "rxnorm_alternatives", columnDefinition = "jsonb")
    private String rxnormAlternatives;  // NEW: Store alternative matches as JSON

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}