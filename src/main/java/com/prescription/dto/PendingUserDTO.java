package com.prescription.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingUserDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String cnic;
    private String role;
    private String accountStatus;
    private LocalDateTime createdAt;

    // Patient specific
    private Integer age;
    private String gender;
    private String address;
    private String documentPath;

    // Doctor specific
    private String specialization;
    private String licenseNumber;
    private String licenseDocumentPath;
}