package com.prescription.controller;

import com.prescription.dto.ApiResponse;
import com.prescription.dto.ApprovalRequest;
import com.prescription.dto.PendingUserDTO;
import com.prescription.security.JwtPrincipal;
import com.prescription.service.AuthService;
import com.prescription.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/registrations")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminRegistrationController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    /**
     * Get all pending registrations
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PendingUserDTO>>> getPendingRegistrations() {
        try {
            List<PendingUserDTO> pending = userService.getPendingRegistrations();
            return ResponseEntity.ok(ApiResponse.success(pending));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Approve registration
     */
    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<String>> approveRegistration(
            @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            authService.approveRegistration(
                    request.getUserId(),
                    userDetails.getUserId(),
                    request.getNotes()
            );
            return ResponseEntity.ok(ApiResponse.success(
                    "Registration approved. Password setup email sent to user."
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reject registration
     */
    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<String>> rejectRegistration(
            @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            authService.rejectRegistration(
                    request.getUserId(),
                    userDetails.getUserId(),
                    request.getRejectionReason()
            );
            return ResponseEntity.ok(ApiResponse.success(
                    "Registration rejected. Notification sent to user."
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get user details for review (including documents)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PendingUserDTO>> getUserDetails(
            @PathVariable Long userId) {
        try {
            PendingUserDTO user = userService.getUserDetailsForReview(userId);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Download license document
     */
    @GetMapping("/{userId}/document")
    public ResponseEntity<?> downloadDocument(@PathVariable Long userId) {
        try {
            // Implementation for document download
            // Return file as response
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Document not found"));
        }
    }
}