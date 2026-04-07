package com.prescription.repository;

import com.prescription.entity.PrescriptionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrescriptionImageRepository extends JpaRepository<PrescriptionImage, Long> {

    Optional<PrescriptionImage> findByPrescription_PrescriptionId(Long prescriptionId);

    Optional<PrescriptionImage> findByStoredFilename(String storedFilename);
}