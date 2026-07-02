package com.prescription.service;

import com.prescription.entity.Doctor;
import com.prescription.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    public Doctor getDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));
    }

    public Doctor getDoctorByUserId(Long userId) {
        return doctorRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Doctor record not found for user ID: " + userId));
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }
}