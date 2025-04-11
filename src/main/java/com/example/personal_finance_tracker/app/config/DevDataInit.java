package com.example.personal_finance_tracker.app.config;

import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.RoleRepo;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import com.example.personal_finance_tracker.app.services.RoleService;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Profile("dev")
@Slf4j
public class DevDataInit implements CommandLineRunner {

    @Autowired
    private RoleRepo roleRepository;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder encoder;

    // Security Note: Should be externalized for real development environments
    private static final String TEST_2FA_SECRET = "JBSWY3DPEHPK3PXP";

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting development data initialization");

        try {
            initializeRoles();
            initializeTestUsers();
        } catch (Exception e) {
            log.error("Critical initialization error: {}", e.getMessage());
            throw e;
        }
    }

    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            log.info("Initializing development roles");

            Role userRole = createRole(ERole.ROLE_USER);
            Role adminRole = createRole(ERole.ROLE_ADMIN);
            Role accountantRole = createRole(ERole.ROLE_ACCOUNTANT);

            roleRepository.save(userRole);
            roleRepository.save(adminRole);
            roleRepository.save(accountantRole);

            log.info("Created 3 development roles: USER, ADMIN, ACCOUNTANT");
        } else {
            log.debug("Roles already exist - count: {}", roleRepository.count());
        }
    }

    private Role createRole(ERole roleName) {
        Role role = new Role();
        role.setName(roleName);
        log.debug("Created role: {}", roleName);
        return role;
    }

    private void initializeTestUsers() {
        checkAndCreateUser(
                "admin",
                "admin",
                ERole.ROLE_ADMIN,
                true,
                "admin@gmail.com"
        );

        checkAndCreateUser(
                "testuser",
                "password",
                ERole.ROLE_USER,
                true,
                "test@example.com"
        );

        checkAndCreateUser(
                "accountant",
                "password",
                ERole.ROLE_ACCOUNTANT,
                true,
                "accountant@example.com"
        );
    }

    private void checkAndCreateUser(
            String username,
            String password,
            ERole role,
            boolean twoFactorEnabled,
            String email
    ) {
        userRepository.findByUsername(username).ifPresentOrElse(
                existingUser -> log.debug("User {} already exists", username),
                () -> createTestUser(username, password, role, twoFactorEnabled, email)
        );
    }

    private void createTestUser(
            String username,
            String password,
            ERole role,
            boolean twoFactorEnabled,
            String email
    ) {
        log.info("Creating test user: {}", username);

        Set<Role> roles = new HashSet<>();
        Role userRole = roleService.findByName(role)
                .orElseThrow(() -> {
                    log.error("Role {} not found", role);
                    return new RuntimeException("Role not found: " + role);
                });
        roles.add(userRole);

        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setEmail(email);
        user.setRoles(roles);
        user.setTwoFactorEnabled(twoFactorEnabled);
        user.setTwoFactorSecret(TEST_2FA_SECRET);

        userService.save(user);

        log.warn("Security Note: Created test user {} with password '{}'", username, password);
        log.info("2FA secret for {}: {}", username, TEST_2FA_SECRET);
    }
}
