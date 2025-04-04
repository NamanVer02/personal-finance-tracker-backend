package com.example.personal_finance_tracker.app.repository;

import com.example.personal_finance_tracker.app.models.TokenRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface TokenRegistryRepository extends JpaRepository<TokenRegistry, Long> {
    boolean existsByToken(String token);
    void deleteByExpiryDate(Date expiryDate);
    TokenRegistry findByToken(String token);
    List<TokenRegistry> findAllByUsername(String username);
}
