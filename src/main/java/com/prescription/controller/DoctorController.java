package com.prescription.controller;

import com.prescription.dto.ApiResponse;
import com.prescription.dto.PatientDTO;
import com.prescription.dto.PrescriptionResponse;
import com.prescription.entity.Doctor;
import com.prescription.security.CustomUserDetails;
import com.prescription.service.AccessControlService;
import com.prescription.service.DoctorService;
import com.prescription.service.PatientService;
import com.prescription.service.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<Doctor>> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {
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

    @PostMapping("/request-access/{patientId}")
    public ResponseEntity<ApiResponse<String>> requestAccess(
            @PathVariable Long patientId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {
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
}