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
    private BlacklistedTokenRepository blacklistedTokenRepo;

    public void blacklistToken(String token, Date expiryDate) {
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setExpiryDate(expiryDate);
        blacklistedTokenRepo.save(blacklistedToken);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepo.existsByToken(token);
    }

    @Scheduled(fixedRate = 60000)
    public void deleteExpiredTokens() {
        blacklistedTokenRepo.deleteByExpiryDate(new Date());
    }
}