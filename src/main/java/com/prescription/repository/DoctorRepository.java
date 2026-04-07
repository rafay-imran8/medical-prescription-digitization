package com.prescription.repository;

import com.prescription.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("SELECT d FROM Doctor d JOIN FETCH d.user WHERE d.user.userId = :userId")
    Optional<Doctor> findByUser_UserId(@Param("userId") Long userId);

    Optional<Doctor> findByDoctorUniqueId(String doctorUniqueId);

    @Query("SELECT CONCAT('DOC-', LPAD(CAST(COALESCE(MAX(CAST(SUBSTRING(d.doctorUniqueId, 5) AS integer)), 0) + 1 AS text), 5, '0')) FROM Doctor d")
    String generateNextDoctorId();

    // Eager fetch with all relationships
    @Query("""
        SELECT DISTINCT d FROM Doctor d
        JOIN FETCH d.user
        LEFT JOIN FETCH d.prescriptions
        ORDER BY d.createdAt DESC
    """)
    List<Doctor> findAllWithDetails();

    // Single doctor with details
    @Query("""
        SELECT d FROM Doctor d
        JOIN FETCH d.user
        LEFT JOIN FETCH d.patientAccesses
        WHERE d.doctorId = :doctorId
    """)
    Optional<Doctor> findByIdWithDetails(@Param("doctorId") Long doctorId);
}