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
@Profile("!dev & !prod")
@Slf4j
public class InitConfig implements CommandLineRunner {

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

    // Security Note: This should be externalized to secure configuration
    private static final String ADMIN_2FA_SECRET = "JBSWY3DPEHPK3PXP";

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing default configuration (neither dev nor prod active)");

        try {
            initializeRoles();
            initializeAdminUser();
        } catch (Exception e) {
            log.error("Critical error during initialization: {}", e.getMessage());
            throw e;
        }
    }

    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            log.info("Initializing default roles");

            Role userRole = createRole(ERole.ROLE_USER);
            Role adminRole = createRole(ERole.ROLE_ADMIN);
            Role accountantRole = createRole(ERole.ROLE_ACCOUNTANT);

            roleRepository.save(userRole);
            roleRepository.save(adminRole);
            roleRepository.save(accountantRole);

            log.info("Successfully initialized 3 default roles: USER, ADMIN, ACCOUNTANT");
        } else {
            log.debug("Roles already exist - count: {}", roleRepository.count());
        }
    }

    private Role createRole(ERole roleName) {
        Role role = new Role();
        role.setName(roleName);
        log.debug("Created role entity: {}", roleName);
        return role;
    }

    private void initializeAdminUser() {
        userRepository.findByUsername("admin").ifPresentOrElse(
                existingAdmin -> handleExistingAdmin(existingAdmin),
                () -> createNewAdmin()
        );
    }

    private void handleExistingAdmin(User adminUser) {
        if (!ADMIN_2FA_SECRET.equals(adminUser.getTwoFactorSecret()) || !adminUser.isTwoFactorEnabled()) {
            log.warn("Admin 2FA configuration mismatch - updating to predefined secret");

            adminUser.setTwoFactorEnabled(true);
            adminUser.setTwoFactorSecret(ADMIN_2FA_SECRET);
            userRepository.save(adminUser);

            log.warn("Admin 2FA secret updated. Security Note: This should be changed in production!");
            log.info("Configure Google Authenticator with secret: {}", ADMIN_2FA_SECRET);
        } else {
            log.debug("Admin user already has correct 2FA configuration");
        }
    }

    private void createNewAdmin() {
        log.info("Creating initial admin user");

        Set<Role> roles = new HashSet<>();
        Role adminRole = roleService.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> {
                    log.error("Admin role not found during admin creation");
                    return new RuntimeException("Admin role not found");
                });
        roles.add(adminRole);

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("admin"));
        admin.setEmail("admin@example.com");
        admin.setRoles(roles);
        admin.setTwoFactorEnabled(true);
        admin.setTwoFactorSecret(ADMIN_2FA_SECRET);

        userService.save(admin);

        log.warn("Security Alert: Initial admin created with default credentials and 2FA secret");
        log.info("Admin initialized with 2FA secret: {}", ADMIN_2FA_SECRET);
    }
}
