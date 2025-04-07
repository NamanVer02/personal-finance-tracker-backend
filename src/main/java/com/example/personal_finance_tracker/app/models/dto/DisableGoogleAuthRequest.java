package com.example.personal_finance_tracker.app.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisableGoogleAuthRequest {
    private String password;
}
