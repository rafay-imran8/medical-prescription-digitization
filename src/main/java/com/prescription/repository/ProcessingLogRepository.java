package com.prescription.repository;

import com.prescription.entity.ProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, Long> {
}