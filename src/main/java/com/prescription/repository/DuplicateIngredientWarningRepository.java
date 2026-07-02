package com.prescription.repository;

import com.prescription.entity.DuplicateIngredientWarningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DuplicateIngredientWarningRepository
        extends JpaRepository<DuplicateIngredientWarningEntity, Long> {
    List<DuplicateIngredientWarningEntity> findByPrescription_PrescriptionId(Long prescriptionId);
}