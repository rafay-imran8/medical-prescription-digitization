package com.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientDTO {

    private Long patientId;
    private String patientUniqueId;
    private String patientName;
    private Integer age;
    private String gender;
    private String contactNumber;
    private String email;
    private String address;
    private LocalDateTime createdAt;

    // Access-related info (when fetched by doctor)
    private Boolean hasAccess;
    private LocalDateTime accessGrantedAt;
    private LocalDateTime accessExpiresAt;
}