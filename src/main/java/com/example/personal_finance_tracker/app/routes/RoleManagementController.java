package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.annotations.Loggable;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.RoleAssignmentDto;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public List<User> getAllUsers() {
        log.info("Entering getAllUsers method for accountant");
        List<User> users = userService.getAllUsers();
        log.info("Exiting getAllUsers method with {} users", users.size());
        return users;
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<User> assignRoles(@PathVariable Long userId, @RequestBody RoleAssignmentDto roleAssignmentDto) {
        roleAssignmentDto.setUserId(userId);
        User updatedUser = userService.assignRolesToUser(roleAssignmentDto);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<User> addRole(@PathVariable Long userId, @PathVariable String roleName) {
        User updatedUser = userService.addRoleToUser(userId, roleName);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<User> removeRole(@PathVariable Long userId, @PathVariable String roleName) {
        User updatedUser = userService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
}