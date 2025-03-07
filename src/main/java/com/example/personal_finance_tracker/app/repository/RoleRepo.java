package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepo extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}
