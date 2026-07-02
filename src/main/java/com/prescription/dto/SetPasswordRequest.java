package com.prescription.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetPasswordRequest {
    private String token;
    private String password;
    private String confirmPassword;
}