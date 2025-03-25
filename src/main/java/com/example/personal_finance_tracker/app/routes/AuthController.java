package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.dto.*;
import com.example.personal_finance_tracker.app.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signupRequest) {
        SignupResponse messageResponse = authService.registerUser(signupRequest);
        return ResponseEntity.ok(messageResponse);
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setup2FA(@RequestParam String username) {
        TwoFactorSetupResponse setupResponse = authService.setup2FA(username);
        return ResponseEntity.ok(setupResponse);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify2FA(@RequestBody TwoFactorVerifyRequest verifyRequest) {
        JwtResponse jwtResponse = authService.verify2FA(verifyRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String token) {
        authService.logout(token.substring(7));
        return ResponseEntity.ok(new MessageResponse("Successfully logged out"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken (@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }


}
