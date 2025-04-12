package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.UserInterface;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("Searching for user with username: {}", username);
        try {
            return userRepo.findByUsername(username);
        } catch (Exception e) {
            log.error("Error finding user by username: {}", username, e);
            throw new RuntimeException("Failed to find user by username", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        log.info("Looking up user by ID: {}", id);
        try {
            return userRepo.findById(id);
        } catch (Exception e) {
            log.error("Error finding user by ID: {}", id, e);
            throw new RuntimeException("Failed to find user by ID", e);
        }
    }

    @Override
    public Boolean existsByUsername(String username) {
        log.info("Checking existence of username: {}", username);
        try {
            return userRepo.existsByUsername(username);
        } catch (Exception e) {
            log.error("Error checking if username exists: {}", username, e);
            throw new RuntimeException("Failed to check username existence", e);
        }
    }

    @Override
    public Boolean existsByEmail(String email) {
        log.info("Checking existence of email: {}", email);
        try {
            return userRepo.existsByEmail(email);
        } catch (Exception e) {
            log.error("Error checking if email exists: {}", email, e);
            throw new RuntimeException("Failed to check email existence", e);
        }
    }

    @Override
    public void save(User user) {
        log.info("Saving user with username: {}", user.getUsername());
        try {
            userRepo.save(user);
        } catch (Exception e) {
            log.error("Error saving user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to save user", e);
        }
    }

    @Override
    public List<User> getAllUsers() {
        log.info("Retrieving all users");
        try {
            return userRepo.findAll();
        } catch (Exception e) {
            log.error("Error retrieving all users", e);
            throw new RuntimeException("Failed to retrieve all users", e);
        }
    }

    @Override
    public String getUsernameByUserId(Long userId) {
        log.info("Getting username for user ID: {}", userId);
        try {
            User user = userRepo.findById(userId).orElse(null);
            return (user != null) ? user.getUsername() : "Unknown";
        } catch (Exception e) {
            log.error("Error getting username for user ID: {}", userId, e);
            throw new RuntimeException("Failed to get username by user ID", e);
        }
    }

    @Override
    public Long getUserIdByUsername(String username) {
        log.info("Getting user ID for username: {}", username);
        try {
            User user = userRepo.findByUsername(username).orElse(null);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            log.error("Error getting user ID for username: {}", username, e);
            throw new RuntimeException("Failed to get user ID by username", e);
        }
    }

    public Collection<? extends GrantedAuthority> getUserAuthorities(User user) {
        log.info("Retrieving authorities for user: {}", user.getUsername());
        try {
            return user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving authorities for user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to get user authorities", e);
        }
    }

    @Transactional
    public void incrementFailedAttempts(User user) {
        log.info("Incrementing failed attempts for user: {}", user.getUsername());
        try {
            int newFailedAttempts = user.getFailedAttempts() + 1;
            userRepo.updateFailedAttempts(newFailedAttempts, user.getUsername());
        } catch (Exception e) {
            log.error("Error incrementing failed attempts for user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to increment failed attempts", e);
        }
    }

    @Transactional
    public void resetFailedAttempts(String username) {
        log.info("Resetting failed attempts for user: {}", username);
        try {
            userRepo.updateFailedAttempts(0, username);
        } catch (Exception e) {
            log.error("Error resetting failed attempts for user: {}", username, e);
            throw new RuntimeException("Failed to reset failed attempts", e);
        }
    }

    @Transactional
    public void lockUser(User user) {
        log.info("Locking user account: {}", user.getUsername());
        try {
            userRepo.lockUser(LocalDateTime.now(), user.getUsername());
        } catch (Exception e) {
            log.error("Error locking user account: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to lock user account", e);
        }
    }

    public boolean isMaxFailedAttemptsReached(User user) {
        log.info("Checking max failed attempts for user: {}", user.getUsername());
        return user.getFailedAttempts() >= maxFailedAttempts;
    }

    @Override
    @Transactional
    public boolean updatePassword(Long id, String currentPassword, String newPassword) {
        log.info("Attempting password update for user ID: {}", id);
        try {
            Optional<User> userOpt = userRepo.findById(id);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("User found for password update: {}", user.getUsername());

                if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                    log.info("Current password verified for user: {}", user.getUsername());
                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepo.save(user);
                    return true;
                }
                log.info("Password verification failed for user: {}", user.getUsername());
            }
            return false;
        } catch (Exception e) {
            log.error("Error updating password for user ID: {}", id, e);
            throw new RuntimeException("Failed to update password", e);
        }
    }

    @Transactional
    public boolean deleteUser(Long id, String password) {
        log.info("Attempting to delete user ID: {}", id);
        try {
            Optional<User> userOpt = userRepo.findById(id);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("User found for deletion: {}", user.getUsername());

                if (passwordEncoder.matches(password, user.getPassword())) {
                    log.info("Password verified, deleting user: {}", user.getUsername());
                    userRepo.delete(user);
                    return true;
                }
                log.info("Password verification failed during deletion attempt for user: {}", user.getUsername());
            }
            return false;
        } catch (Exception e) {
            log.error("Error deleting user ID: {}", id, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    @Override
    @Transactional
    public boolean disableTwoFactorAuth(Long id, String password) {
        log.info("Attempting to disable 2FA for user ID: {}", id);
        try {
            Optional<User> userOpt = userRepo.findById(id);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("User found for 2FA disable: {}", user.getUsername());

                if (passwordEncoder.matches(password, user.getPassword())) {
                    log.info("Password verified, disabling 2FA for user: {}", user.getUsername());
                    user.setTwoFactorEnabled(false);
                    user.setTwoFactorSecret(null);
                    userRepo.save(user);
                    return true;
                }
                log.info("Password verification failed during 2FA disable attempt for user: {}", user.getUsername());
            }
            return false;
        } catch (Exception e) {
            log.error("Error disabling 2FA for user ID: {}", id, e);
            throw new RuntimeException("Failed to disable two-factor authentication", e);
        }
    }
}
