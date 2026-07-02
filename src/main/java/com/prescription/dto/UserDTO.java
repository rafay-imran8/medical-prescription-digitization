package com.prescription.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private Boolean isActive;
    private Boolean isVerified;
    private LocalDateTime createdAt;

    // Patient-specific fields
    private String patientUniqueId;
    private String patientName;
    private Integer age;
    private String gender;

    // Doctor-specific fields
    private String doctorUniqueId;
    private String specialization;
    private String licenseNumber;
}