package com.example.personal_finance_tracker.app.security;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        if (!user.isAccountNonLocked()) {
            throw new LockedException("Account is locked. Please try again after 10 minutes.");
        }

        return UserDetailsImpl.build(user);
    }
}
