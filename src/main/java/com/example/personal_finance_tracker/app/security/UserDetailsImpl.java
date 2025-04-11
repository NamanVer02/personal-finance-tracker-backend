package com.example.personal_finance_tracker.app.security;

import com.example.personal_finance_tracker.app.models.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Getter
public class UserDetailsImpl implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(User user) {
        log.debug("Building UserDetailsImpl for user: {}", user.getUsername());
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities);
    }

    @Override
    public boolean equals(Object o) {
        log.trace("Performing equality check for UserDetailsImpl objects");
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }

    // Required UserDetails method implementations with logging (if needed)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        log.trace("Retrieving authorities");
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        log.trace("Checking account expiration status");
        return true; // Modify based on your requirements
    }

    @Override
    public boolean isAccountNonLocked() {
        log.trace("Checking account lock status");
        return true; // Modify based on your requirements
    }

    @Override
    public boolean isCredentialsNonExpired() {
        log.trace("Checking credentials expiration status");
        return true; // Modify based on your requirements
    }

    @Override
    public boolean isEnabled() {
        log.trace("Checking account enabled status");
        return true; // Modify based on your requirements
    }
}
