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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
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

            user.setRoles(roles);
            userService.save(user);

            System.out.println("Admin initialized in database");
        }
    }
}
