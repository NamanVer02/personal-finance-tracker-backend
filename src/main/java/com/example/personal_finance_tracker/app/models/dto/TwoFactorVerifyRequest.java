package com.example.personal_finance_tracker.app.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwoFactorVerifyRequest {
    private String username;
    private Integer code;
}
