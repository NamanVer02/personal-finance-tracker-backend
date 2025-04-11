package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.ResetPasswordRequest;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final GAService gaService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo;

    public boolean initiatePasswordReset(String username) {
        log.info("Initiating password reset for username: {}", username);
        Optional<User> optionalUser = userRepo.findByUsername(username);
        boolean userExists = optionalUser.isPresent();
        log.info("User existence check result: {}", userExists);
        return userExists;
    }

    public boolean resetPassword(ResetPasswordRequest resetPasswordRequest) {
        log.info("Processing password reset for username: {}", resetPasswordRequest.getUsername());
        Optional<User> optionalUser = userRepo.findByUsername(resetPasswordRequest.getUsername());

        if (optionalUser.isEmpty()) {
            log.info("User not found for password reset");
            return false;
        }

        User user = optionalUser.get();
        log.info("User found for password reset: {}", user.getUsername());

        boolean isValid = gaService.isValid(user.getTwoFactorSecret(), resetPasswordRequest.getTwoFactorCode());
        log.info("2FA validation result: {}", isValid);

        if (!isValid) {
            log.info("2FA validation failed for user: {}", user.getUsername());
            return false;
        }

        log.info("Updating password for user: {}", user.getUsername());
        user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        userRepo.save(user);

        return true;
    }
}
