package com.example.personal_finance_tracker.app.scheduled;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.TokenRegistryRepository;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class CleanupSchedules {

    private final UserRepo userRepository;
    private final TokenRegistryRepository blacklistedTokenRepo;

    public CleanupSchedules (UserRepo userRepository, TokenRegistryRepository blacklistedTokenRepo) {
        this.userRepository = userRepository;
        this.blacklistedTokenRepo = blacklistedTokenRepo;
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void unlockAccounts() {
        log.info("Starting account unlock scheduler");
        List<User> lockedUsers = userRepository.findAllByLockTimeIsNotNull();

        if (lockedUsers.isEmpty()) {
            log.debug("No locked accounts found");
            return;
        }

        int unlockedCount = 0;
        for (User user : lockedUsers) {
            if (user.isAccountNonLocked()) {
                log.info("Unlocking account for user: {}", user.getUsername());
                user.setFailedAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
                unlockedCount++;
            }
        }
        log.info("Unlocked {} accounts", unlockedCount);
    }

    @Scheduled(fixedRate = 60000)
    public void removeExpiredAccounts() {
        log.info("Starting expired account cleanup");
        LocalDateTime expirationThreshold = LocalDateTime.now().minusDays(30);
        List<User> expiredAccounts = userRepository.findExpiredAccounts(expirationThreshold);

        if (expiredAccounts.isEmpty()) {
            log.debug("No expired accounts found");
            return;
        }

        log.info("Deleting {} expired accounts", expiredAccounts.size());
        userRepository.deleteAll(expiredAccounts);
        log.info("Successfully deleted expired accounts");
    }

    @Scheduled(fixedRate = 60000)
    public void deleteExpiredTokens() {
        log.info("Starting expired token cleanup");
        Date currentDate = new Date();
        blacklistedTokenRepo.deleteByExpiryDate(currentDate);

        log.info("Blacklisted token deleted.");
    }
}
