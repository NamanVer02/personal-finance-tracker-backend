package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.RoleInterface;
import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.repository.RoleRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoleService implements RoleInterface {
    @Autowired
    private RoleRepo roleRepo;

    @Override
    public Optional<Role> findByName(ERole name) {
        return roleRepo.findByName(name);
    }
}
