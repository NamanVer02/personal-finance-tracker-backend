package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.dto.*;
import com.example.personal_finance_tracker.app.services.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Authentication attempt for username: {}", loginRequest.getUsername());
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        log.info("Successful authentication for username: {}", loginRequest.getUsername());
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signupRequest) {
        log.info("Registration attempt for username: {}", signupRequest.getUsername());
        SignupResponse messageResponse = authService.registerUser(signupRequest);
        log.info("Successful registration for username: {}", signupRequest.getUsername());
        return ResponseEntity.ok(messageResponse);
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setup2FA(@RequestParam String username) {
        log.info("2FA setup request for username: {}", username);
        TwoFactorSetupResponse setupResponse = authService.setup2FA(username);
        log.info("2FA setup completed for username: {}", username);
        return ResponseEntity.ok(setupResponse);
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify2FA(@RequestBody TwoFactorVerifyRequest verifyRequest) {
        log.info("2FA verification attempt for username: {}", verifyRequest.getUsername());
        JwtResponse jwtResponse = authService.verify2FA(verifyRequest);
        log.info("Successful 2FA verification for username: {}", verifyRequest.getUsername());
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String token) {
        String jwtToken = token.substring(7);
        log.info("Logout request received for token (truncated): {}...", jwtToken.substring(0, Math.min(10, jwtToken.length())));
        authService.logout(jwtToken);
        log.info("Successful logout for token (truncated): {}...", jwtToken.substring(0, Math.min(10, jwtToken.length())));
        return ResponseEntity.ok(new MessageResponse("Successfully logged out"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        log.info("Token refresh request received");
        TokenRefreshResponse response = authService.refreshToken(request.getRefreshToken());
        log.info("Token refresh completed successfully");
        return ResponseEntity.ok(response);
    }
}
