package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    boolean existsByToken(String token);
    void deleteByExpiryDate(Date expiryDate);
}
