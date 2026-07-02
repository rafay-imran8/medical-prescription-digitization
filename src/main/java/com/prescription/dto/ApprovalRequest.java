package com.prescription.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalRequest {
    private Long userId;
    private Boolean approved;
    private String notes;
    private String rejectionReason;
}