package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.UserInterface;
import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.RoleAssignmentDto;
import com.example.personal_finance_tracker.app.repository.RoleRepo;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.personal_finance_tracker.app.exceptions.ResourceNotFoundException;


import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    private RoleRepo roleRepo;

    @Override
    public Optional<User> findByUsername(String username) {
        log.info("Searching for user with username: {}", username);
        return userRepo.findByUsername(username);
    }

    @Override
    public Optional<User> findById(Long id) {
        log.info("Looking up user by ID: {}", id);
        return userRepo.findById(id);
    }

    @Override
    public Boolean existsByUsername(String username) {
        log.info("Checking existence of username: {}", username);
        return userRepo.existsByUsername(username);
    }

    @Override
    public Boolean existsByEmail(String email) {
        log.info("Checking existence of email: {}", email);
        return userRepo.existsByEmail(email);
    }

    @Override
    public void save(User user) {
        log.info("Saving user with username: {}", user.getUsername());
        userRepo.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        log.info("Retrieving all users");
        return userRepo.findAll();
    }

    @Override
    public String getUsernameByUserId(Long userId) {
        log.info("Getting username for user ID: {}", userId);
        User user = userRepo.findById(userId).orElse(null);
        return (user != null) ? user.getUsername() : "Unknown";
    }

    @Override
    public Long getUserIdByUsername(String username) {
        log.info("Getting user ID for username: {}", username);
        User user = userRepo.findByUsername(username).orElse(null);
        return user != null ? user.getId() : null;
    }

    public Collection<? extends GrantedAuthority> getUserAuthorities(User user) {
        log.info("Retrieving authorities for user: {}", user.getUsername());
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void incrementFailedAttempts(User user) {
        log.info("Incrementing failed attempts for user: {}", user.getUsername());
        int newFailedAttempts = user.getFailedAttempts() + 1;
        userRepo.updateFailedAttempts(newFailedAttempts, user.getUsername());
    }

    @Transactional
    public void resetFailedAttempts(String username) {
        log.info("Resetting failed attempts for user: {}", username);
        userRepo.updateFailedAttempts(0, username);
    }

    @Transactional
    public void lockUser(User user) {
        log.info("Locking user account: {}", user.getUsername());
        userRepo.lockUser(LocalDateTime.now(), user.getUsername());
    }

    public boolean isMaxFailedAttemptsReached(User user) {
        log.info("Checking max failed attempts for user: {}", user.getUsername());
        return user.getFailedAttempts() >= maxFailedAttempts;
    }

    @Override
    @Transactional
    public boolean updatePassword(Long id, String currentPassword, String newPassword) {
        log.info("Attempting password update for user ID: {}", id);
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
    }

    @Transactional
    public boolean deleteUser(Long id, String password) {
        log.info("Attempting to delete user ID: {}", id);
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
    }

    @Override
    @Transactional
    public boolean disableTwoFactorAuth(Long id, String password) {
        log.info("Attempting to disable 2FA for user ID: {}", id);
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
    }

    @Transactional
    public User assignRolesToUser(RoleAssignmentDto roleAssignmentDto) {
        User user = userRepo.findById(roleAssignmentDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + roleAssignmentDto.getUserId()));

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleAssignmentDto.getRoleNames()) {
            try {
                ERole eRole = ERole.valueOf(roleName.toUpperCase());
                Role role = roleRepo.findByName(eRole)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
                roles.add(role);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role name: " + roleName);
            }
        }

        user.setRoles(roles);
        return userRepo.save(user);
    }

    @Transactional
    public User addRoleToUser(Long userId, String roleName) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        try {
            ERole eRole = ERole.valueOf(roleName.toUpperCase());
            Role role = roleRepo.findByName(eRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

            user.getRoles().add(role);
            return userRepo.save(user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role name: " + roleName);
        }
    }

    @Transactional
    public User removeRoleFromUser(Long userId, String roleName) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        try {
            ERole eRole = ERole.valueOf(roleName.toUpperCase());
            Role role = roleRepo.findByName(eRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

            user.getRoles().removeIf(r -> r.getName() == eRole);
            return userRepo.save(user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role name: " + roleName);
        }
    }

    public User getUserById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
