package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.UserInterface;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService implements UserInterface {
    @Autowired
    private UserRepo userRepo;

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    @Override
    public Boolean existsByUsername(String username) {
        return userRepo.existsByUsername(username);
    }

    @Override
    public Boolean existsByEmail(String email) {
        return userRepo.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepo.save(user);
    }
}
