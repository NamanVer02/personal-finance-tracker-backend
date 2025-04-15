package com.example.personal_finance_tracker.app.interfaces;

import com.example.personal_finance_tracker.app.models.dto.*;

public interface AuthServiceInterface {
    JwtResponse authenticateUser(LoginRequest loginRequest);
    SignupResponse registerUser(SignUpRequest signupRequest, String base64Image);
    TwoFactorSetupResponse setup2FA(String username);
    JwtResponse verify2FA(TwoFactorVerifyRequest verifyRequest);
}
