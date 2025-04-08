package com.example.personal_finance_tracker.app.models.dto;

import com.example.personal_finance_tracker.app.models.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsResponse {
    private User user;
    private int totalTransactions;
}
