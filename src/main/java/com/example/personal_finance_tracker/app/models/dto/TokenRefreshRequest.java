package com.example.personal_finance_tracker.app.models.dto;

import lombok.Data;

@Data
public class TokenRefreshRequest {
    private String refreshToken;
}
