package com.prescription.service;

import com.prescription.dto.PendingUserDTO;
import com.prescription.dto.UserDTO;
import com.prescription.dto.CreateAdminUserRequest;
import com.prescription.entity.Doctor;
import com.prescription.entity.Patient;
import com.prescription.entity.User;
import com.prescription.repository.DoctorRepository;
import com.prescription.repository.PatientDoctorAccessRepository;
import com.prescription.repository.PatientRepository;
import com.prescription.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.prescription.dto.UserLogResponse;
@Service
public class UserService {

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
    private PatientDoctorAccessRepository patientDoctorAccessRepository;

    /**
     * Get all pending registrations with details
     */
    @Transactional(readOnly = true)
    public List<PendingUserDTO> getPendingRegistrations() {
        List<User> pendingUsers = userRepository.findPendingRegistrationsWithDetails();
        return pendingUsers.stream()
                .map(this::convertToPendingDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get user details for admin review
     */
    @Transactional(readOnly = true)
    public PendingUserDTO getUserDetailsForReview(Long userId) {
        User user = userRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToPendingDTO(user);
    }

    /**
     * Validate password setup token
     */
    @Transactional(readOnly = true)
    public User validatePasswordToken(String token) {
        User user = userRepository.findByRegistrationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (user.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token has expired");
        }

        if (!user.getAccountStatus().equals(User.AccountStatus.APPROVED)) {
            throw new RuntimeException("Account not approved");
        }

        return user;
    }

    /**
     * Convert User to PendingUserDTO
     */
    private PendingUserDTO convertToPendingDTO(User user) {
        PendingUserDTO dto = new PendingUserDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setCnic(user.getCnic());
        dto.setRole(user.getRole().name());
        dto.setAccountStatus(user.getAccountStatus().name());
        dto.setCreatedAt(user.getCreatedAt());

        if (user.getPatient() != null) {
            Patient patient = user.getPatient();
            dto.setAge(patient.getAge());
            dto.setGender(patient.getGender());
            dto.setAddress(patient.getAddress());
            dto.setDocumentPath(patient.getDocumentPath());
        }

        if (user.getDoctor() != null) {
            Doctor doctor = user.getDoctor();
            dto.setSpecialization(doctor.getSpecialization());
            dto.setLicenseNumber(doctor.getLicenseNumber());
            dto.setLicenseDocumentPath(doctor.getLicenseDocumentPath());
        }

        return dto;
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Get all users as DTOs (existing method)
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsersDTO() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserLogResponse> getUserLogs() {
        return userRepository.findAllByOrderByLastLoginDesc()
                .stream()
                .map(this::toLogResponse)
                .collect(Collectors.toList());
    }

    // ADD this import at the top


    // REPLACE toLogResponse with:
    private UserLogResponse toLogResponse(User user) {
        UserLogResponse r = new UserLogResponse();
        r.setUserId(user.getUserId());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole().name());
        r.setIsActive(user.getIsActive());
        r.setIsVerified(user.getIsVerified());
        r.setAccountStatus(user.getAccountStatus().name());  // ← .name() — enum to String
        r.setCnic(user.getCnic());
        r.setCreatedAt(user.getCreatedAt());
        r.setLastLogin(user.getLastLogin());
        r.setUpdatedAt(user.getUpdatedAt());
        r.setAdminNotes(user.getAdminNotes());
        r.setRejectionReason(user.getRejectionReason());
        return r;
    }

    @Transactional
    public UserDTO createAdminOrAnalyst(CreateAdminUserRequest request, Long creatorId) {
        // Validate role
        if (!request.getRole().equals("ADMIN") && !request.getRole().equals("ANALYST")) {
            throw new RuntimeException("Can only create ADMIN or ANALYST users");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(User.UserRole.valueOf(request.getRole()));
        user.setPasswordHash(passwordEncoder.encode(request.getTemporaryPassword()));
        user.setAccountStatus(User.AccountStatus.APPROVED);
        user.setIsActive(true);
        user.setIsVerified(true);
        user.setPasswordSet(true);
        user.setCnic("SYSTEM-" + UUID.randomUUID().toString().substring(0, 8));

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        user.setApprovedBy(creator);
        user.setApprovedAt(LocalDateTime.now());

        user = userRepository.save(user);

        // Send email with credentials
        emailService.sendAdminCredentialsEmail(
                user.getEmail(),
                user.getFullName(),
                request.getTemporaryPassword()
        );

        return convertToDTO(user);
    }

    /**
     * Update user status (existing method)
     */
    @Transactional
    public UserDTO updateUserStatus(Long userId, Boolean isActive) {
        User user = getUserById(userId);
        user.setIsActive(isActive);
        user = userRepository.save(user);
        return convertToDTO(user);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setIsActive(user.getIsActive());
        dto.setIsVerified(user.getIsVerified());
        dto.setCreatedAt(user.getCreatedAt());

        if (user.getPatient() != null) {
            Patient patient = user.getPatient();
            dto.setPatientUniqueId(patient.getPatientUniqueId());
            dto.setPatientName(patient.getPatientName());
            dto.setAge(patient.getAge());
            dto.setGender(patient.getGender());
        }

        if (user.getDoctor() != null) {
            Doctor doctor = user.getDoctor();
            dto.setDoctorUniqueId(doctor.getDoctorUniqueId());
            dto.setSpecialization(doctor.getSpecialization());
            dto.setLicenseNumber(doctor.getLicenseNumber());
        }

        return dto;
    }
}