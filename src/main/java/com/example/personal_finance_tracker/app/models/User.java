package com.example.personal_finance_tracker.app.models;

import com.example.personal_finance_tracker.app.annotations.Encode;
import com.example.personal_finance_tracker.app.annotations.Loggable;
import com.example.personal_finance_tracker.app.config.StringEncodeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Loggable
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores and hyphens")
    @Encode
    @Convert(converter = StringEncodeConverter.class)
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$", 
             message = "Password must contain at least one digit, one lowercase, one uppercase letter, and one special character")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Encode
    @Convert(converter = StringEncodeConverter.class)
    @Column(nullable = false, unique = true)
    private String email;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    private boolean twoFactorEnabled = false;

    @Encode
    @Convert(converter = StringEncodeConverter.class)
    private String twoFactorSecret;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<FinanceEntry> financeEntries = new ArrayList<>();

    @Version
    private Long version;

    private int failedAttempts;
    private LocalDateTime lockTime;
    private LocalDateTime lastLoginDate;
    private boolean expired = false;
    private LocalDateTime expirationDate;

    @Column(columnDefinition = "LONGTEXT")
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String profileImage;

    public void updateLastLoginDate() {
        this.lastLoginDate = LocalDateTime.now();
    }

    public boolean isAccountExpired() {
        if (expired) {
            return true;
        }
        
        if (expirationDate != null) {
            return LocalDateTime.now().isAfter(expirationDate);
        }
        
        return false;
    }
    
    public void setAccountExpired(boolean expired) {
        this.expired = expired;
        if (expired) {
            // Set expiration date to 7 days from now
            this.expirationDate = LocalDateTime.now().plusDays(7);
        } else {
            this.expirationDate = null;
        }
    }

    public boolean isAccountNonLocked() {
        if (lockTime == null) {
            return true;
        }
        LocalDateTime unlockTime = lockTime.plusMinutes(1);
        return LocalDateTime.now().isAfter(unlockTime);
    }
}
