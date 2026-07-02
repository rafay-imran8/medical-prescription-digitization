package com.prescription.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorDTO {

    private Long doctorId;
    private String doctorUniqueId;
    private String fullName;
    private String email;
    private String specialization;
    private String licenseNumber;
    private String phone;
    private String clinicAddress;
    private LocalDateTime createdAt;

    // Access request info (when fetched by patient)
    private Boolean accessRequested;
    private Boolean accessGranted;
    private LocalDateTime accessRequestedAt;
}