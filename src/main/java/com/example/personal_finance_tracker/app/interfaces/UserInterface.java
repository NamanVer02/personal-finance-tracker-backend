package com.example.personal_finance_tracker.app.interfaces;

import com.example.personal_finance_tracker.app.models.User;

import java.util.List;
import java.util.Optional;

public interface UserInterface {
    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    List<User> getAllUsers();
    String getUsernameByUserId(Long userId);
    Long getUserIdByUsername(String username);
    void save(User user);
    boolean updatePassword(Long id, String currentPassword, String newPassword);
    boolean deleteUser(Long id, String password);
    boolean disableTwoFactorAuth(Long id, String password);
}
