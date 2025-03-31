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
@Profile("!dev & !prod")
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

    // Constant predefined secret for admin 2FA
    private static final String ADMIN_2FA_SECRET = "JBSWY3DPEHPK3PXP"; // Example secret - replace with your own

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Running with default profile (neither dev nor prod active)");
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

            System.out.println("Roles initialized in database");
        }


        if(userRepository.findByUsername("admin").isEmpty()) {
            Set<Role> roles = new HashSet<>();
            Role adminRole = roleService.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);

            User user = new User();
            user.setUsername("admin");
            user.setPassword(encoder.encode("admin"));
            user.setEmail("admin@gmail.com");
            user.setRoles(roles);

            // Set the constant 2FA secret for admin
            user.setTwoFactorEnabled(true);
            user.setTwoFactorSecret(ADMIN_2FA_SECRET);

            userService.save(user);

            System.out.println("Admin initialized in database with constant 2FA secret");
            System.out.println("Please configure your Google Authenticator with this secret: " + ADMIN_2FA_SECRET);
        } else {
            // If admin already exists, ensure it has the constant 2FA secret
            User adminUser = userRepository.findByUsername("admin").get();
            if (!ADMIN_2FA_SECRET.equals(adminUser.getTwoFactorSecret()) || !adminUser.isTwoFactorEnabled()) {
                adminUser.setTwoFactorEnabled(true);
                adminUser.setTwoFactorSecret(ADMIN_2FA_SECRET);
                userRepository.save(adminUser);
                System.out.println("Admin 2FA secret updated to constant value");
                System.out.println("Please configure your Google Authenticator with this secret: " + ADMIN_2FA_SECRET);
            }
        }
    }
}