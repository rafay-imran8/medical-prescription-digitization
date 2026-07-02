package com.prescription.controller;

import com.prescription.dto.*;

import java.nio.file.*;
import org.springframework.http.MediaType;
import com.prescription.entity.Doctor;
import com.prescription.repository.PrescriptionRepository;
import com.prescription.security.JwtPrincipal;
import com.prescription.service.AccessControlService;
import com.prescription.service.DoctorService;
import com.prescription.service.PatientService;
import com.prescription.service.PrescriptionService;
import com.prescription.entity.Prescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private AccessControlService accessControlService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Doctor>> getProfile(@AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Doctor doctor = doctorService.getDoctorByUserId(userDetails.getUserId());
            return ResponseEntity.ok(ApiResponse.success(doctor));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my-patients")
    public ResponseEntity<ApiResponse<List<PatientDTO>>> getMyPatients(
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Doctor doctor = doctorService.getDoctorByUserId(userDetails.getUserId());
            List<PatientDTO> patients = patientService.getPatientsGrantedAccessToDoctor(doctor.getDoctorId());
            return ResponseEntity.ok(ApiResponse.success(patients));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/prescriptions/create")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> createPrescription(
            @RequestBody CreatePrescriptionRequest request,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Doctor doctor = doctorService.getDoctorByUserId(userDetails.getUserId());
            PrescriptionResponse response = prescriptionService.createDigitalPrescription(
                    doctor.getDoctorId(),
                    request
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/patients/search")
    public ResponseEntity<ApiResponse<List<PatientDTO>>> searchPatients(
            @RequestParam String patientId) {
        List<PatientDTO> results = accessControlService.searchPatientsByUniqueId(patientId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @PostMapping("/access/activate")
    public ResponseEntity<ApiResponse<String>> activateAccess(
            @RequestBody AccessCodeRequest request) {
        try {
            accessControlService.activateAccessWithCode(request.getAccessCode());
            return ResponseEntity.ok(ApiResponse.success("Access activated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/access/request/{patientId}")
    public ResponseEntity<ApiResponse<String>> requestAccess(
            @PathVariable Long patientId,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Doctor doctor = doctorService.getDoctorByUserId(userDetails.getUserId());
            accessControlService.requestAccess(doctor.getDoctorId(), patientId);
            return ResponseEntity.ok(ApiResponse.success("Access request sent to patient"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/patients/{patientId}/prescriptions")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getPatientPrescriptions(
            @PathVariable Long patientId,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            Doctor doctor = doctorService.getDoctorByUserId(userDetails.getUserId());
            List<PrescriptionResponse> prescriptions = prescriptionService.getDoctorPatientPrescriptions(
                    doctor.getDoctorId(),
                    patientId
            );
            return ResponseEntity.ok(ApiResponse.success(prescriptions));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/prescriptions/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescription(
            @PathVariable Long prescriptionId,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            PrescriptionResponse prescription = prescriptionService.getPrescriptionById(
                    prescriptionId,
                    userDetails.getUserId(),
                    "DOCTOR"
            );
            return ResponseEntity.ok(ApiResponse.success(prescription));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @GetMapping("/prescriptions/{id}/image")
    public ResponseEntity<byte[]> getPrescriptionImage(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal userDetails) {

        try {
            // ✅ Get logged-in doctor
            Doctor doctor = doctorService.getDoctorByUserId(userDetails.getUserId());

            // ✅ Fetch prescription directly from repository
            Prescription prescription = prescriptionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Prescription not found"));

            // ✅ SECURITY CHECK (use EXISTING logic in your project)
            boolean hasAccess = patientService.hasAccessToPatient(
                    doctor.getDoctorId(),
                    prescription.getPatient().getPatientId()
            );

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // ✅ Validate image path
            if (prescription.getImagePath() == null || prescription.getImagePath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path imagePath = Paths.get(prescription.getImagePath());

            if (!Files.exists(imagePath)) {
                System.err.println("Image not found: " + imagePath);
                return ResponseEntity.notFound().build();
            }

            // ✅ Read image
            byte[] imageBytes = Files.readAllBytes(imagePath);

            // ✅ Detect content type
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            // ✅ Return image
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageBytes);

        } catch (Exception e) {
            System.err.println("Error fetching prescription image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}