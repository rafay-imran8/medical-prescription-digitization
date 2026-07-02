package com.prescription.dto;

import com.prescription.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private User.UserRole role;
    private String roleSpecificId; // PAT-00001 or DOC-00001
    private Long roleSpecificPrimaryId; // patient_id or doctor_id
    private String message;

    public AuthResponse(String token, Long userId, String email, String fullName, User.UserRole role) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.message = "Login successful";
    }
}