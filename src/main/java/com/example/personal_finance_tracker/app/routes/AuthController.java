package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.dto.JwtResponse;
import com.example.personal_finance_tracker.app.models.dto.LoginRequest;
import com.example.personal_finance_tracker.app.models.dto.MessageResponse;
import com.example.personal_finance_tracker.app.models.dto.SignUpRequest;
import com.example.personal_finance_tracker.app.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
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
        MessageResponse messageResponse = authService.registerUser(signupRequest);
        return ResponseEntity.ok(messageResponse);
    }
}
