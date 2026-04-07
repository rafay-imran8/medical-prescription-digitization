package com.prescription.service;

import com.prescription.dto.*;
import com.prescription.entity.*;
import com.prescription.repository.*;
import com.prescription.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${document.storage.path:/var/prescription-system/documents}")
    private String documentStoragePath;

    /**
     * Register new patient
     */
    @Transactional
    public void registerPatient(PatientRegistrationRequest request) {
        // Validate CNIC uniqueness
        if (userRepository.existsByCnic(request.getCnic())) {
            throw new RuntimeException("CNIC already registered");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create user account
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setCnic(request.getCnic());
        user.setRole(User.UserRole.PATIENT);
        user.setAccountStatus(User.AccountStatus.PENDING);
        user.setIsVerified(false);
        user.setIsActive(false);
        user.setPasswordSet(false);

        user = userRepository.save(user);

        // Create patient profile
        Patient patient = new Patient();
        patient.setUser(user);
        patient.setPatientName(request.getFullName());
        patient.setPatientUniqueId(generatePatientId());
        patient.setCnic(request.getCnic());
        patient.setAge(request.getAge());
        patient.setGender(request.getGender());
        patient.setContactNumber(request.getContactNumber());
        patient.setEmail(request.getEmail());
        patient.setAddress(request.getAddress());

        patientRepository.save(patient);

        // Send email to user
        emailService.sendRegistrationPendingEmail(
                request.getEmail(),
                request.getFullName()
        );

        // Send email to admin
        emailService.sendNewRegistrationNotificationToAdmin(
                user.getUserId(),
                request.getFullName(),
                request.getEmail(),
                request.getCnic(),
                "PATIENT"
        );
    }

    /**
     * Register new doctor
     */
    @Transactional
    public void registerDoctor(DoctorRegistrationRequest request, MultipartFile licenseDocument) {
        // Validate CNIC uniqueness
        if (userRepository.existsByCnic(request.getCnic())) {
            throw new RuntimeException("CNIC already registered");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Save license document
        String documentPath = saveLicenseDocument(licenseDocument, request.getCnic());

        // Create user account
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setCnic(request.getCnic());
        user.setRole(User.UserRole.DOCTOR);
        user.setAccountStatus(User.AccountStatus.PENDING);
        user.setIsVerified(false);
        user.setIsActive(false);
        user.setPasswordSet(false);

        user = userRepository.save(user);

        // Create doctor profile
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setDoctorUniqueId(generateDoctorId());
        doctor.setCnic(request.getCnic());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setLicenseDocumentPath(documentPath);
        doctor.setContactNumber(request.getContactNumber());
        doctor.setLicenseVerified(false);
        doctor.setIsVerified(false);

        doctorRepository.save(doctor);

        // Send email to doctor
        emailService.sendDoctorRegistrationPendingEmail(
                request.getEmail(),
                request.getFullName()
        );

        // Send email to admin
        emailService.sendNewDoctorRegistrationToAdmin(
                user.getUserId(),
                request.getFullName(),
                request.getEmail(),
                request.getCnic(),
                request.getLicenseNumber(),
                documentPath
        );
    }

    /**
     * Approve user registration
     */
    @Transactional
    public void approveRegistration(Long userId, Long approverId, String notes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User admin = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (user.getAccountStatus() != User.AccountStatus.PENDING) {
            throw new RuntimeException("User is not in pending status");
        }

        // Generate password setup token
        String token = UUID.randomUUID().toString();
        user.setRegistrationToken(token);
        user.setTokenExpiresAt(LocalDateTime.now().plusHours(24));
        user.setAccountStatus(User.AccountStatus.APPROVED);
        user.setApprovedBy(admin);
        user.setApprovedAt(LocalDateTime.now());
        user.setAdminNotes(notes);

        userRepository.save(user);

        // Send password setup email
        emailService.sendPasswordSetupEmail(
                user.getEmail(),
                user.getFullName(),
                token
        );
    }

    /**
     * Reject user registration
     */
    @Transactional
    public void rejectRegistration(Long userId, Long adminId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        user.setAccountStatus(User.AccountStatus.REJECTED);
        user.setRejectionReason(reason);
        user.setApprovedBy(admin);
        user.setApprovedAt(LocalDateTime.now());

        userRepository.save(user);

        // Send rejection email
        emailService.sendRegistrationRejectedEmail(
                user.getEmail(),
                user.getFullName(),
                reason
        );
    }
    /**
     * Login with account status check
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check account status
        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            throw new RuntimeException("Your account is pending approval. Please wait for admin verification.");
        }

        if (user.getAccountStatus() == User.AccountStatus.REJECTED) {
            throw new RuntimeException("Your registration was rejected. Please contact support for details.");
        }

        if (user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            throw new RuntimeException("Your account has been suspended. Please contact support.");
        }

        if (!user.getPasswordSet()) {
            throw new RuntimeException("Please set your password using the link sent to your email.");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("Your account is not active. Please contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());

        // Build response
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());

        return response;
    }
    /**
     * Set password using token
     */
    @Transactional
    public void setPassword(SetPasswordRequest request) {
        User user = userRepository.findByRegistrationToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        // Validate token expiry
        if (user.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token has expired");
        }

        // Validate password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Set password
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPasswordSet(true);
        user.setIsActive(true);
        user.setIsVerified(true);
        user.setRegistrationToken(null);
        user.setTokenExpiresAt(null);

        userRepository.save(user);

        // Send account activated email
        emailService.sendAccountActivatedEmail(
                user.getEmail(),
                user.getFullName()
        );
    }

    /**
     * Helper: Save license document
     */
    private String saveLicenseDocument(MultipartFile file, String cnic) {
        try {
            String filename = cnic + "_license_" + UUID.randomUUID().toString() +
                    "_" + file.getOriginalFilename();
            Path licensePath = Paths.get(documentStoragePath, "licenses", filename);
            Files.createDirectories(licensePath.getParent());
            Files.write(licensePath, file.getBytes());
            return licensePath.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to save license document: " + e.getMessage());
        }
    }

    /**
     * Helper: Generate patient ID
     */
    private String generatePatientId() {
        String lastId = patientRepository.generateNextPatientId();
        return lastId != null ? lastId : "PAT-00001";
    }

    /**
     * Helper: Generate doctor ID
     */
    private String generateDoctorId() {
        String lastId = doctorRepository.generateNextDoctorId();
        return lastId != null ? lastId : "DOC-00001";
    }
}