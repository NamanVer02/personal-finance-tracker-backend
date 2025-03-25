package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.BlacklistedToken;
import com.example.personal_finance_tracker.app.repository.BlacklistedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class BlackListedTokenService {
    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    public void blacklistToken(String token, Date expiryDate) {
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setExpiryDate(expiryDate);
        blacklistedTokenRepository.save(blacklistedToken);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupExpiredTokens() {
        blacklistedTokenRepository.deleteByExpiryDate(new Date());
    }
}