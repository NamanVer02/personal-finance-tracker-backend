package com.example.personal_finance_tracker.app.interfaces;

import com.example.personal_finance_tracker.app.models.dto.JwtResponse;
import com.example.personal_finance_tracker.app.models.dto.LoginRequest;
import com.example.personal_finance_tracker.app.models.dto.MessageResponse;
import com.example.personal_finance_tracker.app.models.dto.SignUpRequest;

public interface AuthServiceInterface {
    JwtResponse authenticateUser(LoginRequest loginRequest);
    MessageResponse registerUser(SignUpRequest signupRequest);
}
