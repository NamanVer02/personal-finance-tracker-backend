package com.example.personal_finance_tracker.app.interfaces;

import com.example.personal_finance_tracker.app.models.User;

import java.util.Optional;

public interface UserInterface {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    User save(User user);
}
