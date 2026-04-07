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

        // Check if request already exists
        if (accessRepository.findByPatient_PatientIdAndDoctor_DoctorId(patientId, doctorId).isPresent()) {
            throw new RuntimeException("Access request already exists");
        }

        // Create new access request
        PatientDoctorAccess access = new PatientDoctorAccess();
        access.setDoctor(doctor);
        access.setPatient(patient);
        access.setAccessGranted(false);
        access.setIsActive(false);
        access.setRequestedAt(LocalDateTime.now());

        accessRepository.save(access);

        // Send email notification to patient
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

        // Generate access code
        String accessCode = generateAccessCode();

        access.setAccessCode(accessCode);
        access.setCodeExpiresAt(LocalDateTime.now().plusHours(1)); // Code valid for 1 hour
        access.setAccessGranted(true);
        access.setAccessGrantedAt(LocalDateTime.now());
        access.setAccessExpiresAt(LocalDateTime.now().plusHours(24)); // Access valid for 24 hours

        accessRepository.save(access);

        // Send email with access code to patient
        Patient patient = access.getPatient();
        Doctor doctor = access.getDoctor();

        emailService.sendAccessCodeEmail(
                patient.getEmail(),
                patient.getPatientName(),
                doctor.getUser().getFullName(),
                accessCode
        );

        return accessCode;
    }

    /**
     * Patient enters code to activate access
     */
    @Transactional
    public void activateAccessWithCode(String accessCode) {
        PatientDoctorAccess access = accessRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new RuntimeException("Invalid access code"));

        // Check if code expired
        if (access.getCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Access code has expired");
        }

        // Activate access
        access.setIsActive(true);
        accessRepository.save(access);

        // Send confirmation email to doctor
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
                    dto.setAccessRequestedAt(access.getRequestedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get doctors who have been granted access
     */
    @Transactional(readOnly = true)
    public List<DoctorDTO> getGrantedDoctors(Long patientId) {
        List<PatientDoctorAccess> granted = accessRepository
                .findByPatient_PatientIdAndAccessGrantedTrue(patientId);

        return granted.stream()
                .map(access -> {
                    Doctor doctor = access.getDoctor();
                    DoctorDTO dto = new DoctorDTO();
                    dto.setDoctorId(doctor.getDoctorId());
                    dto.setDoctorUniqueId(doctor.getDoctorUniqueId());
                    dto.setFullName(doctor.getUser().getFullName());  // ← Changed
                    dto.setSpecialization(doctor.getSpecialization());
                    dto.setLicenseNumber(doctor.getLicenseNumber());
                    dto.setPhone(doctor.getContactNumber());  // ← Changed
                    dto.setEmail(doctor.getUser().getEmail());
                    dto.setAccessRequested(true);
                    dto.setAccessGranted(true);
                    dto.setAccessRequestedAt(access.getRequestedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Grant access via token (legacy method - can be removed if not used)
     */
    @Transactional
    public void grantAccessViaToken(String token) {
        // If you're not using this method, you can remove the endpoint
        throw new RuntimeException("This method is deprecated. Use grantAccessWithCode instead.");
    }

    /**
     * Grant access manually (without code system)
     */
    @Transactional
    public void grantAccessManually(Long patientId, Long doctorId) {
        PatientDoctorAccess access = accessRepository
                .findByPatient_PatientIdAndDoctor_DoctorId(patientId, doctorId)
                .orElseThrow(() -> new RuntimeException("Access request not found"));

        access.setAccessGranted(true);
        access.setAccessGrantedAt(LocalDateTime.now());
        access.setIsActive(true);
        access.setAccessExpiresAt(LocalDateTime.now().plusHours(24));

        accessRepository.save(access);

        // Send email notification
        emailService.sendAccessGrantedEmail(
                access.getDoctor().getUser().getEmail(),
                access.getDoctor().getUser().getFullName(),
                access.getPatient().getPatientName()
        );
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

        accessRepository.save(access);

        // Notify doctor
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
            accessRepository.save(access);

            // Notify doctor that access has expired
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