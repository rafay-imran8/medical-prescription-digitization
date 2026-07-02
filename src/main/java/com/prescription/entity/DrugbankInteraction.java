package com.prescription.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Maps drugbank_schema.drug_interactions — the DrugBank reference table
 * loaded by drugbank_etl.py. Used exclusively for Phase 4 lookups.
 *
 * This is NOT the per-prescription interaction entity.
 * For that, see app_schema.drug_interactions → DrugInteraction.java
 */
@Entity
@Table(name = "drug_interactions", schema = "drugbank_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugbankInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drug1_db_id")
    private String drug1DbId;

    @Column(name = "drug1_rxcui")
    private String drug1Rxcui;

    @Column(name = "drug2_db_id")
    private String drug2DbId;

    @Column(name = "drug2_rxcui")
    private String drug2Rxcui;

    @Column(name = "drug2_name")
    private String drug2Name;

    @Column(name = "severity")
    private String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}