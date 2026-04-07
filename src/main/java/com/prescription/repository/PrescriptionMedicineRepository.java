package com.prescription.repository;

import com.prescription.entity.PrescriptionMedicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionMedicineRepository extends JpaRepository<PrescriptionMedicine, Long> {

    List<PrescriptionMedicine> findByPrescription_PrescriptionId(Long prescriptionId);

    @Query("SELECT pm.medicineName, COUNT(pm) as count FROM PrescriptionMedicine pm GROUP BY pm.medicineName ORDER BY count DESC")
    List<Object[]> findMostPrescribedMedicines();

    @Query("SELECT pm FROM PrescriptionMedicine pm WHERE pm.rxcui IS NOT NULL AND pm.prescription.prescriptionId = :prescriptionId")
    List<PrescriptionMedicine> findByPrescriptionWithRxcui(@Param("prescriptionId") Long prescriptionId);
}