package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.ResetPasswordRequest;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetService passwordResetService;
    private final GAService gaService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo;

    public boolean initiatePasswordReset (String username) {
        Optional<User> optionalUser = userRepo.findByUsername(username);
        return optionalUser.isPresent();
    }

    public boolean resetPassword (ResetPasswordRequest resetPasswordRequest) {
        Optional<User> optionalUser = userRepo.findByUsername(resetPasswordRequest.getUsername());

        if (optionalUser.isEmpty()) { return false; }

        User user = optionalUser.get();

        boolean isValid = gaService.isValid(user.getTwoFactorSecret(), resetPasswordRequest.getTwoFactorCode());

        if (!isValid) { return false; }

        user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        userRepo.save(user);

        return true;
    }

}
