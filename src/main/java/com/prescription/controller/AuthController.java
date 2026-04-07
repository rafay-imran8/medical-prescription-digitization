package com.prescription.controller;

import com.prescription.dto.*;
import com.prescription.entity.User;
import com.prescription.security.CustomUserDetails;
import com.prescription.service.AuthService;
import com.prescription.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    /**
     * Register new patient
     */
    @PostMapping("/register/patient")
    public ResponseEntity<ApiResponse<String>> registerPatient(
            @RequestBody PatientRegistrationRequest request) {
        try {
            authService.registerPatient(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Registration successful! You will receive an email once your account is approved."
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Register new doctor (with license document upload)
     */
    @PostMapping("/register/doctor")
    public ResponseEntity<ApiResponse<String>> registerDoctor(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("cnic") String cnic,
            @RequestParam("specialization") String specialization,
            @RequestParam("licenseNumber") String licenseNumber,
            @RequestParam("contactNumber") String contactNumber,
            @RequestParam("licenseDocument") MultipartFile licenseDocument) {
        try {
            DoctorRegistrationRequest request = new DoctorRegistrationRequest();
            request.setFullName(fullName);
            request.setEmail(email);
            request.setCnic(cnic);
            request.setSpecialization(specialization);
            request.setLicenseNumber(licenseNumber);
            request.setContactNumber(contactNumber);

            authService.registerDoctor(request, licenseDocument);

            return ResponseEntity.ok(ApiResponse.success(
                    "Registration successful! Your documents will be reviewed within 2-3 business days."
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Set password using token
     */
    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<String>> setPassword(
            @RequestBody SetPasswordRequest request) {
        try {
            authService.setPassword(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Password set successfully! You can now log in."
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Validate password setup token
     */
    @GetMapping("/validate-token")
    public ResponseEntity<ApiResponse<User>> validateToken(
            @RequestParam String token) {
        try {
            User user = userService.validatePasswordToken(token);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Login (existing endpoint - update to check account status)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}