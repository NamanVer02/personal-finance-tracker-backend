package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.dto.ResetPasswordRequest;
import com.example.personal_finance_tracker.app.services.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        log.info("Password reset request received");
        boolean initiated = passwordResetService.initiatePasswordReset(resetPasswordRequest.getNewPassword());
        log.debug("Initiation status: {}", initiated);
        return ResponseEntity.ok(Map.of("message", "If your username is registered, you will receive instructions"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        log.info("Password reset attempt received");
        boolean reset = passwordResetService.resetPassword(resetPasswordRequest);

        if (reset) {
            log.info("Password reset successful");
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } else {
            log.warn("Password reset failed");
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request or 2FA code"));
        }
    }
}
