package com.example.personal_finance_tracker.app.config;

import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.repository.RoleRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@Slf4j
public class ProdDataInit implements CommandLineRunner {

    @Autowired
    private RoleRepo roleRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting production data initialization");

        try {
            if (roleRepository.count() == 0) {
                log.info("Initializing roles in production database");

                Role userRole = createRole(ERole.ROLE_USER);
                Role adminRole = createRole(ERole.ROLE_ADMIN);
                Role accountantRole = createRole(ERole.ROLE_ACCOUNTANT);

                roleRepository.save(userRole);
                roleRepository.save(adminRole);
                roleRepository.save(accountantRole);

                log.info("Successfully initialized 3 roles: USER, ADMIN, ACCOUNTANT");
            } else {
                log.debug("Roles already exist in database - count: {}", roleRepository.count());
            }
        } catch (Exception e) {
            log.error("Error during production data initialization: {}", e.getMessage());
            throw e;
        }
    }

    private Role createRole(ERole roleName) {
        Role role = new Role();
        role.setName(roleName);
        log.debug("Created role entity for: {}", roleName);
        return role;
    }
}
