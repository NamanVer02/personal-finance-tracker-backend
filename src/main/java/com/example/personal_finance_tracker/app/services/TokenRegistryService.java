package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.TokenRegistry;
import com.example.personal_finance_tracker.app.repository.TokenRegistryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
        TokenRegistry tokenRegistry = tokenRegistryRepository.findByToken(token);
        tokenRegistry.setExpiryDate(expiryDate);
        tokenRegistry.setActive(false);
        tokenRegistryRepository.save(tokenRegistry);
    }

    public boolean isTokenBlacklisted(String token) {
        log.info("Checking blacklist status for token: {}", token);
        TokenRegistry tokenRegistry = tokenRegistryRepository.findByToken(token);
        return !tokenRegistry.isActive();
    }

    @Scheduled(fixedRate = 60000)
    public void deleteExpiredTokens() {
        log.info("Running scheduled token cleanup");
        tokenRegistryRepository.deleteByExpiryDate(new Date());
    }

    public void saveTokenRegistry(TokenRegistry tokenRegistry) {
        log.info("Saving token registry entry for username: {}", tokenRegistry.getUsername());
        tokenRegistryRepository.save(tokenRegistry);
    }

    public void invalidatePreviousTokens(String username) {
        log.info("Invalidating all previous tokens for username: {}", username);
        List<TokenRegistry> tokensToInvalidate = tokenRegistryRepository.findAllByUsername(username);
        for (TokenRegistry tokenRegistry : tokensToInvalidate) {
            tokenRegistry.setActive(false);
            tokenRegistryRepository.save(tokenRegistry);
        }
    }
}
