package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.UserInterface;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserInterface {

    @Value("${app.max_failed_attempts}")
    private int maxFailedAttempts;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Override
    public Optional<User> findById(Long id) { return userRepo.findById(id); }

    @Override
    public Boolean existsByUsername(String username) {
        return userRepo.existsByUsername(username);
    }

    @Override
    public Boolean existsByEmail(String email) {
        return userRepo.existsByEmail(email);
    }

    @Override
    public void save(User user) {
        userRepo.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    @Override
    public String getUsernameByUserId(Long userId) {
        User user = userRepo.findById(userId).orElse(null);
        return (user != null) ? user.getUsername() : "Unknown";
    }

    @Override
    public Long getUserIdByUsername(String username) {
        User user = userRepo.findByUsername(username).orElse(null);
        return user != null ? user.getId() : null;
    }

    public Collection<? extends GrantedAuthority> getUserAuthorities(User user) {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void incrementFailedAttempts(User user) {
        int newFailedAttempts = user.getFailedAttempts() + 1;
        userRepo.updateFailedAttempts(newFailedAttempts, user.getUsername());
    }

    @Transactional
    public void resetFailedAttempts(String username) {
        userRepo.updateFailedAttempts(0, username);
    }

    @Transactional
    public void lockUser(User user) {
        userRepo.lockUser(LocalDateTime.now(), user.getUsername());
    }

    public boolean isMaxFailedAttemptsReached(User user) {
        return user.getFailedAttempts() >= maxFailedAttempts;
    }

    @Override
    @Transactional
    public boolean updatePassword(Long id, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepo.findById(id);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify current password
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                // Encode and set new password
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepo.save(user);
                return true;
            }
        }

        return false;
    }

    @Transactional
    public boolean deleteUser(Long id, String password) {
        Optional<User> userOpt = userRepo.findById(id);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify password before deletion
            if (passwordEncoder.matches(password, user.getPassword())) {
                userRepo.delete(user);
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional
    public boolean disableTwoFactorAuth(Long id, String password) {
        Optional<User> userOpt = userRepo.findById(id);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Verify password before disabling 2FA
            if (passwordEncoder.matches(password, user.getPassword())) {
                user.setTwoFactorEnabled(false);
                // Clear 2FA secret if you store it
                user.setTwoFactorSecret(null);
                userRepo.save(user);
                return true;
            }
        }

        return false;
    }
}
