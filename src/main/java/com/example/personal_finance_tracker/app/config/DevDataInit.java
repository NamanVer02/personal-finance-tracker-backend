package com.example.personal_finance_tracker.app.config;

import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.RoleRepo;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import com.example.personal_finance_tracker.app.services.RoleService;
import com.example.personal_finance_tracker.app.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Profile("dev")
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

    // Constant predefined secret for admin 2FA
    private static final String TEST_2FA_SECRET = "JBSWY3DPEHPK3PXP";

    @Override
    public void run(String... args) throws Exception {
        // Initialize roles if they don't exist
        if (roleRepository.count() == 0) {
            Role userRole = new Role();
            userRole.setName(ERole.ROLE_USER);
            roleRepository.save(userRole);

            Role adminRole = new Role();
            adminRole.setName(ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);

            Role accountantRole = new Role();
            accountantRole.setName(ERole.ROLE_ACCOUNTANT);
            roleRepository.save(accountantRole);

            System.out.println("Roles initialized in database for dev environment");
        }

        if(userRepository.findByUsername("admin").isEmpty()) {
            Set<Role> roles = new HashSet<>();
            Role adminRole = roleService.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);

            User user = new User();
            user.setUsername("admin");
            user.setPassword(encoder.encode("admin"));
            user.setEmail("admin@gmail.com");
            user.setRoles(roles);

            // Set the constant 2FA secret for admin
            user.setTwoFactorEnabled(true);
            user.setTwoFactorSecret(TEST_2FA_SECRET);

            userService.save(user);

            System.out.println("Admin initialized in database with 2FA secret for dev environment");
            System.out.println("Please configure your Google Authenticator with this secret: " + TEST_2FA_SECRET);
        }

        // Add test user
        if(userRepository.findByUsername("testuser").isEmpty()) {
            Set<Role> roles = new HashSet<>();
            Role userRole = roleService.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);

            User user = new User();
            user.setUsername("testuser");
            user.setPassword(encoder.encode("password"));
            user.setEmail("test@example.com");
            user.setRoles(roles);
            user.setTwoFactorEnabled(false);

            user.setTwoFactorEnabled(true);
            user.setTwoFactorSecret(TEST_2FA_SECRET);

            userService.save(user);

            System.out.println("Test user initialized in database for dev environment");
        }

        // Add test accountant
        if(userRepository.findByUsername("accountant").isEmpty()) {
            Set<Role> roles = new HashSet<>();
            Role accountantRole = roleService.findByName(ERole.ROLE_ACCOUNTANT)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(accountantRole);

            User user = new User();
            user.setUsername("accountant");
            user.setPassword(encoder.encode("password"));
            user.setEmail("accountant@example.com");
            user.setRoles(roles);
            user.setTwoFactorEnabled(false);

            user.setTwoFactorEnabled(true);
            user.setTwoFactorSecret(TEST_2FA_SECRET);

            userService.save(user);

            System.out.println("Test accountant initialized in database for dev environment");
        }
    }
}
