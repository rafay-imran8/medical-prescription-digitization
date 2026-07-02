package com.prescription.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "prescriptions", schema = "app_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prescription_id")
    private Long prescriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonIgnore
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    @JsonIgnore
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "prescription_type", length = 20)
    private PrescriptionType prescriptionType = PrescriptionType.SCANNED;

    @Column(name = "prescription_date")
    private LocalDate prescriptionDate;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "patient_history", columnDefinition = "TEXT")
    private String patientHistory;

    @Column(length = 20)
    private String weight;

    @Column(length = 20)
    private String temperature;

    @Column(name = "blood_pressure", length = 20)
    private String bloodPressure;

    @Type(JsonBinaryType.class)
    @Column(name = "raw_ocr_json", columnDefinition = "jsonb")
    private String rawOcrJson;

    @Type(JsonBinaryType.class)
    @Column(name = "llm_corrected_json", columnDefinition = "jsonb")
    private String llmCorrectedJson;

    @Column(name = "processing_status", length = 50)
    private String processingStatus = "PENDING";

    @Column(name = "image_path", columnDefinition = "TEXT")
    private String imagePath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "prescription", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionMedicine> medicines;

    @OneToMany(mappedBy = "prescription", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionImage> images;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcessingLog> processingLogs;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LlmCorrection> llmCorrections;

    @OneToMany(mappedBy = "prescription", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DrugInteraction> drugInteractions;

    public enum PrescriptionType {
        SCANNED,
        DIGITAL
    }
}