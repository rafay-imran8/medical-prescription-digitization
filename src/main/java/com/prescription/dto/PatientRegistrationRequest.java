package com.prescription.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientRegistrationRequest {
    private String fullName;
    private String email;
    private String cnic;
    private Integer age;
    private String gender;
    private String contactNumber;
    private String address;
}