package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = ?1 WHERE u.username = ?2")
    void updateFailedAttempts(Integer failedAttempts, String username);


    @Modifying
    @Query("UPDATE User u SET u.lockTime = ?1 WHERE u.username = ?2")
    void lockUser(LocalDateTime lockTime, String username);

    @Query("SELECT u FROM User u WHERE u.lastLoginDate < :expirationDate")
    List<User> findExpiredAccounts(@Param("expirationDate") LocalDateTime expirationDate);

    List<User> findAllByLockTimeIsNotNull();
}
