package com.prescription.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorRegistrationRequest {
    private String fullName;
    private String email;
    private String cnic;
    private String specialization;
    private String licenseNumber;
    private String contactNumber;
    // License document will be sent as MultipartFile in controller
}