package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.dto.ResetPasswordRequest;
import com.example.personal_finance_tracker.app.services.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        boolean initiated = passwordResetService.initiatePasswordReset(resetPasswordRequest.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "If your username is registered, you will receive instructions"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
        boolean reset = passwordResetService.resetPassword(resetPasswordRequest);

        if (reset) {
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request or 2FA code"));
        }
    }

}
