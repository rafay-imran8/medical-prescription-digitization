package com.prescription.controller;

import com.prescription.dto.*;
import com.prescription.entity.Patient;
import com.prescription.entity.Prescription;
import com.prescription.repository.PrescriptionRepository;
import com.prescription.security.JwtPrincipal;
import com.prescription.service.AccessControlService;
import com.prescription.service.PatientService;
import com.prescription.service.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/patient")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Patient>> getProfile(
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Patient patient = patientService.getPatientByUserId(userDetails.getUserId());
            return ResponseEntity.ok(ApiResponse.success(patient));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/prescriptions/upload")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> uploadPrescription(
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            System.out.println("Upload request received from user: " + userDetails.getUserId());

            Patient patient = patientService.getPatientByUserId(userDetails.getUserId());
            System.out.println("Patient found: " + patient.getPatientId());

            PrescriptionResponse response = prescriptionService.uploadAndProcessPrescription(
                    patient.getPatientId(),
                    image
            );

            System.out.println("Prescription processed successfully: " + response.getPrescriptionId());

            return ResponseEntity.ok(ApiResponse.success("Prescription processed successfully", response));

        } catch (Exception e) {
            System.err.println("Upload failed: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to process prescription: " + e.getMessage()));
        }
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<List<PrescriptionSummaryDTO>>> getMyPrescriptions(
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Patient patient = patientService.getPatientByUserId(userDetails.getUserId());
            List<PrescriptionSummaryDTO> prescriptions =
                    prescriptionRepository.findSummariesByPatientId(patient.getPatientId());
            return ResponseEntity.ok(ApiResponse.success(prescriptions));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/prescriptions/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescription(
            @PathVariable Long prescriptionId,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            PrescriptionResponse prescription =
                    prescriptionService.getPrescriptionById(
                            prescriptionId,
                            userDetails.getUserId(),
                            "PATIENT"
                    );
            return ResponseEntity.ok(ApiResponse.success(prescription));

        } catch (RuntimeException e) {
            System.err.println("Error fetching prescription " + prescriptionId + ": " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage() != null && e.getMessage().contains("Access denied")) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/prescriptions/{id}/image")
    public ResponseEntity<byte[]> getPrescriptionImage(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            // Get patient
            Patient patient = patientService.getPatientByUserId(userDetails.getUserId());

            // Get prescription entity directly from repository
            Prescription prescription = prescriptionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Prescription not found"));

            System.out.println("Image path: " + prescription.getImagePath());

            // Check access - verify patient owns this prescription
            if (!prescription.getPatient().getPatientId().equals(patient.getPatientId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Check if image path exists
            if (prescription.getImagePath() == null || prescription.getImagePath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Read image file
            Path imagePath = Paths.get(prescription.getImagePath());
            System.out.println("File exists: " + Files.exists(imagePath));
            if (!Files.exists(imagePath)) {
                System.err.println("Image file not found: " + imagePath);
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);

            // Determine content type
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageBytes);

        } catch (Exception e) {
            System.err.println("Error fetching prescription image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/access-requests/pending")
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getPendingAccessRequests(
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Patient patient = patientService.getPatientByUserId(userDetails.getUserId());
            List<DoctorDTO> requests = accessControlService.getPendingAccessRequests(patient.getPatientId());
            return ResponseEntity.ok(ApiResponse.success(requests));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/access-requests/granted")
    public ResponseEntity<ApiResponse<List<DoctorDTO>>> getGrantedDoctors(
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Patient patient = patientService.getPatientByUserId(userDetails.getUserId());
            List<DoctorDTO> doctors = accessControlService.getGrantedDoctors(patient.getPatientId());
            return ResponseEntity.ok(ApiResponse.success(doctors));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }




    @PostMapping("/access/grant/{doctorId}")
    public ResponseEntity<ApiResponse<AccessCodeResponse>> grantAccessWithCode(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Patient patient = patientService.getPatientByUserId(userDetails.getUserId());
            String code = accessControlService.grantAccessWithCode(patient.getPatientId(), doctorId);
            AccessCodeResponse resp = new AccessCodeResponse(code, "Code generated",
                    LocalDateTime.now().plusHours(1));
            return ResponseEntity.ok(ApiResponse.success(resp));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/access/revoke/{doctorId}")
    public ResponseEntity<ApiResponse<String>> revokeAccess(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Patient patient = patientService.getPatientByUserId(userDetails.getUserId());
            accessControlService.revokeAccess(patient.getPatientId(), doctorId);
            return ResponseEntity.ok(ApiResponse.success("Access revoked successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}