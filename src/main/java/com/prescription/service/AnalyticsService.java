package com.prescription.service;

import com.prescription.entity.Prescription;
import com.prescription.entity.PrescriptionMedicine;
import com.prescription.repository.PrescriptionRepository;
import com.prescription.repository.PrescriptionMedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionMedicineRepository medicineRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getTrends(LocalDate startDate, LocalDate endDate) {
        List<Prescription> prescriptions = prescriptionRepository.findByDateRangeForAnalytics(startDate, endDate);

        Map<String, Object> trends = new HashMap<>();
        trends.put("total_prescriptions", prescriptions.size());
        trends.put("date_range", Map.of("start", startDate, "end", endDate));

        // Group by date
        Map<LocalDate, Long> prescriptionsByDate = prescriptions.stream()
                .filter(p -> p.getPrescriptionDate() != null)
                .collect(Collectors.groupingBy(
                        Prescription::getPrescriptionDate,
                        Collectors.counting()
                ));
        trends.put("prescriptions_by_date", prescriptionsByDate);

        // Group by diagnosis
        Map<String, Long> diagnoses = prescriptions.stream()
                .filter(p -> p.getDiagnosis() != null && !p.getDiagnosis().isEmpty())
                .collect(Collectors.groupingBy(
                        Prescription::getDiagnosis,
                        Collectors.counting()
                ));
        trends.put("common_diagnoses", diagnoses);

        return trends;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMostPrescribedMedicines(LocalDate startDate, LocalDate endDate) {
        List<Prescription> prescriptions = prescriptionRepository.findByDateRangeForAnalytics(startDate, endDate);

        Map<String, Long> medicineCounts = new HashMap<>();
        for (Prescription p : prescriptions) {
            for (PrescriptionMedicine med : p.getMedicines()) {
                String name = med.getMedicineName();
                medicineCounts.put(name, medicineCounts.getOrDefault(name, 0L) + 1);
            }
        }

        // Sort by count descending
        List<Map.Entry<String, Long>> sortedMedicines = medicineCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total_unique_medicines", medicineCounts.size());
        result.put("top_medicines", sortedMedicines.stream()
                .map(e -> Map.of("medicine", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList()));

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCommonDiagnoses(LocalDate startDate, LocalDate endDate) {
        List<Prescription> prescriptions = prescriptionRepository.findByDateRangeForAnalytics(startDate, endDate);

        Map<String, Long> diagnosisCounts = prescriptions.stream()
                .filter(p -> p.getDiagnosis() != null && !p.getDiagnosis().isEmpty())
                .collect(Collectors.groupingBy(
                        Prescription::getDiagnosis,
                        Collectors.counting()
                ));

        List<Map.Entry<String, Long>> sortedDiagnoses = diagnosisCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total_unique_diagnoses", diagnosisCounts.size());
        result.put("top_diagnoses", sortedDiagnoses.stream()
                .map(e -> Map.of("diagnosis", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList()));

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOverallStatistics() {
        Long totalPrescriptions = prescriptionRepository.count();
        Long completedPrescriptions = prescriptionRepository.countByProcessingStatus("COMPLETED");
        Long failedPrescriptions = prescriptionRepository.countByProcessingStatus("FAILED");
        Long processingPrescriptions = prescriptionRepository.countByProcessingStatus("PROCESSING");

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_prescriptions", totalPrescriptions);
        stats.put("completed", completedPrescriptions);
        stats.put("failed", failedPrescriptions);
        stats.put("processing", processingPrescriptions);
        stats.put("success_rate", totalPrescriptions > 0 ?
                (completedPrescriptions * 100.0 / totalPrescriptions) : 0);

        return stats;
    }
}