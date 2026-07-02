package com.prescription.controller;

import com.prescription.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {

    @Autowired
    private EmailService emailService;

    // Simple test endpoint
    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail() {
        try {
            emailService.sendTestEmail("your-email@example.com"); // replace with your email
            return ResponseEntity.ok("Email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed: " + e.getMessage());
        }
    }
}
