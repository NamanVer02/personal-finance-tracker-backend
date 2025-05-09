package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.exceptions.JwtAuthenticationException;
import com.example.personal_finance_tracker.app.exceptions.ResourceNotFoundException;
import com.example.personal_finance_tracker.app.exceptions.ValidationException;
import com.example.personal_finance_tracker.app.exceptions.RateLimitExceededException;
import com.example.personal_finance_tracker.app.interfaces.AuthServiceInterface;
import com.example.personal_finance_tracker.app.models.ERole;
import com.example.personal_finance_tracker.app.models.Role;
import com.example.personal_finance_tracker.app.models.TokenRegistry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.*;
import com.example.personal_finance_tracker.app.repository.TokenRegistryRepository;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import com.example.personal_finance_tracker.app.security.JwtUtil;
import com.example.personal_finance_tracker.app.security.UserDetailsImpl;
import com.warrenstrange.googleauth.GoogleAuthenticatorException;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.*;
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

@Slf4j
@Service
public class AuthService implements AuthServiceInterface {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtils;
    private final UserRepo userRepo;
    private final GAService gaService;
    private final TokenRegistryRepository tokenRegistryRepository;
    private final TokenRegistryService blacklistedTokenRegistryService;
    private final TokenRegistryService tokenRegistryService;
    private final EmailService emailService;

