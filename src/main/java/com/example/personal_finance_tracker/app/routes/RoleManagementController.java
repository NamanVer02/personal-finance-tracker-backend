package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.annotations.Loggable;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.RoleAssignmentDto;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Loggable
public class RoleManagementController {

    private final UserService userService;

    @GetMapping()
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    @Cacheable(value = "allUsers")
    public List<User> getAllUsers() {
        log.info("Entering getAllUsers method for accountant");
        List<User> users = userService.getAllUsers();
        log.info("Exiting getAllUsers method with {} users", users.size());
        return users;
    }

    @PutMapping("/{userId}/roles")
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#userId"),
            @CacheEvict(value = "userByUsername", key = "#result.body.username"),
            @CacheEvict(value = "userAuthorities", key = "#result.body.username"),
            @CacheEvict(value = "allUsers", allEntries = true)
    })
    public ResponseEntity<User> assignRoles(@PathVariable Long userId, @RequestBody RoleAssignmentDto roleAssignmentDto) {
        try {
            roleAssignmentDto.setUserId(userId);
            User updatedUser = userService.assignRolesToUser(roleAssignmentDto);
            return ResponseEntity.ok(updatedUser);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic locking failure while assigning roles to user ID: {}", userId);
            return ResponseEntity.status(409).build(); // 409 Conflict
        }
    }

    @PostMapping("/{userId}/roles/{roleName}")
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#userId"),
            @CacheEvict(value = "userByUsername", key = "#result.body.username"),
            @CacheEvict(value = "userAuthorities", key = "#result.body.username"),
            @CacheEvict(value = "allUsers", allEntries = true)
    })
    public ResponseEntity<User> addRole(@PathVariable Long userId, @PathVariable String roleName, @RequestParam(required = false) Long version) {
        try {
            User updatedUser = userService.addRoleToUser(userId, roleName);
            return ResponseEntity.ok(updatedUser);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic locking failure while adding role {} to user ID: {}", roleName, userId);
            return ResponseEntity.status(409).build(); // 409 Conflict
        }
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#userId"),
            @CacheEvict(value = "userByUsername", key = "#result.body.username"),
            @CacheEvict(value = "userAuthorities", key = "#result.body.username"),
            @CacheEvict(value = "allUsers", allEntries = true)
    })
    public ResponseEntity<User> removeRole(@PathVariable Long userId, @PathVariable String roleName, @RequestParam(required = false) Long version) {
        try {
            User updatedUser = userService.removeRoleFromUser(userId, roleName);
            return ResponseEntity.ok(updatedUser);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic locking failure while removing role {} from user ID: {}", roleName, userId);
            return ResponseEntity.status(409).build(); // 409 Conflict
        }
    }

    @GetMapping("/{userId}")
    @Cacheable(value = "userById", key = "#userId")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}/expire")
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#userId"),
            @CacheEvict(value = "userByUsername", key = "#result.body.username"),
            @CacheEvict(value = "userAuthorities", key = "#result.body.username"),
            @CacheEvict(value = "allUsers", allEntries = true)
    })
    public ResponseEntity<User> expireAccount(@PathVariable Long userId, @RequestParam(required = false) Long version) {
        try {
            log.info("Request to expire account for user ID: {}", userId);
            User updatedUser = userService.setAccountExpiration(userId, true);
            return ResponseEntity.ok(updatedUser);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic locking failure while expiring account for user ID: {}", userId);
            return ResponseEntity.status(409).build(); // 409 Conflict
        }
    }

    @PutMapping("/{userId}/unexpire")
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#userId"),
            @CacheEvict(value = "userByUsername", key = "#result.body.username"),
            @CacheEvict(value = "userAuthorities", key = "#result.body.username"),
            @CacheEvict(value = "allUsers", allEntries = true)
    })
    public ResponseEntity<User> unexpireAccount(@PathVariable Long userId, @RequestParam(required = false) Long version) {
        try {
            log.info("Request to unexpire account for user ID: {}", userId);
            User updatedUser = userService.setAccountExpiration(userId, false);
            return ResponseEntity.ok(updatedUser);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic locking failure while unexpiring account for user ID: {}", userId);
            return ResponseEntity.status(409).build(); // 409 Conflict
        }
    }
}
