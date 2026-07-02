package com.prescription.repository;

import com.prescription.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("SELECT p FROM Patient p JOIN FETCH p.user WHERE p.user.userId = :userId")
    Optional<Patient> findByUser_UserId(@Param("userId") Long userId);

    Optional<Patient> findByPatientUniqueId(String patientUniqueId);

    @Query("SELECT CONCAT('PAT-', LPAD(CAST(COALESCE(MAX(CAST(SUBSTRING(p.patientUniqueId, 5) AS integer)), 0) + 1 AS text), 5, '0')) FROM Patient p")
    String generateNextPatientId();

    // Eager fetch with all relationships
    @Query("""
        SELECT DISTINCT p FROM Patient p
        JOIN FETCH p.user
        LEFT JOIN FETCH p.prescriptions
        ORDER BY p.createdAt DESC
    """)
    List<Patient> findAllWithDetails();

    // Single patient with all details
    @Query("""
        SELECT p FROM Patient p
        JOIN FETCH p.user
        LEFT JOIN FETCH p.doctorAccesses
        WHERE p.patientId = :patientId
    """)
    Optional<Patient> findByIdWithDetails(@Param("patientId") Long patientId);

    @Query("SELECT p FROM Patient p WHERE LOWER(p.patientUniqueId) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Patient> findByPatientUniqueIdContainingIgnoreCase(@Param("search") String search);
}