    public AuthService(AuthenticationManager authenticationManager, UserService userService, RoleService roleService, PasswordEncoder encoder, JwtUtil jwtUtils, UserRepo userRepo, GAService gaService, TokenRegistryRepository tokenRegistryRepository, TokenRegistryService blacklistedTokenRegistryService, TokenRegistryService tokenRegistryService, EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.roleService = roleService;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.userRepo = userRepo;
        this.gaService = gaService;
        this.tokenRegistryRepository = tokenRegistryRepository;
        this.blacklistedTokenRegistryService = blacklistedTokenRegistryService;
        this.tokenRegistryService = tokenRegistryService;
        this.emailService = emailService;
    }

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());

        try {
            User user = findAndValidateUser(loginRequest.getUsername());
            Authentication authentication = performAuthentication(loginRequest);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            validateUserLockStatus(user);

            if (user.isTwoFactorEnabled()) {
                return handleTwoFactorAuthentication(loginRequest, user, authentication, userDetails);
            } else {
                return handleStandardAuthentication(user, authentication, userDetails);
            }
        } catch (LockedException | BadCredentialsException e) {
            // These are already handled and should be propagated
            throw e;
        } catch (UsernameNotFoundException e) {
            log.error("User not found during authentication: {}", loginRequest.getUsername(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during authentication for user: {}", loginRequest.getUsername(), e);
            throw new AuthenticationServiceException("Authentication failed due to server error", e);
        }
    }

    private User findAndValidateUser(String username) {
        User user = userRepo.findByUsername(username).orElse(null);

        // Check if account is expired before authentication
        if (user != null && user.isAccountExpired()) {
            log.warn("Account is expired for user: {}", username);
            throw new BadCredentialsException("Your account has expired. Please contact the admin to reactivate your account.");
        }

        if (user != null && user.getFailedAttempts() > 5) {
            userService.lockUser(user);
            throw new LockedException("Your account is locked for 10mins for user: " + username);
        }

        return user;
    }

    private Authentication performAuthentication(LoginRequest loginRequest) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));
        } catch (BadCredentialsException e) {
            handleFailedAuthentication(loginRequest.getUsername());
            throw e;
        }
    }

    private void handleFailedAuthentication(String username) {
        log.warn("Authentication failed due to bad credentials for user: {}", username);
        User user = userRepo.findByUsername(username).orElse(null);

        if (user != null) {
            log.info("Incrementing failed login attempts for user: {}", username);
            try {
                userService.incrementFailedAttempts(user);

                if (userService.isMaxFailedAttemptsReached(user)) {
                    log.warn("Max failed attempts reached, locking account for user: {}", username);
                    userService.lockUser(user);
                }
            } catch (Exception ex) {
                log.error("Error handling failed login attempt for user: {}", username, ex);
                // Continue with throwing the original exception
            }
        }
    }

    private void validateUserLockStatus(User user) {
        if (user != null && !user.isAccountNonLocked()) {
            log.warn("Account is locked for user: {}", user.getUsername());
            throw new LockedException("Account is locked. Please try again after 10 minutes.");
        }
    }

    private JwtResponse handleTwoFactorAuthentication(LoginRequest loginRequest, User user,
                                                      Authentication authentication, UserDetailsImpl userDetails) {
        // If 2FA code is provided, verify it
        if (loginRequest.getTwoFactorCode() != null) {
            return verifyTwoFactorAndGenerateResponse(loginRequest, user, authentication, userDetails);
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
    }

    private JwtResponse verifyTwoFactorAndGenerateResponse(LoginRequest loginRequest, User user,
                                                           Authentication authentication, UserDetailsImpl userDetails) {
        log.info("Verifying 2FA code for user: {}", loginRequest.getUsername());
        boolean isValid = gaService.isValid(user.getTwoFactorSecret(), loginRequest.getTwoFactorCode());

        if (!isValid) {
            log.warn("Invalid 2FA code provided for user: {}", loginRequest.getUsername());
            throw new BadCredentialsException("Invalid 2FA code");
        }

        // 2FA successful, set authentication and generate JWT
        log.info("2FA verification successful for user: {}. Invalidating previous tokens.", loginRequest.getUsername());
        invalidatePreviousTokens(user.getUsername());

        return generateAuthenticationResponse(user, authentication, userDetails);
    }

    private JwtResponse handleStandardAuthentication(User user, Authentication authentication, UserDetailsImpl userDetails) {
        // No 2FA required, process normally
        log.info("2FA not enabled for user: {}. Invalidating previous tokens.", user.getUsername());
        invalidatePreviousTokens(user.getUsername());

        return generateAuthenticationResponse(user, authentication, userDetails);
    }

    private void invalidatePreviousTokens(String username) {
        try {
            tokenRegistryService.invalidatePreviousTokens(username);
        } catch (Exception e) {
            log.warn("Failed to invalidate previous tokens for user: {}", username, e);
            // Continue with authentication despite token invalidation failure
        }
    }

    private JwtResponse generateAuthenticationResponse(User user, Authentication authentication, UserDetailsImpl userDetails) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtUtils.generateJwtToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        updateUserLoginData(user);

        JwtResponse response = new JwtResponse(
                accessToken,
                refreshToken,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles,
                false,
                user.getProfileImage());
        log.info("Authentication successful, returning JWT response for user: {}", user.getUsername());
        return response;
    }

    private void updateUserLoginData(User user) {
        try {
            log.info("Resetting failed login attempts for user: {}", user.getUsername());
            userService.resetFailedAttempts(user.getUsername());
            log.info("Updating last login date for user: {}", user.getUsername());
            user.updateLastLoginDate();
            userRepo.save(user);
        } catch (DataAccessException e) {
            log.warn("Failed to update user login data for: {}", user.getUsername(), e);
            // Continue with authentication despite update failure
        }
    }


    @Override
    public SignupResponse registerUser(SignUpRequest signupRequest, String base64Image) {
        log.info("Registering new user with username: {}", signupRequest.getUsername());
        try {
            validateUserInput(signupRequest);
            checkUserExistence(signupRequest);

            User user = createUserObject(signupRequest, base64Image);
            assignUserRoles(user, signupRequest.getRoles());

            return setupTwoFactorAndSaveUser(user);
        } catch (DataAccessException e) {
            log.error("Database error during user registration: {}", signupRequest.getUsername(), e);
            throw new ResourceNotFoundException("Database error during registration");
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions that we've already created
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during user registration: {}", signupRequest.getUsername(), e);
            throw new JwtAuthenticationException("Registration failed due to server error", e);
        }
    }

    private void validateUserInput(SignUpRequest signupRequest) {
        validateUsername(signupRequest.getUsername());
        validateEmail(signupRequest.getEmail());
        validatePassword(signupRequest.getPassword(), signupRequest.getUsername());
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().length() < 3) {
            log.warn("Registration failed: Username too short '{}'", username);
            throw new ValidationException("Username must be at least 3 characters long");
        }
        if (!username.matches("^\\w+$")) {
            log.warn("Registration failed: Username invalid '{}'", username);
            throw new ValidationException("Username can only contain letters, numbers, and underscores");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            log.warn("Registration failed: Email invalid '{}'", email);
            throw new ValidationException("Please enter a valid email address");
        }
    }

    private void validatePassword(String password, String username) {
        if (password == null || password.length() < 8) {
            log.warn("Registration failed: Password too short for '{}'", username);
            throw new ValidationException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            log.warn("Registration failed: Password missing uppercase for '{}'", username);
            throw new ValidationException("Must contain one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            log.warn("Registration failed: Password missing lowercase for '{}'", username);
            throw new ValidationException("Must contain one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            log.warn("Registration failed: Password missing number for '{}'", username);
            throw new ValidationException("Must contain one number");
        }
        if (!password.matches(".*[!@#$%^&*].*")) {
            log.warn("Registration failed: Password missing special character for '{}'", username);
            throw new ValidationException("Must contain one special character (!@#$%^&*)");
        }
    }

    private void checkUserExistence(SignUpRequest signupRequest) {
        boolean existsByUsername = userRepo.existsByUsername(signupRequest.getUsername());
        boolean existsByEmail = userRepo.existsByEmail(signupRequest.getEmail());

        if (existsByUsername) {
            log.warn("Registration failed: Username already taken for username: {}", signupRequest.getUsername());
            throw new ValidationException("Error: Username is already taken!");
        }

        if (existsByEmail) {
            log.warn("Registration failed: Email already in use for email: {}", signupRequest.getEmail());
            throw new ValidationException("Error: Email is already in use!");
        }
    }

    private User createUserObject(SignUpRequest signupRequest, String base64Image) {
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(encoder.encode(signupRequest.getPassword()));
        user.setProfileImage(base64Image); // Set profile image
        return user;
    }

    private void assignUserRoles(User user, Set<String> strRoles) {
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            log.info("No roles provided, assigning default role 'user' to: {}", user.getUsername());
            Role userRole = findRoleByName(ERole.ROLE_USER, user.getUsername());
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                try {
                    if ("admin".equals(role)) {
                        log.info("Assigning 'admin' role to user: {}", user.getUsername());
                        Role adminRole = findRoleByName(ERole.ROLE_ADMIN, user.getUsername());
                        roles.add(adminRole);
                    } else {
                        log.info("Assigning 'user' role to user: {}", user.getUsername());
                        Role userRole = findRoleByName(ERole.ROLE_USER, user.getUsername());
                        roles.add(userRole);
                    }
                } catch (Exception e) {
                    log.error("Error assigning role '{}' to user: {}", role, user.getUsername(), e);
                    throw new ResourceNotFoundException("Error assigning roles");
                }
            });
        }

        user.setRoles(roles);
    }

    private Role findRoleByName(ERole roleName, String username) {
        return roleService.findByName(roleName)
                .orElseThrow(() -> {
                    log.error("Role '{}' not found during registration for: {}", roleName.name(), username);
                    return new ResourceNotFoundException("Error: Role is not found.");
                });
    }

    private SignupResponse setupTwoFactorAndSaveUser(User user) {
        try {
            log.info("Generating 2FA secret for user: {}", user.getUsername());
            String secret = gaService.generateKey();
            user.setTwoFactorSecret(secret);
            user.setTwoFactorEnabled(true);

            log.info("Saving user data for user: {}", user.getUsername());
            userService.save(user);

            // Generate QR code
            log.info("Generating QR code for user: {}", user.getUsername());
            String qrCodeBase64 = gaService.generateQRUrl(secret, user.getUsername());
            
            // Send 2FA setup email
            try {
                log.info("Sending 2FA setup email to user: {}", user.getUsername());
                emailService.send2FASetupEmail(user.getEmail(), user.getUsername(), secret, qrCodeBase64);
            } catch (Exception e) {
                // Log but don't rethrow any email-related exceptions
                log.warn("Email sending failed, but continuing with registration: {}", e.getMessage());
            }

            return createSignupResponse(secret, qrCodeBase64);
        } catch (Exception e) {
            log.error("Error during 2FA setup for user: {}", user.getUsername(), e);
            throw new GoogleAuthenticatorException("Error setting up 2FA during registration", e);
        }
    }

    private SignupResponse createSignupResponse(String secret, String qrCodeBase64) {
        TwoFactorSetupResponse setupResponse = new TwoFactorSetupResponse();
        setupResponse.setSecret(secret);
        setupResponse.setQrCodeBase64(qrCodeBase64);

        SignupResponse response = new SignupResponse();
        response.setMessage("User registered successfully!");
        response.setTwoFactorSetup(setupResponse);

        return response;
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
            throw e;
        }  catch (Exception e) {
            log.error("Unexpected error during 2FA setup for user: {}", username, e);
            throw new GoogleAuthenticatorException("2FA setup failed due to server error", e);
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
                    .toList();

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
            throw new GoogleAuthenticatorException("Database error during 2FA verification", e);
        } catch (Exception e) {
            log.error("Unexpected error during 2FA verification for user: {}", verifyRequest.getUsername(), e);
            throw new GoogleAuthenticatorException("2FA verification failed due to server error", e);
        }
    }

    public TokenRefreshResponse refreshToken(String refreshToken) {
        log.info("Attempting to refresh token");
        try {
            // Validate refresh token
            if (!jwtUtils.validateJwtToken(refreshToken) || isTokenBlacklisted(refreshToken)) {
                log.warn("Invalid refresh token");
                throw new JwtAuthenticationException("Invalid refresh token");
            }

            String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
            log.info("Extracted username {} from refresh token", username);

            // Generate new access token
            String newAccessToken = jwtUtils.generateJwtToken(username);
            // Generate new refresh token
            String newRefreshToken = jwtUtils.generateRefreshToken(username);

            // Attempt to blacklist the old token but continue if it fails
            blacklistTokenSafely(refreshToken);

            TokenRefreshResponse response = new TokenRefreshResponse(newAccessToken, newRefreshToken, "Bearer");
            log.info("Successfully generated new access and refresh tokens");
            return response;
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions we've already created
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            throw new JwtAuthenticationException("Token refresh failed due to server error", e);
        }
    }

    private void blacklistTokenSafely(String token) {
        try {
            blacklistToken(token);
        } catch (Exception e) {
            log.warn("Failed to blacklist old refresh token", e);
            // Continue despite blacklisting failure
        }
    }


    public void logout(String token) {
        log.info("Logging out user with token: {}", token);
        try {
            Date expiryDate = jwtUtils.getExpirationDateFromJwtToken(token);
            blacklistedTokenRegistryService.blacklistToken(token, expiryDate);
        } catch (Exception e) {
            log.error("Database error during logout", e);
            throw new ResourceNotFoundException("Database error during logout");
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
        } catch (Exception e) {
            log.error("Unexpected error while blacklisting token", e);
            throw new ResourceNotFoundException("Failed to blacklist token");
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
