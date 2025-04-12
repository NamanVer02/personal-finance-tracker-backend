package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.TokenRegistry;
import com.example.personal_finance_tracker.app.repository.TokenRegistryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class TokenRegistryService {
    @Autowired
    private TokenRegistryRepository tokenRegistryRepository;

    public void blacklistToken(String token, Date expiryDate) {
        log.info("Blacklisting token: {}", token);
        try {
            TokenRegistry tokenRegistry = tokenRegistryRepository.findByToken(token);
            tokenRegistry.setExpiryDate(expiryDate);
            tokenRegistry.setActive(false);
            tokenRegistryRepository.save(tokenRegistry);
        } catch (DataAccessException e) {
            log.error("Error blacklisting token", e);
            throw new RuntimeException("Failed to blacklist token", e);
        } catch (NullPointerException e) {
            log.error("Token not found for blacklisting", e);
            throw new RuntimeException("Token not found for blacklisting", e);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        log.info("Checking blacklist status for token: {}", token);
        try {
            TokenRegistry tokenRegistry = tokenRegistryRepository.findByToken(token);
            return !tokenRegistry.isActive();
        } catch (DataAccessException e) {
            log.error("Error checking if token is blacklisted", e);
            throw new RuntimeException("Failed to check if token is blacklisted", e);
        } catch (NullPointerException e) {
            log.error("Token not found when checking blacklist status", e);
            return false;
        }
    }

    @Scheduled(fixedRate = 60000)
    public void deleteExpiredTokens() {
        log.info("Running scheduled token cleanup");
        try {
            tokenRegistryRepository.deleteByExpiryDate(new Date());
        } catch (DataAccessException e) {
            log.error("Error deleting expired tokens", e);
            // We don't throw here since this is a scheduled task
        }
    }

    public void saveTokenRegistry(TokenRegistry tokenRegistry) {
        log.info("Saving token registry entry for username: {}", tokenRegistry.getUsername());
        try {
            tokenRegistryRepository.save(tokenRegistry);
        } catch (DataAccessException e) {
            log.error("Error saving token registry for username: {}", tokenRegistry.getUsername(), e);
            throw new RuntimeException("Failed to save token registry", e);
        }
    }

    public void invalidatePreviousTokens(String username) {
        log.info("Invalidating all previous tokens for username: {}", username);
        try {
            List<TokenRegistry> tokensToInvalidate = tokenRegistryRepository.findAllByUsername(username);
            for (TokenRegistry tokenRegistry : tokensToInvalidate) {
                tokenRegistry.setActive(false);
                tokenRegistryRepository.save(tokenRegistry);
            }
        } catch (DataAccessException e) {
            log.error("Error invalidating previous tokens for username: {}", username, e);
            throw new RuntimeException("Failed to invalidate previous tokens", e);
        }
    }
}
