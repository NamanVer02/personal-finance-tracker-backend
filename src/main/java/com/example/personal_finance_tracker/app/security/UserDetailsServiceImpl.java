package com.example.personal_finance_tracker.app.security;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Attempting to load user details for username: {}", username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User Not Found with username: " + username);
                });

        if (!user.isAccountNonLocked()) {
            log.warn("Account locked for username: {}", username);
            throw new LockedException("Account is locked. Please try again after 10 minutes.");
        }

        log.info("Successfully loaded user details for username: {}", username);
        return UserDetailsImpl.build(user);
    }
}
