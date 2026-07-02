package com.prescription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PrescriptionManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrescriptionManagementApplication.class, args);
    }
}