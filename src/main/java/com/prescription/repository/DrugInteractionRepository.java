package com.prescription.repository;

import com.prescription.entity.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {
    List<DrugInteraction> findByPrescription_PrescriptionId(Long prescriptionId);
}