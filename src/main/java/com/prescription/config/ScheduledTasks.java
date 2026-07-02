package com.prescription.config;

import com.prescription.service.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    @Autowired
    private AccessControlService accessControlService;

    /**
     * Check and revoke expired access every hour
     */
    @Scheduled(cron = "0 0 * * * *")  // Run at the start of every hour
    public void revokeExpiredAccess() {
        System.out.println("Running scheduled task: Revoking expired access...");
        try {
            accessControlService.revokeExpiredAccess();
            System.out.println("Expired access revocation completed successfully");
        } catch (Exception e) {
            System.err.println("Error in scheduled task: " + e.getMessage());
            e.printStackTrace();
        }
    }
}