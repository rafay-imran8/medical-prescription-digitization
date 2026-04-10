package com.prescription.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserLogResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private Boolean isActive;
    private Boolean isVerified;
    private String accountStatus;   // ← String, not enum
    private String cnic;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private LocalDateTime updatedAt;
    private String adminNotes;
    private String rejectionReason;
}