package com.example.personal_finance_tracker.app.interfaces;

import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;

import java.util.Optional;

public interface RoleInterface {
    Optional<Role> findByName(ERole name);
}
