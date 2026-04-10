package com.prescription.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Entity
@Table(name = "duplicate_ingredient_warnings", schema = "app_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuplicateIngredientWarningEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "rxcui")
    private String rxcui;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    // Comma-separated medicine names — split on read
    @Column(name = "medicine_names", columnDefinition = "TEXT")
    private String medicineNames;
}