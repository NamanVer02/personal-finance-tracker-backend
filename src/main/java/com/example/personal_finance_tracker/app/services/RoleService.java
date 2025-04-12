package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.RoleInterface;
import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.repository.RoleRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class RoleService implements RoleInterface {
    @Autowired
    private RoleRepo roleRepo;

    @Override
    public Optional<Role> findByName(ERole name) {
        try {
            log.info("Finding role by name: {}", name);
            return roleRepo.findByName(name);
        } catch (DataAccessException e) {
            log.error("Error finding role by name: {}", name, e);
            throw new RuntimeException("Failed to find role by name", e);
        } catch (Exception e) {
            log.error("Unexpected error finding role by name: {}", name, e);
            throw new RuntimeException("Unexpected error finding role", e);
        }
    }
}
