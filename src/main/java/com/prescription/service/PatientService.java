package com.prescription.service;

import com.prescription.dto.PatientDTO;
import com.prescription.dto.PrescriptionResponse;
import com.prescription.entity.Patient;
import com.prescription.entity.PatientDoctorAccess;
import com.prescription.entity.Prescription;
import com.prescription.repository.PatientDoctorAccessRepository;
import com.prescription.repository.PatientRepository;
import com.prescription.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private  PrescriptionRepository prescriptionRepository;

    @Autowired
    private PatientDoctorAccessRepository accessRepository;

    @Autowired
    private PatientDoctorAccessRepository patientDoctorAccessRepository;


    @Transactional(readOnly = true)
    public Patient getPatientByUserId(Long userId) {
        return patientRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Patient record not found for user ID: " + userId));
    }
    @Transactional(readOnly = true)
    public Patient getPatientById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }



    @Transactional(readOnly = true)
    public List<PatientDTO> getPatientsGrantedAccessToDoctor(Long doctorId) {
        List<PatientDoctorAccess> accesses = accessRepository.findByDoctor_DoctorIdAndAccessGrantedTrueWithDetails(doctorId);

        return accesses.stream().map(access -> {
            Patient patient = access.getPatient();
            PatientDTO dto = new PatientDTO();
            dto.setPatientId(patient.getPatientId());
            dto.setPatientUniqueId(patient.getPatientUniqueId());
            dto.setPatientName(patient.getPatientName());
            dto.setAge(patient.getAge());
            dto.setGender(patient.getGender());
            dto.setContactNumber(patient.getContactNumber());
            dto.setEmail(patient.getEmail());
            dto.setAddress(patient.getAddress());
            dto.setCreatedAt(patient.getCreatedAt());
            dto.setHasAccess(access.getAccessGranted());
            dto.setAccessGrantedAt(access.getAccessGrantedAt());
            dto.setAccessExpiresAt(access.getAccessExpiresAt());
            return dto;
        }).collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public boolean hasAccessToPatient(Long doctorId, Long patientId) {
        return patientDoctorAccessRepository
                .findActiveAccess(patientId, doctorId)
                .isPresent();
    }
}