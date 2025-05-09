package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.exceptions.RateLimitExceededException;
import com.example.personal_finance_tracker.app.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
public class EmailStatsController {

    private final EmailService emailService;

    @Autowired
    public EmailStatsController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEmailStats() {
        log.info("Getting email statistics");
        return ResponseEntity.ok(emailService.getEmailStats());
    }

    @GetMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetEmailStats() {
        log.info("Resetting email statistics");
        emailService.resetEmailStats();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestParam String email) {
        log.info("Sending test email to: {}", email);
        try {
            emailService.sendEmail(
                email,
                "Test Email - Rate Limit Monitor",
                """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <h2>Test Email for Rate Limit Monitoring</h2>
                    <p>This is a test email sent from the Email Rate Limit Monitor.</p>
                    <p>This email is used to test and demonstrate the rate limiting functionality.</p>
                    <hr>
                    <p><small>Sent at: %s</small></p>
                </body>
                </html>
                """.formatted(java.time.LocalDateTime.now())
            );
            return ResponseEntity.ok(Collections.singletonMap("message", "Test email sent successfully"));
        } catch (RateLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error sending test email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to send test email: " + e.getMessage()));
        }
    }
} 