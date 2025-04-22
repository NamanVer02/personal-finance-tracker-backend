package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.AuthServiceInterface;
import com.example.personal_finance_tracker.app.models.TokenRegistry;
import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.*;
import com.example.personal_finance_tracker.app.repository.TokenRegistryRepository;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import com.example.personal_finance_tracker.app.security.JwtUtil;
import com.example.personal_finance_tracker.app.security.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService implements AuthServiceInterface {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtil jwtUtils;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private GAService gaService;

    @Autowired
    private TokenRegistryRepository tokenRegistryRepository;

    @Autowired
    private TokenRegistryService blacklistedTokenRegistryService;
    @Autowired
    private TokenRegistryService tokenRegistryService;

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());
        try {
            User user = userRepo.findByUsername(loginRequest.getUsername()).orElse(null);

            // Check if account is expired before authentication
            if (user != null && user.isAccountExpired()) {
                log.warn("Account is expired for user: {}", loginRequest.getUsername());
                throw new BadCredentialsException("Your account has expired. Please contact the admin to reactivate your account.");
            }

            // First authenticate with username and password
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

                // Get the user details
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

                // Check if user has 2FA enabled
                if (user != null && !user.isAccountNonLocked()) {
                    log.warn("Account is locked for user: {}", loginRequest.getUsername());
                    throw new LockedException("Account is locked. Please try again after 10 minutes.");
                }

                assert user != null;
                if (user.isTwoFactorEnabled()) {
                    // If 2FA code is provided, verify it
                    if (loginRequest.getTwoFactorCode() != null) {
                        log.info("Verifying 2FA code for user: {}", loginRequest.getUsername());
                        boolean isValid = gaService.isValid(user.getTwoFactorSecret(), loginRequest.getTwoFactorCode());

                        if (!isValid) {
                            log.warn("Invalid 2FA code provided for user: {}", loginRequest.getUsername());
                            throw new BadCredentialsException("Invalid 2FA code");
                        }

                        // 2FA successful, set authentication and generate JWT
                        log.info("2FA verification successful for user: {}. Invalidating previous tokens.", loginRequest.getUsername());
                        try {
                            tokenRegistryService.invalidatePreviousTokens(user.getUsername());
                        } catch (Exception e) {
                            log.warn("Failed to invalidate previous tokens for user: {}", loginRequest.getUsername(), e);
                            // Continue with authentication despite token invalidation failure
                        }

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        String accessToken = jwtUtils.generateJwtToken(authentication);
                        String refreshToken = jwtUtils.generateRefreshToken(authentication);

                        List<String> roles = userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toList());

                        try {
                            log.info("Resetting failed login attempts for user: {}", loginRequest.getUsername());
                            userService.resetFailedAttempts(user.getUsername());
                            log.info("Updating last login date for user: {}", loginRequest.getUsername());
                            user.updateLastLoginDate();
                            userRepo.save(user);
                        } catch (DataAccessException e) {
                            log.warn("Failed to update user login data for: {}", loginRequest.getUsername(), e);
                            // Continue with authentication despite update failure
                        }

                        JwtResponse response = new JwtResponse(
                                accessToken,
                                refreshToken,
                                userDetails.getId(),
                                userDetails.getUsername(),
                                userDetails.getEmail(),
                                roles,
                                false,
                                user.getProfileImage());
                        log.info("Authentication successful, returning JWT response for user: {}", loginRequest.getUsername());
                        return response;
                    } else {
                        // 2FA code not provided, return response indicating 2FA is required
                        log.info("2FA code required for user: {}", loginRequest.getUsername());
                        JwtResponse response = new JwtResponse(
                                null,
                                null,
                                userDetails.getId(),
                                userDetails.getUsername(),
                                userDetails.getEmail(),
                                null,
                                true,
                                user.getProfileImage());
                        log.info("Returning 2FA required JWT response for user: {}", loginRequest.getUsername());
                        return response;
                    }
                } else {
                    // No 2FA required, process normally
                    log.info("2FA not enabled for user: {}. Invalidating previous tokens.", loginRequest.getUsername());
                    try {
                        tokenRegistryService.invalidatePreviousTokens(user.getUsername());
                    } catch (Exception e) {
                        log.warn("Failed to invalidate previous tokens for user: {}", loginRequest.getUsername(), e);
                        // Continue with authentication despite token invalidation failure
                    }

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    String accessToken = jwtUtils.generateJwtToken(authentication);
                    String refreshToken = jwtUtils.generateRefreshToken(authentication);

                    List<String> roles = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList());

                    try {
                        log.info("Resetting failed login attempts for user: {}", loginRequest.getUsername());
                        userService.resetFailedAttempts(user.getUsername());
                        log.info("Updating last login date for user: {}", loginRequest.getUsername());
                        user.updateLastLoginDate();
                        userRepo.save(user);
                    } catch (DataAccessException e) {
                        log.warn("Failed to update user login data for: {}", loginRequest.getUsername(), e);
                        // Continue with authentication despite update failure
                    }

                    JwtResponse response = new JwtResponse(
                            accessToken,
                            refreshToken,
                            userDetails.getId(),
                            userDetails.getUsername(),
                            userDetails.getEmail(),
                            roles,
                            false,
                            user.getProfileImage());
                    log.info("Authentication successful, returning JWT response for user: {}", loginRequest.getUsername());
                    return response;
                }
            } catch (BadCredentialsException e) {
                log.warn("Authentication failed due to bad credentials for user: {}", loginRequest.getUsername());
                if (user != null) {
                    log.info("Incrementing failed login attempts for user: {}", loginRequest.getUsername());
                    try {
                        userService.incrementFailedAttempts(user);

                        if (userService.isMaxFailedAttemptsReached(user)) {
                            log.warn("Max failed attempts reached, locking account for user: {}", loginRequest.getUsername());
                            userService.lockUser(user);
                        }
                    } catch (Exception ex) {
                        log.error("Error handling failed login attempt for user: {}", loginRequest.getUsername(), ex);
                        // Continue with throwing the original exception
                    }
                }
                throw e;
            }
        } catch (LockedException | BadCredentialsException e) {
            // These are already handled and should be propagated
            throw e;
        } catch (UsernameNotFoundException e) {
            log.error("User not found during authentication: {}", loginRequest.getUsername(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during authentication for user: {}", loginRequest.getUsername(), e);
            throw new RuntimeException("Authentication failed due to server error", e);
        }
    }

    @Override
    public SignupResponse registerUser(SignUpRequest signupRequest, String base64Image) {
        log.info("Registering new user with username: {}", signupRequest.getUsername());
        try {
            // Username validation
            String username = signupRequest.getUsername();
            if (username == null || username.trim().length() < 3) {
                log.warn("Registration failed: Username too short '{}'", username);
                throw new RuntimeException("Username must be at least 3 characters long");
            }
            if (!username.matches("^[a-zA-Z0-9_]+$")) {
                log.warn("Registration failed: Username invalid '{}'", username);
                throw new RuntimeException("Username can only contain letters, numbers, and underscores");
            }

            // Email validation
            String email = signupRequest.getEmail();
            if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                log.warn("Registration failed: Email invalid '{}'", email);
                throw new RuntimeException("Please enter a valid email address");
            }

            // Password validation
            String password = signupRequest.getPassword();
            if (password == null || password.length() < 8) {
                log.warn("Registration failed: Password too short for '{}'", username);
                throw new RuntimeException("Password must be at least 8 characters");
            }
            if (!password.matches(".*[A-Z].*")) {
                log.warn("Registration failed: Password missing uppercase for '{}'", username);
                throw new RuntimeException("Must contain one uppercase letter");
            }
            if (!password.matches(".*[a-z].*")) {
                log.warn("Registration failed: Password missing lowercase for '{}'", username);
                throw new RuntimeException("Must contain one lowercase letter");
            }
            if (!password.matches(".*\\d.*")) {
                log.warn("Registration failed: Password missing number for '{}'", username);
                throw new RuntimeException("Must contain one number");
            }
            if (!password.matches(".*[!@#$%^&*].*")) {
                log.warn("Registration failed: Password missing special character for '{}'", username);
                throw new RuntimeException("Must contain one special character (!@#$%^&*)");
            }

            if (userService.existsByUsername(signupRequest.getUsername())) {
                log.warn("Registration failed: Username already taken for username: {}", signupRequest.getUsername());
                throw new RuntimeException("Error: Username is already taken!");
            }

            if (userService.existsByEmail(signupRequest.getEmail())) {
                log.warn("Registration failed: Email already in use for email: {}", signupRequest.getEmail());
                throw new RuntimeException("Error: Email is already in use!");
            }

            // Create new user's account
            User user = new User();
            user.setUsername(signupRequest.getUsername());
            user.setEmail(signupRequest.getEmail());
            user.setPassword(encoder.encode(signupRequest.getPassword()));
            user.setProfileImage(base64Image); // Set profile image

            Set<String> strRoles = signupRequest.getRoles();
            Set<Role> roles = new HashSet<>();

            if (strRoles == null) {
                log.info("No roles provided, assigning default role 'user' to: {}", signupRequest.getUsername());
                Role userRole = roleService.findByName(ERole.ROLE_USER)
                        .orElseThrow(() -> {
                            log.error("Role 'user' not found during registration for: {}", signupRequest.getUsername());
                            return new RuntimeException("Error: Role is not found.");
                        });
                roles.add(userRole);
            } else {
                strRoles.forEach(role -> {
                    try {
                        switch (role) {
                            case "admin":
                                log.info("Assigning 'admin' role to user: {}", signupRequest.getUsername());
                                Role adminRole = roleService.findByName(ERole.ROLE_ADMIN)
                                        .orElseThrow(() -> {
                                            log.error("Role 'admin' not found during registration for: {}", signupRequest.getUsername());
                                            return new RuntimeException("Error: Role is not found.");
                                        });
                                roles.add(adminRole);
                                break;
                            default:
                                log.info("Assigning 'user' role to user: {}", signupRequest.getUsername());
                                Role userRole = roleService.findByName(ERole.ROLE_USER)
                                        .orElseThrow(() -> {
                                            log.error("Role 'user' not found during registration for: {}", signupRequest.getUsername());
                                            return new RuntimeException("Error: Role is not found.");
                                        });
                                roles.add(userRole);
                        }
                    } catch (Exception e) {
                        log.error("Error assigning role '{}' to user: {}", role, signupRequest.getUsername(), e);
                        throw new RuntimeException("Error assigning roles", e);
                    }
                });
            }

            user.setRoles(roles);

            // Generate 2FA secret
            try {
                log.info("Generating 2FA secret for user: {}", signupRequest.getUsername());
                String secret = gaService.generateKey();
                user.setTwoFactorSecret(secret);
                user.setTwoFactorEnabled(true);

                log.info("Saving user data for user: {}", signupRequest.getUsername());
                userService.save(user);

                // Generate QR code
                log.info("Generating QR code for user: {}", signupRequest.getUsername());
                String qrCodeBase64 = gaService.generateQRUrl(secret, user.getUsername());

                // Create response
                TwoFactorSetupResponse setupResponse = new TwoFactorSetupResponse();
                setupResponse.setSecret(secret);
                setupResponse.setQrCodeBase64(qrCodeBase64);

                SignupResponse response = new SignupResponse();
                response.setMessage("User registered successfully!");
                response.setTwoFactorSetup(setupResponse);

                log.info("User registration successful for user: {}", signupRequest.getUsername());
                return response;
            } catch (Exception e) {
                log.error("Error during 2FA setup for user: {}", signupRequest.getUsername(), e);
                throw new RuntimeException("Error setting up 2FA during registration", e);
            }
        } catch (DataAccessException e) {
            log.error("Database error during user registration: {}", signupRequest.getUsername(), e);
            throw new RuntimeException("Database error during registration", e);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions that we've already created
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during user registration: {}", signupRequest.getUsername(), e);
            throw new RuntimeException("Registration failed due to server error", e);
        }
    }

    @Override
    public TwoFactorSetupResponse setup2FA(String username) {
        log.info("Setting up 2FA for user: {}", username);
        try {
            User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> {
                        log.error("User not found: {}", username);
                        return new UsernameNotFoundException("User not found");
                    });

            // Generate new secret
            log.info("Generating new 2FA secret for user: {}", username);
            String secret = gaService.generateKey();
            user.setTwoFactorSecret(secret);
            user.setTwoFactorEnabled(true);

            log.info("Saving updated user data for user: {}", username);
            userRepo.save(user);

            // Generate QR code
            log.info("Generating QR code for user: {}", username);
            String qrCodeBase64 = gaService.generateQRUrl(secret, username);

            TwoFactorSetupResponse response = new TwoFactorSetupResponse();
            response.setSecret(secret);
            response.setQrCodeBase64(qrCodeBase64);

            log.info("2FA setup completed for user: {}", username);
            return response;
        } catch (UsernameNotFoundException e) {
            // Re-throw the specific exception
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error during 2FA setup for user: {}", username, e);
            throw new RuntimeException("Database error during 2FA setup", e);
        } catch (Exception e) {
            log.error("Unexpected error during 2FA setup for user: {}", username, e);
            throw new RuntimeException("2FA setup failed due to server error", e);
        }
    }

    @Override
    public JwtResponse verify2FA(TwoFactorVerifyRequest verifyRequest) {
        log.info("Verifying 2FA for user: {}", verifyRequest.getUsername());
        try {
            User user = userRepo.findByUsername(verifyRequest.getUsername())
                    .orElseThrow(() -> {
                        log.error("User not found: {}", verifyRequest.getUsername());
                        return new UsernameNotFoundException("User not found");
                    });

            log.info("Validating 2FA code for user: {}", verifyRequest.getUsername());
            boolean isValid = gaService.isValid(user.getTwoFactorSecret(), verifyRequest.getCode());

            if (!isValid) {
                log.warn("Invalid 2FA code provided for user: {}", verifyRequest.getUsername());
                throw new BadCredentialsException("Invalid 2FA code");
            }

            // Create authentication object
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, userService.getUserAuthorities(user));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String accessToken = jwtUtils.generateJwtToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(authentication);

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            JwtResponse response = new JwtResponse(
                    accessToken,
                    refreshToken,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    roles,
                    false,
                    user.getProfileImage());
            log.info("2FA verification successful, returning JWT response for user: {}", verifyRequest.getUsername());
            return response;
        } catch (UsernameNotFoundException | BadCredentialsException e) {
            // Re-throw specific exceptions
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error during 2FA verification for user: {}", verifyRequest.getUsername(), e);
            throw new RuntimeException("Database error during 2FA verification", e);
        } catch (Exception e) {
            log.error("Unexpected error during 2FA verification for user: {}", verifyRequest.getUsername(), e);
            throw new RuntimeException("2FA verification failed due to server error", e);
        }
    }

    public TokenRefreshResponse refreshToken(String refreshToken) {
        log.info("Attempting to refresh token");
        try {
            // Validate refresh token
            if (!jwtUtils.validateJwtToken(refreshToken) || isTokenBlacklisted(refreshToken)) {
                log.warn("Invalid refresh token");
                throw new RuntimeException("Invalid refresh token");
            }

            String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
            log.info("Extracted username {} from refresh token", username);
            // Generate new access token
            String newAccessToken = jwtUtils.generateJwtToken(username);
            // Generate new refresh token
            String newRefreshToken = jwtUtils.generateRefreshToken(username);

            // Blacklist the old refresh token
            try {
                blacklistToken(refreshToken);
            } catch (Exception e) {
                log.warn("Failed to blacklist old refresh token", e);
                // Continue despite blacklisting failure
            }

            TokenRefreshResponse response = new TokenRefreshResponse(newAccessToken, newRefreshToken, "Bearer");
            log.info("Successfully generated new access and refresh tokens");
            return response;
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions we've already created
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            throw new RuntimeException("Token refresh failed due to server error", e);
        }
    }

    public void logout(String token) {
        log.info("Logging out user with token: {}", token);
        try {
            Date expiryDate = jwtUtils.getExpirationDateFromJwtToken(token);
            blacklistedTokenRegistryService.blacklistToken(token, expiryDate);
            log.info("Token blacklisted successfully");
        } catch (DataAccessException e) {
            log.error("Database error during logout", e);
            throw new RuntimeException("Database error during logout", e);
        } catch (Exception e) {
            log.error("Unexpected error during logout", e);
            throw new RuntimeException("Logout failed due to server error", e);
        }
    }

    private void blacklistToken(String token) {
        log.info("Blacklisting token: {}", token);
        try {
            Date expiryDate = jwtUtils.getExpirationDateFromJwtToken(token);
            TokenRegistry tokenRegistry = new TokenRegistry();
            tokenRegistry.setToken(token);
            tokenRegistry.setExpiryDate(expiryDate);
            tokenRegistryRepository.save(tokenRegistry);
            log.info("Token blacklisted successfully");
        } catch (DataAccessException e) {
            log.error("Database error while blacklisting token", e);
            throw new RuntimeException("Failed to blacklist token due to database error", e);
        } catch (Exception e) {
            log.error("Unexpected error while blacklisting token", e);
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        log.info("Checking if token is blacklisted: {}", token);
        try {
            boolean isBlacklisted = tokenRegistryRepository.existsByToken(token);
            log.info("Token blacklisted status: {}", isBlacklisted);
            return isBlacklisted;
        } catch (DataAccessException e) {
            log.error("Database error while checking token blacklist status", e);
            // In case of error, assume token is not blacklisted to prevent unauthorized access
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while checking token blacklist status", e);
            // In case of error, assume token is not blacklisted to prevent unauthorized access
            return false;
        }
    }
}
