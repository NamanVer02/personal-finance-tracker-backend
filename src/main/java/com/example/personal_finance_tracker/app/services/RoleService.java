package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.exceptions.ResourceNotFoundException;
import com.example.personal_finance_tracker.app.interfaces.RoleInterface;
import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.repository.RoleRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class RoleService implements RoleInterface {

    private final RoleRepo roleRepo;

    public RoleService(RoleRepo roleRepo) {
        this.roleRepo = roleRepo;
    }

    @Override
    public Optional<Role> findByName(ERole name) {
        try {
            log.info("Finding role by name: {}", name);
            return roleRepo.findByName(name);
        } catch (DataAccessException e) {
            log.error("Error finding role by name: {}", name, e);
            throw new ResourceNotFoundException("Failed to find role by name");
        } catch (Exception e) {
            log.error("Unexpected error finding role by name: {}", name, e);
            throw new ResourceNotFoundException("Unexpected error finding role");
        }
    }
}
