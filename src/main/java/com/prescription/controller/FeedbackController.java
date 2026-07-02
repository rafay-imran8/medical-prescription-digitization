package com.prescription.controller;

import com.prescription.dto.ApiResponse;
import com.prescription.dto.FeedbackResponse;
import com.prescription.dto.FeedbackRequest;
import com.prescription.security.JwtPrincipal;
import com.prescription.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
            @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            FeedbackResponse feedback = feedbackService.submitFeedback(
                    userDetails.getUserId(),
                    request
            );
            return ResponseEntity.ok(ApiResponse.success("Feedback submitted", feedback));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMyFeedbacks(
            @AuthenticationPrincipal JwtPrincipal userDetails) {
        try {
            List<FeedbackResponse> feedbacks = feedbackService.getUserFeedbacks(
                    userDetails.getUserId()
            );
            return ResponseEntity.ok(ApiResponse.success(feedbacks));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getAllFeedbacks() {
        try {
            List<FeedbackResponse> feedbacks = feedbackService.getAllFeedbacks();
            return ResponseEntity.ok(ApiResponse.success(feedbacks));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{feedbackId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateFeedbackStatus(
            @PathVariable Long feedbackId,
            @RequestParam String status) {
        try {
            FeedbackResponse feedback = feedbackService.updateFeedbackStatus(
                    feedbackId,
                    status
            );
            return ResponseEntity.ok(ApiResponse.success("Status updated", feedback));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}