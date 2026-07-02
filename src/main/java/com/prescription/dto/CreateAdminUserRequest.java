package com.prescription.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAdminUserRequest {
    private String fullName;
    private String email;
    private String role; // ADMIN or ANALYST
    private String temporaryPassword;
}