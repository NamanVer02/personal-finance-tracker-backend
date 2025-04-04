package com.example.personal_finance_tracker.app.scheduled;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.TokenRegistryRepository;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
public class CleanupSchedules {
    @Autowired
    private UserRepo userRepository;

    @Autowired
    private TokenRegistryRepository blacklistedTokenRepo;

    @Scheduled(fixedRate = 60000) // Run every minute
    public void unlockAccounts() {
        List<User> lockedUsers = userRepository.findAllByLockTimeIsNotNull();

        for (User user : lockedUsers) {
            if (user.isAccountNonLocked()) {
                // Reset failed attempts and lock time for users whose lock period has expired
                user.setFailedAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void removeExpiredAccounts() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusDays(30);
        List<User> expiredAccounts = userRepository.findExpiredAccounts(expirationThreshold);

        userRepository.deleteAll(expiredAccounts);
    }

    @Scheduled(fixedRate = 60000)
    public void deleteExpiredTokens() {
        blacklistedTokenRepo.deleteByExpiryDate(new Date());
    }
}