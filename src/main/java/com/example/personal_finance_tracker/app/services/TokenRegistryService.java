package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.TokenRegistry;
import com.example.personal_finance_tracker.app.repository.TokenRegistryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TokenRegistryService {
    @Autowired
    private TokenRegistryRepository tokenRegistryRepository;

    public void blacklistToken(String token, Date expiryDate) {
        TokenRegistry tokenRegistry = tokenRegistryRepository.findByToken(token);
        tokenRegistry.setExpiryDate(expiryDate);
        tokenRegistry.setActive(false);
        tokenRegistryRepository.save(tokenRegistry);
    }

    public boolean isTokenBlacklisted(String token) {
        TokenRegistry tokenRegistry = tokenRegistryRepository.findByToken(token);
        return !tokenRegistry.isActive();
    }

    @Scheduled(fixedRate = 60000)
    public void deleteExpiredTokens() {
        tokenRegistryRepository.deleteByExpiryDate(new Date());
    }

    public void saveTokenRegistry(TokenRegistry tokenRegistry) {
        tokenRegistryRepository.save(tokenRegistry);
    }

    public void invalidatePreviousTokens(String username) {
        List<TokenRegistry> tokensToInvalidate = tokenRegistryRepository.findAllByUsername(username);
        for (TokenRegistry tokenRegistry : tokensToInvalidate) {
            tokenRegistry.setActive(false);
            tokenRegistryRepository.save(tokenRegistry);
        }
    }
}