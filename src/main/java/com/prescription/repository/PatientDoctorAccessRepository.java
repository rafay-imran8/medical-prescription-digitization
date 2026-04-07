package com.prescription.repository;

import com.prescription.entity.PatientDoctorAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientDoctorAccessRepository extends JpaRepository<PatientDoctorAccess, Long> {

    Optional<PatientDoctorAccess> findByPatient_PatientIdAndDoctor_DoctorId(
            Long patientId,
            Long doctorId
    );

    Optional<PatientDoctorAccess> findByAccessCode(String accessCode);

    List<PatientDoctorAccess> findByAccessExpiresAtBeforeAndIsActiveTrue(
            LocalDateTime now
    );

    @Query("""
        SELECT pda FROM PatientDoctorAccess pda
        JOIN FETCH pda.patient p
        JOIN FETCH p.user
        WHERE pda.doctor.doctorId = :doctorId
        AND pda.accessGranted = true
    """)
    List<PatientDoctorAccess> findByDoctor_DoctorIdAndAccessGrantedTrueWithDetails(
            @Param("doctorId") Long doctorId
    );

    @Query("""
        SELECT pda FROM PatientDoctorAccess pda
        JOIN FETCH pda.doctor d
        JOIN FETCH d.user
        WHERE pda.patient.patientId = :patientId
        AND pda.accessGranted = false
    """)
    List<PatientDoctorAccess> findPendingRequestsByPatientId(
            @Param("patientId") Long patientId
    );

    @Query("""
        SELECT pda FROM PatientDoctorAccess pda
        WHERE pda.patient.patientId = :patientId
        AND pda.doctor.doctorId = :doctorId
        AND pda.accessGranted = true
        AND pda.isActive = true
        AND pda.accessExpiresAt > CURRENT_TIMESTAMP
    """)
    Optional<PatientDoctorAccess> findActiveAccess(
            @Param("patientId") Long patientId,
            @Param("doctorId") Long doctorId
    );

    @Query("""
    SELECT pda FROM PatientDoctorAccess pda
    WHERE pda.patient.patientId = :patientId
    AND pda.accessGranted = true
""")
    List<PatientDoctorAccess> findByPatient_PatientIdAndAccessGrantedTrue(
            @Param("patientId") Long patientId
    );
}