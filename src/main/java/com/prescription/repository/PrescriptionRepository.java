package com.prescription.repository;

import com.prescription.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.prescription.dto.PrescriptionSummaryDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    // For list view - only fetch patient and doctor
    @Query("""
        SELECT DISTINCT p FROM Prescription p
        JOIN FETCH p.patient pat
        JOIN FETCH pat.user
        LEFT JOIN FETCH p.doctor d
        LEFT JOIN FETCH d.user
        WHERE p.patient.patientId = :patientId
        ORDER BY p.createdAt DESC
    """)
    List<Prescription> findByPatient_PatientIdWithDetails(@Param("patientId") Long patientId);


    @Query("""
    SELECT new com.prescription.dto.PrescriptionSummaryDTO(
        p.prescriptionId,
        p.prescriptionDate,
        p.processingStatus,
        CAST(p.prescriptionType AS string),
        p.diagnosis,
        p.createdAt,
        p.updatedAt,
        COUNT(DISTINCT m),
        COUNT(DISTINCT di)
    )
    FROM Prescription p
    LEFT JOIN p.medicines m
    LEFT JOIN p.drugInteractions di
    WHERE p.patient.patientId = :patientId
    GROUP BY p.prescriptionId, p.prescriptionDate, p.processingStatus,
             p.prescriptionType, p.diagnosis, p.createdAt, p.updatedAt
    ORDER BY p.createdAt DESC
""")
    List<PrescriptionSummaryDTO> findSummariesByPatientId(@Param("patientId") Long patientId);


    // For detail view - fetch patient and doctor only
    // Collections will be loaded separately
    @Query("""
        SELECT p FROM Prescription p
        JOIN FETCH p.patient pat
        JOIN FETCH pat.user
        LEFT JOIN FETCH p.doctor d
        LEFT JOIN FETCH d.user
        WHERE p.prescriptionId = :prescriptionId
    """)
    Optional<Prescription> findByIdWithDetails(@Param("prescriptionId") Long prescriptionId);

    List<Prescription> findByDoctor_DoctorId(Long doctorId);

    @Query("""
        SELECT p FROM Prescription p
        JOIN FETCH p.patient pat
        WHERE p.patient.patientId = :patientId 
        AND p.prescriptionDate BETWEEN :startDate AND :endDate
        ORDER BY p.prescriptionDate DESC
    """)
    List<Prescription> findByPatientAndDateRange(
            @Param("patientId") Long patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.processingStatus = :status")
    Long countByProcessingStatus(@Param("status") String status);

    // For analytics
    @Query("""
        SELECT DISTINCT p FROM Prescription p
        JOIN FETCH p.patient
        WHERE p.prescriptionDate BETWEEN :startDate AND :endDate
    """)
    List<Prescription> findByDateRangeForAnalytics(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}