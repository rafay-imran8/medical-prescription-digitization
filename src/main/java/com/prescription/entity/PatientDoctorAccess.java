package com.prescription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "patient_doctor_access", schema = "app_schema",
        uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "doctor_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientDoctorAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Long accessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "access_granted", nullable = false)
    private Boolean accessGranted = false;

    @CreationTimestamp
    @Column(name = "access_requested_at", nullable = false, updatable = false)
    private LocalDateTime accessRequestedAt;

    @Column(name = "access_granted_at")
    private LocalDateTime accessGrantedAt;

    @Column(name = "access_expires_at")
    private LocalDateTime accessExpiresAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "requested_at")  // ← ADD THIS
    private LocalDateTime requestedAt;

    @Column(name = "access_code", length = 10)  // ← ADD THIS
    private String accessCode;

    @Column(name = "code_expires_at")  // ← ADD THIS
    private LocalDateTime codeExpiresAt;

    // Relationships
    @OneToMany(mappedBy = "patientDoctorAccess", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccessToken> accessTokens;
}