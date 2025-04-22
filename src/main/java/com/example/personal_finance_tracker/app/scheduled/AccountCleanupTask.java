package com.example.personal_finance_tracker.app.scheduled;

import com.example.personal_finance_tracker.app.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCleanupTask {

    private final UserService userService;

    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void cleanupExpiredAccounts() {
        log.info("Running scheduled task to clean up expired accounts");
        userService.deleteExpiredAccounts();
    }
}