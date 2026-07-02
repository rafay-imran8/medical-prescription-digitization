package com.prescription.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PrescriptionUploadRequest {

    private MultipartFile image;
    private String notes; // Optional patient notes
}