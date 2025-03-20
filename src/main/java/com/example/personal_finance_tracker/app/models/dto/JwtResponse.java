package com.example.personal_finance_tracker.app.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private boolean twoFactorRequired = false;

    // Constructor keeping backward compatibility
    public JwtResponse(String accessToken, String refreshToken, Long id, String username, String email, List<String> roles, boolean twoFactorRequired) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.twoFactorRequired = twoFactorRequired;
    }
}
