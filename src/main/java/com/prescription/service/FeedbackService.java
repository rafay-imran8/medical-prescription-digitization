package com.prescription.service;

import com.prescription.dto.FeedbackRequest;
import com.prescription.dto.FeedbackResponse;
import com.prescription.entity.Feedback;
import com.prescription.entity.User;
import com.prescription.repository.FeedbackRepository;
import com.prescription.repository.PatientDoctorAccessRepository;
import com.prescription.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientDoctorAccessRepository patientDoctorAccessRepository;

    @Transactional
    public FeedbackResponse submitFeedback(Long userId, FeedbackRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setRating(request.getRating());
        feedback.setSubject(request.getSubject());
        feedback.setMessage(request.getMessage());

        feedback = feedbackRepository.save(feedback);

        return buildFeedbackResponse(feedback);
    }
    /**
     * Convert Feedback entity to FeedbackResponse DTO
     */
    private FeedbackResponse convertToResponse(Feedback feedback) {
        FeedbackResponse response = new FeedbackResponse();
        response.setFeedbackId(feedback.getFeedbackId());
        response.setUserId(feedback.getUser().getUserId());
        response.setUserName(feedback.getUser().getFullName());
        response.setUserRole(feedback.getUser().getRole().name());
        response.setFeedbackType(feedback.getFeedbackType());
        response.setSubject(feedback.getSubject());
        response.setMessage(feedback.getMessage());
        response.setRating(feedback.getRating());
        response.setStatus(feedback.getStatus());
        response.setCreatedAt(feedback.getCreatedAt());
        return response;
    }

    @Transactional
    public FeedbackResponse updateFeedbackStatus(Long feedbackId, String status) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        feedback.setStatus(status);
        feedback = feedbackRepository.save(feedback);

        return convertToResponse(feedback);
    }

    /**
     * Check if doctor has active access to patient
     */
    @Transactional(readOnly = true)
    public boolean hasAccessToPatient(Long doctorId, Long patientId) {
        return patientDoctorAccessRepository
                .findActiveAccess(patientId, doctorId)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getUserFeedbacks(Long userId) {
        List<Feedback> feedbacks = feedbackRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        return feedbacks.stream()
                .map(this::buildFeedbackResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getAllFeedbacks() {
        List<Feedback> feedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        return feedbacks.stream()
                .map(this::buildFeedbackResponse)
                .collect(Collectors.toList());
    }

    private FeedbackResponse buildFeedbackResponse(Feedback feedback) {
        FeedbackResponse response = new FeedbackResponse();
        response.setFeedbackId(feedback.getFeedbackId());
        response.setUserName(feedback.getUser().getFullName());
        response.setFeedbackType(feedback.getFeedbackType());
        response.setRating(feedback.getRating());
        response.setSubject(feedback.getSubject());
        response.setMessage(feedback.getMessage());
        response.setStatus(feedback.getStatus());
        response.setCreatedAt(feedback.getCreatedAt());
        return response;
    }
}