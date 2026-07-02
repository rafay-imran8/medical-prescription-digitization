package com.prescription.repository;

import com.prescription.entity.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for app_schema.drug_interactions.
 * Stores per-prescription DDI rows saved by PrescriptionService at upload time.
 *
 * DO NOT confuse with DrugbankInteractionRepository which queries
 * drugbank_schema.drug_interactions for Phase 4 reference lookups.
 */
@Repository
public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {

    List<DrugInteraction> findByPrescription_PrescriptionId(Long prescriptionId);

    List<DrugInteraction> findByPrescription_PrescriptionIdAndSeverity(
            Long prescriptionId, String severity);
}