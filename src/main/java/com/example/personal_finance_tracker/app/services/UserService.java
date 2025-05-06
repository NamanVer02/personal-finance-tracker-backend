package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.exceptions.ResourceNotFoundException;
import com.example.personal_finance_tracker.app.interfaces.UserInterface;
import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.RoleAssignmentDto;
import com.example.personal_finance_tracker.app.repository.RoleRepo;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class UserService implements UserInterface {

    @Value("${app.max_failed_attempts}")
    private int maxFailedAttempts;

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepo roleRepo;

    private static final String CACHE_MISS_MESSAGE = "Cache MISS for userById: {}";
    private static final String USER_NOT_FOUND_MESSAGE = "User not found with id: ";
    private static final String ROLE_NOT_FOUND_MESSAGE = "Role not found: ";

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder, RoleRepo roleRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.roleRepo = roleRepo;
    }

    @Override
    @Cacheable(value = "userByUsername", key = "#username", unless = "#result == null")
    public Optional<User> findByUsername(String username) {
        log.info("Cache MISS for userByUsername: {}", username);
        try {
            Optional<User> result = userRepo.findByUsername(username);
            if (result.isPresent()) {
                log.info("Found user by username: {} in database", username);
            } else {
                log.info("User with username: {} not found in database", username);
            }
            return result;
        } catch (Exception e) {
            log.error("Error finding user by username: {}", username, e);
            throw new ResourceNotFoundException("Failed to find user by username");
        }
    }

    @Override
    @Cacheable(value = "userById", key = "#id", unless = "#result == null")
    public Optional<User> findById(Long id) {
        log.info(CACHE_MISS_MESSAGE, id);
        try {
            Optional<User> result = userRepo.findById(id);
            if (result.isPresent()) {
                log.info("Found user by ID: {} in database", id);
            } else {
                log.info("User with ID: {} not found in database", id);
            }
            return result;
        } catch (Exception e) {
            log.error("Error finding user by ID: {}", id, e);
            throw new ResourceNotFoundException("Failed to find user by ID");
        }
    }

    @Override
    public Boolean existsByUsername(String username) {
        log.info("Checking existence of username: {}", username);
        try {
            return userRepo.existsByUsername(username);
        } catch (Exception e) {
            log.error("Error checking if username exists: {}", username, e);
            throw new ResourceNotFoundException("Failed to check username existence");
        }
    }

    @Override
    public Boolean existsByEmail(String email) {
        log.info("Checking existence of email: {}", email);
        try {
            return userRepo.existsByEmail(email);
        } catch (Exception e) {
            log.error("Error checking if email exists: {}", email, e);
            throw new ResourceNotFoundException("Failed to check email existence");
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#user.id", condition = "#user.id != null"),
            @CacheEvict(value = "userByUsername", key = "#user.username"),
            @CacheEvict(value = "allUsers", allEntries = true),
            @CacheEvict(value = "usernameById", key = "#user.id", condition = "#user.id != null"),
            @CacheEvict(value = "userIdByUsername", key = "#user.username"),
            @CacheEvict(value = "userAuthorities", allEntries = true)
    })
    public void save(User user) {
        log.info("Saving user with username: {} and evicting related caches", user.getUsername());
        try {
            userRepo.save(user);
        } catch (Exception e) {
            log.error("Error saving user: {}", user.getUsername(), e);
            throw new ResourceNotFoundException("Failed to save user");
        }
    }

    @Override
    @Cacheable(value = "allUsers")
    public List<User> getAllUsers() {
        log.info("Cache MISS for allUsers - retrieving all users from database");
        try {
            List<User> users = userRepo.findAll();
            log.info("Successfully retrieved {} users from database", users.size());
            return users;
        } catch (Exception e) {
            log.error("Error retrieving all users", e);
            throw new ResourceNotFoundException("Failed to retrieve all users");
        }
    }

    @Override
    @Cacheable(value = "usernameById", key = "#userId", unless = "#result == 'Unknown'")
    public String getUsernameByUserId(Long userId) {
        log.info("Cache MISS for usernameById: {}", userId);
        try {
            User user = userRepo.findById(userId).orElse(null);
            String username = (user != null) ? user.getUsername() : "Unknown";
            log.info("Retrieved username: {} for user ID: {} from database", username, userId);
            return username;
        } catch (Exception e) {
            log.error("Error getting username for user ID: {}", userId, e);
            throw new ResourceNotFoundException("Failed to get username by user ID");
        }
    }

    @Override
    @Cacheable(value = "userIdByUsername", key = "#username", unless = "#result == null")
    public Long getUserIdByUsername(String username) {
        log.info("Cache MISS for userIdByUsername: {}", username);
        try {
            User user = userRepo.findByUsername(username).orElse(null);
            Long userId = user != null ? user.getId() : null;
            log.info("Retrieved user ID: {} for username: {} from database", userId, username);
            return userId;
        } catch (Exception e) {
            log.error("Error getting user ID for username: {}", username, e);
            throw new ResourceNotFoundException("Failed to get user ID by username");
        }
    }

    @Cacheable(value = "userAuthorities", key = "#user.username")
    public Collection<? extends GrantedAuthority> getUserAuthorities(User user) {
        log.info("Cache MISS for userAuthorities: {}", user.getUsername());
        try {
            Collection<? extends GrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                    .toList();
            log.info("Retrieved {} authorities for user: {} from database", authorities.size(), user.getUsername());
            return authorities;
        } catch (Exception e) {
            log.error("Error retrieving authorities for user: {}", user.getUsername(), e);
            throw new ResourceNotFoundException("Failed to get user authorities");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#user.id"),
            @CacheEvict(value = "userByUsername", key = "#user.username"),
            @CacheEvict(value = "userAuthorities", key = "#user.username")
    })
    public void incrementFailedAttempts(User user) {
        log.info("Incrementing failed attempts for user: {} and evicting related caches", user.getUsername());
        try {
            int newFailedAttempts = user.getFailedAttempts() + 1;
            userRepo.updateFailedAttempts(newFailedAttempts, user.getUsername());
        } catch (Exception e) {
            log.error("Error incrementing failed attempts for user: {}", user.getUsername(), e);
            throw new ResourceNotFoundException("Failed to increment failed attempts");
        }
    }

    @Transactional
    @CacheEvict(value = "userByUsername", key = "#username")
    public void resetFailedAttempts(String username) {
        log.info("Resetting failed attempts for user: {} and evicting from cache", username);
        try {
            userRepo.updateFailedAttempts(0, username);
        } catch (Exception e) {
            log.error("Error resetting failed attempts for user: {}", username, e);
            throw new ResourceNotFoundException("Failed to reset failed attempts");
        }
    }

    @Transactional
    @CacheEvict(value = "userByUsername", key = "#user.username")
    public void lockUser(User user) {
        log.info("Locking user account: {} and evicting from cache", user.getUsername());
        try {
            userRepo.lockUser(LocalDateTime.now(), user.getUsername());
        } catch (Exception e) {
            log.error("Error locking user account: {}", user.getUsername(), e);
            throw new ResourceNotFoundException("Failed to lock user account");
        }
    }

    public boolean isMaxFailedAttemptsReached(User user) {
        log.info("Checking max failed attempts for user: {}", user.getUsername());
        return user.getFailedAttempts() >= maxFailedAttempts;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#id"),
            @CacheEvict(value = "userByUsername", allEntries = true),
            @CacheEvict(value = "allUsers", allEntries = true)
    })
    public boolean updatePassword(Long id, String currentPassword, String newPassword) {
        log.info("Attempting password update for user ID: {} and evicting related caches", id);
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
            throw new ResourceNotFoundException("Failed to update password");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#id"),
            @CacheEvict(value = "userByUsername", allEntries = true),
            @CacheEvict(value = "allUsers", allEntries = true),
            @CacheEvict(value = "usernameById", key = "#id"),
            @CacheEvict(value = "userIdByUsername", allEntries = true),
            @CacheEvict(value = "userAuthorities", allEntries = true)
    })
    public boolean deleteUser(Long id, String password) {
        log.info("Attempting to delete user with ID: {} and evicting all related caches", id);
        try {
            Optional<User> userOpt = userRepo.findById(id);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("User found for deletion: {}", user.getUsername());

                if (passwordEncoder.matches(password, user.getPassword())) {
                    log.info("Password verified for user deletion: {}", user.getUsername());
                    userRepo.delete(user);
                    return true;
                }
                log.info("Password verification failed for user deletion: {}", user.getUsername());
            }
            return false;
        } catch (Exception e) {
            log.error("Error deleting user with ID: {}", id, e);
            throw new ResourceNotFoundException("Failed to delete user");
        }
    }

    @Override
    @Transactional
    @Caching(put = {
            @CachePut(value = "userById", key = "#userId", unless = "#result == null")
    }, evict = {
            @CacheEvict(value = "userByUsername", allEntries = true),
            @CacheEvict(value = "allUsers", allEntries = true)
    })
    public User setAccountExpiration(Long userId, boolean expired) {
        log.info("Setting account expiration status to {} for user ID: {} and updating caches", expired, userId);
        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + userId));

            user.setAccountExpired(expired);
            userRepo.save(user);

            log.info("Successfully set account expiration for user: {}", user.getUsername());
            return user;
        } catch (ResourceNotFoundException e) {
            log.error("User not found for setting expiration: {}", userId);
            throw e;
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"userById", "userByUsername", "allUsers", "usernameById", "userIdByUsername", "userAuthorities"}, allEntries = true)
    public void deleteExpiredAccounts() {
        log.info("Checking for expired accounts to delete and evicting all caches");
        try {
            List<User> users = userRepo.findAll();
            int deletedCount = 0;

            for (User user : users) {
                if (user.isAccountExpired() && user.getExpirationDate() != null
                        && LocalDateTime.now().isAfter(user.getExpirationDate())) {
                    log.info("Deleting expired account: {}", user.getUsername());
                    userRepo.delete(user);
                    deletedCount++;
                }
            }

            log.info("Deleted {} expired accounts", deletedCount);
        } catch (Exception e) {
            log.error("Error deleting expired accounts", e);
            throw new ResourceNotFoundException("Failed to delete expired accounts");
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#id"),
            @CacheEvict(value = "userByUsername", allEntries = true),
            @CacheEvict(value = "userAuthorities", allEntries = true)
    })
    public boolean disableTwoFactorAuth(Long id, String password) {
        log.info("Attempting to disable 2FA for user ID: {} and evicting related caches", id);
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
            throw new ResourceNotFoundException("Failed to disable two-factor authentication");
        }
    }

    @Transactional
    @Caching(put = {
            @CachePut(value = "userById", key = "#roleAssignmentDto.userId", unless = "#result == null")
    }, evict = {
            @CacheEvict(value = "userByUsername", allEntries = true),
            @CacheEvict(value = "userAuthorities", allEntries = true)
    })
    public User assignRolesToUser(RoleAssignmentDto roleAssignmentDto) {
        log.info("Assigning roles to user ID: {} and updating caches", roleAssignmentDto.getUserId());
        User user = userRepo.findById(roleAssignmentDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + roleAssignmentDto.getUserId()));

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleAssignmentDto.getRoleNames()) {
            try {
                ERole eRole = ERole.valueOf(roleName.toUpperCase());
                Role role = roleRepo.findByName(eRole)
                        .orElseThrow(() -> new ResourceNotFoundException(ROLE_NOT_FOUND_MESSAGE + roleName));
                roles.add(role);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role name: " + roleName);
            }
        }

        user.setRoles(roles);
        return userRepo.save(user);
    }

    @Transactional
    @Caching(put = {
            @CachePut(value = "userById", key = "#userId", unless = "#result == null")
    }, evict = {
            @CacheEvict(value = "userByUsername", allEntries = true),
            @CacheEvict(value = "userAuthorities", allEntries = true)
    })
    public User addRoleToUser(Long userId, String roleName) {
        log.info("Adding role {} to user ID: {} and updating caches", roleName, userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + userId));

        try {
            ERole eRole = ERole.valueOf(roleName.toUpperCase());
            Role role = roleRepo.findByName(eRole)
                    .orElseThrow(() -> new ResourceNotFoundException(ROLE_NOT_FOUND_MESSAGE + roleName));

            user.getRoles().add(role);
            return userRepo.save(user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role name: " + roleName);
        }
    }

    @Transactional
    @Caching(put = {
            @CachePut(value = "userById", key = "#userId", unless = "#result == null")
    }, evict = {
            @CacheEvict(value = "userByUsername", allEntries = true),
            @CacheEvict(value = "userAuthorities", allEntries = true)
    })
    public User removeRoleFromUser(Long userId, String roleName) {
        log.info("Removing role {} from user ID: {} and updating caches", roleName, userId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + userId));

        try {
            ERole eRole = ERole.valueOf(roleName.toUpperCase());

            user.getRoles().removeIf(r -> r.getName() == eRole);
            return userRepo.save(user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role name: " + roleName);
        }
    }

    @Cacheable(value = "userById", key = "#id", unless = "#result == null")
    public User getUserById(Long id) {
        log.info(CACHE_MISS_MESSAGE, id);
        try {
            User user = userRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + id));
            log.info("Retrieved user by ID: {} from database", id);
            return user;
        } catch (ResourceNotFoundException e) {
            log.error("User not found with ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Error getting user by ID: {}", id, e);
            throw new ResourceNotFoundException("Failed to get user by ID");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#userId"),
            @CacheEvict(value = "userByUsername", allEntries = true),
            @CacheEvict(value = "allUsers", allEntries = true)
    })
    public void updateProfileImage(Long userId, String base64Image) {
        log.info("Updating profile image for user ID: {} and evicting related caches", userId);
        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setProfileImage(base64Image);
            userRepo.save(user);
            log.info("Successfully updated profile image for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Error updating profile image for user ID: {}", userId, e);
            throw new ResourceNotFoundException("Failed to update profile image");
        }
    }

    // This method can be added to show cache hits explicitly
    @Cacheable(value = "userById", key = "#id", unless = "#result == null")
    public User getUserByIdWithLogging(Long id) {
        // This code block only executes on cache miss
        log.info(CACHE_MISS_MESSAGE, id);
        try {
            return userRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_MESSAGE + id));
        } catch (Exception e) {
            log.error("Error getting user by ID: {}", id, e);
            throw new ResourceNotFoundException("Failed to get user by ID");
        }
    }
}
