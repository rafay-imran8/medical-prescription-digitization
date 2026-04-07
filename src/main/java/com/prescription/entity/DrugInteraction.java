package com.prescription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "drug_interactions", schema = "app_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interaction_id")
    private Long interactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(name = "drug1_rxcui", length = 20)
    private String drug1Rxcui;

    @Column(name = "drug1_name", length = 255)
    private String drug1Name;

    @Column(name = "drug2_rxcui", length = 20)
    private String drug2Rxcui;

    @Column(name = "drug2_name", length = 255)
    private String drug2Name;

    @Column(length = 50)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String source; // "DRUGBANK", "ATC_CLASS", "RXNREL"

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


}