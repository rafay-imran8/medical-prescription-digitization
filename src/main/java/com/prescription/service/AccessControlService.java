package com.prescription.service;

import com.prescription.dto.DoctorDTO;
import com.prescription.dto.PatientDTO;
import com.prescription.entity.*;
import com.prescription.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccessControlService {

    @Autowired
    private PatientDoctorAccessRepository accessRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private EmailService emailService;

    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate 6-digit access code
     */
    private String generateAccessCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Doctor searches for patient by ID
     */
    @Transactional(readOnly = true)
    public List<PatientDTO> searchPatientsByUniqueId(String patientUniqueId) {
        List<Patient> patients = patientRepository.findByPatientUniqueIdContainingIgnoreCase(patientUniqueId);

        return patients.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Doctor requests access to patient
     */

    @Transactional
    public void requestAccess(Long doctorId, Long patientId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Optional<PatientDoctorAccess> existing =
                accessRepository.findByPatient_PatientIdAndDoctor_DoctorId(patientId, doctorId);

        PatientDoctorAccess access;

        if (existing.isPresent()) {
            access = existing.get();

            // If already active or already granted, do not overwrite it
            if (Boolean.TRUE.equals(access.getIsActive()) || Boolean.TRUE.equals(access.getAccessGranted())) {
                throw new RuntimeException("Access request already exists");
            }

            // Reuse old inactive record
            access.setAccessGranted(false);
            access.setIsActive(false);
            access.setAccessCode(null);
            access.setCodeExpiresAt(null);
            access.setAccessGrantedAt(null);
            access.setAccessExpiresAt(null);
        } else {
            access = new PatientDoctorAccess();
            access.setDoctor(doctor);
            access.setPatient(patient);
            access.setAccessGranted(false);
            access.setIsActive(false);
        }

        accessRepository.save(access);

        emailService.sendAccessRequestEmail(
                patient.getEmail(),
                patient.getPatientName(),
                doctor.getUser().getFullName(),
                doctor.getSpecialization()
        );
    }
    /**
     * Patient grants access - generates code and sends email to patient
     */

    @Transactional
    public String grantAccessWithCode(Long patientId, Long doctorId) {

        PatientDoctorAccess access = accessRepository
                .findByPatient_PatientIdAndDoctor_DoctorId(patientId, doctorId)
                .orElseThrow(() -> new RuntimeException("Access request not found"));

        // Prevent re-grant if already active
        if (Boolean.TRUE.equals(access.getIsActive())) {
            throw new RuntimeException("Access already active");
        }

        String code = generateAccessCode();

        access.setAccessCode(code);
        access.setCodeExpiresAt(LocalDateTime.now().plusHours(1)); // 1 hour
        access.setAccessGranted(true);
        access.setAccessGrantedAt(LocalDateTime.now());
        access.setAccessExpiresAt(LocalDateTime.now().plusHours(24)); // 24 hours
        access.setIsActive(false); // IMPORTANT

        accessRepository.save(access);

        emailService.sendAccessCodeEmail(
                access.getPatient().getEmail(),
                access.getPatient().getPatientName(),
                access.getDoctor().getUser().getFullName(),
                code
        );

        return code;
    }
    /**
     * Patient enters code to activate access
     */
    @Transactional
    public void activateAccessWithCode(String accessCode) {

        PatientDoctorAccess access = accessRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new RuntimeException("Invalid access code"));

        // 1. Check if code exists
        if (access.getCodeExpiresAt() == null) {
            throw new RuntimeException("Code already used or invalid");
        }

        // 2. Check expiry (1 hour)
        if (access.getCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Access code has expired");
        }

        // 3. Ensure patient actually approved
        if (!Boolean.TRUE.equals(access.getAccessGranted())) {
            throw new RuntimeException("Access not approved by patient");
        }

        // 4. Prevent reuse
        if (Boolean.TRUE.equals(access.getIsActive())) {
            throw new RuntimeException("Access already activated");
        }

        // 5. Activate access (24h validity already set earlier)
        access.setIsActive(true);

        // 6. Invalidate code immediately (one-time use)
        access.setAccessCode(null);
        access.setCodeExpiresAt(null);

        accessRepository.save(access);

        // 7. Notify doctor
        Doctor doctor = access.getDoctor();
        Patient patient = access.getPatient();

        emailService.sendAccessActivatedEmail(
                doctor.getUser().getEmail(),
                doctor.getUser().getFullName(),
                patient.getPatientName()
        );
    }

    /**
     * Get pending access requests for a patient
     */
    /**
     * Get pending access requests for a patient
     */
    @Transactional(readOnly = true)
    public List<DoctorDTO> getPendingAccessRequests(Long patientId) {
        List<PatientDoctorAccess> requests = accessRepository.findPendingRequestsByPatientId(patientId);

        return requests.stream()
                .map(access -> {
                    Doctor doctor = access.getDoctor();
                    DoctorDTO dto = new DoctorDTO();
                    dto.setDoctorId(doctor.getDoctorId());
                    dto.setDoctorUniqueId(doctor.getDoctorUniqueId());
                    dto.setFullName(doctor.getUser().getFullName());  // ← Changed from setDoctorName
                    dto.setSpecialization(doctor.getSpecialization());
                    dto.setLicenseNumber(doctor.getLicenseNumber());
                    dto.setPhone(doctor.getContactNumber());  // ← Changed from setContactNumber
                    dto.setEmail(doctor.getUser().getEmail());
                    dto.setAccessRequested(true);
                    dto.setAccessGranted(false);
//
                    dto.setAccessRequestedAt(access.getAccessRequestedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get doctors who have been granted access
     */
//    @Transactional(readOnly = true)
//    public List<DoctorDTO> getGrantedDoctors(Long patientId) {
//        List<PatientDoctorAccess> granted = accessRepository
//                .findByPatient_PatientIdAndAccessGrantedTrue(patientId);
//
//        return granted.stream()
//                .map(access -> {
//                    Doctor doctor = access.getDoctor();
//                    DoctorDTO dto = new DoctorDTO();
//                    dto.setDoctorId(doctor.getDoctorId());
//                    dto.setDoctorUniqueId(doctor.getDoctorUniqueId());
//                    dto.setFullName(doctor.getUser().getFullName());  // ← Changed
//                    dto.setSpecialization(doctor.getSpecialization());
//                    dto.setLicenseNumber(doctor.getLicenseNumber());
//                    dto.setPhone(doctor.getContactNumber());  // ← Changed
//                    dto.setEmail(doctor.getUser().getEmail());
//                    dto.setAccessRequested(true);
//                    dto.setAccessGranted(true);
//                    dto.setAccessRequestedAt(access.getAccessRequestedAt());
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }

    @Transactional(readOnly = true)
    public List<DoctorDTO> getGrantedDoctors(Long patientId) {
        List<PatientDoctorAccess> granted =
                accessRepository.findByPatient_PatientIdAndAccessGrantedTrue(patientId);

        return granted.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .filter(a -> a.getAccessExpiresAt() != null && a.getAccessExpiresAt().isAfter(LocalDateTime.now()))
                .map(access -> {
                    Doctor doctor = access.getDoctor();
                    DoctorDTO dto = new DoctorDTO();
                    dto.setDoctorId(doctor.getDoctorId());
                    dto.setDoctorUniqueId(doctor.getDoctorUniqueId());
                    dto.setFullName(doctor.getUser().getFullName());
                    dto.setSpecialization(doctor.getSpecialization());
                    dto.setLicenseNumber(doctor.getLicenseNumber());
                    dto.setPhone(doctor.getContactNumber());
                    dto.setEmail(doctor.getUser().getEmail());
                    dto.setAccessRequested(true);
                    dto.setAccessGranted(true);
                    dto.setAccessRequestedAt(access.getAccessRequestedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }


    /**
     * Revoke access
     */

    @Transactional
    public void revokeAccess(Long patientId, Long doctorId) {
        PatientDoctorAccess access = accessRepository
                .findByPatient_PatientIdAndDoctor_DoctorId(patientId, doctorId)
                .orElseThrow(() -> new RuntimeException("Access not found"));

        access.setIsActive(false);
        access.setAccessGranted(false);
        access.setAccessCode(null);
        access.setCodeExpiresAt(null);

        accessRepository.save(access);

        emailService.sendAccessRevokedEmail(
                access.getDoctor().getUser().getEmail(),
                access.getDoctor().getUser().getFullName(),
                access.getPatient().getPatientName()
        );
    }

    /**
     * Check and revoke expired access (run this as scheduled task)
     */
    @Transactional
    public void revokeExpiredAccess() {
        List<PatientDoctorAccess> expiredAccess = accessRepository
                .findByAccessExpiresAtBeforeAndIsActiveTrue(LocalDateTime.now());

        for (PatientDoctorAccess access : expiredAccess) {
            access.setIsActive(false);
            access.setAccessGranted(false);
            access.setAccessCode(null);
            access.setCodeExpiresAt(null);

            accessRepository.save(access);

            emailService.sendAccessExpiredEmail(
                    access.getDoctor().getUser().getEmail(),
                    access.getDoctor().getUser().getFullName(),
                    access.getPatient().getPatientName()
            );
        }
    }

    private PatientDTO convertToDTO(Patient patient) {
        PatientDTO dto = new PatientDTO();
        dto.setPatientId(patient.getPatientId());
        dto.setPatientUniqueId(patient.getPatientUniqueId());
        dto.setPatientName(patient.getPatientName());
        dto.setAge(patient.getAge());
        dto.setGender(patient.getGender());
        dto.setContactNumber(patient.getContactNumber());
        dto.setEmail(patient.getEmail());
        return dto;
    }
}