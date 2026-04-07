package com.prescription.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FeedbackResponse {
    private Long feedbackId;
    private Long userId;
    private String userName;
    private String userRole;
    private String feedbackType;
    private Integer rating;
    private String subject;
    private String message;
    private String status;
    private LocalDateTime createdAt;
}