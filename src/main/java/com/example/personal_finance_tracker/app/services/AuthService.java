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
import org.springframework.beans.factory.annotation.Autowired;
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
        User user = userRepo.findByUsername(loginRequest.getUsername()).orElse(null);

        // First authenticate with username and password
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            // Get the user details
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Check if user has 2FA enabled
            if (user != null && !user.isAccountNonLocked()) {
                throw new LockedException("Account is locked. Please try again after 10 minutes.");
            }

            assert user != null;
            if (user.isTwoFactorEnabled()) {
                // If 2FA code is provided, verify it
                if (loginRequest.getTwoFactorCode() != null) {
                    boolean isValid = gaService.isValid(user.getTwoFactorSecret(), loginRequest.getTwoFactorCode());

                    if (!isValid) {
                        throw new BadCredentialsException("Invalid 2FA code");
                    }

                    // 2FA successful, set authentication and generate JWT
                    tokenRegistryService.invalidatePreviousTokens(user.getUsername());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    String accessToken = jwtUtils.generateJwtToken(authentication);
                    String refreshToken = jwtUtils.generateRefreshToken(authentication);

                    List<String> roles = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList());

                    userService.resetFailedAttempts(user.getUsername());
                    user.updateLastLoginDate();
                    userRepo.save(user);

                    return new JwtResponse(
                            accessToken,
                            refreshToken,
                            userDetails.getId(),
                            userDetails.getUsername(),
                            userDetails.getEmail(),
                            roles,
                            false);
                } else {
                    // 2FA code not provided, return response indicating 2FA is required
                    return new JwtResponse(
                            null,
                            null,
                            userDetails.getId(),
                            userDetails.getUsername(),
                            userDetails.getEmail(),
                            null,
                            true);
                }
            } else {
                // No 2FA required, process normally
                tokenRegistryService.invalidatePreviousTokens(user.getUsername());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                String accessToken = jwtUtils.generateJwtToken(authentication);
                String refreshToken = jwtUtils.generateJwtToken(authentication);

                List<String> roles = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());

                userService.resetFailedAttempts(user.getUsername());
                user.updateLastLoginDate();
                userRepo.save(user);

                return new JwtResponse(
                        accessToken,
                        refreshToken,
                        userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getEmail(),
                        roles,
                        false);
            }
        } catch (BadCredentialsException e) {
            if (user != null) {
                userService.incrementFailedAttempts(user);

                if (userService.isMaxFailedAttemptsReached(user)) {
                    userService.lockUser(user);
                }
            }
            throw e;
        }
    }

    @Override
    public SignupResponse registerUser(SignUpRequest signupRequest) {
        if (userService.existsByUsername(signupRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userService.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(encoder.encode(signupRequest.getPassword()));

        Set<String> strRoles = signupRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleService.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleService.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    default:
                        Role userRole = roleService.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);

        // Generate 2FA secret
        String secret = gaService.generateKey();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);

        userService.save(user);

        // Generate QR code
        String qrCodeBase64 = gaService.generateQRUrl(secret, user.getUsername());

        // Create response
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
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Generate new secret
        String secret = gaService.generateKey();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);

        userRepo.save(user);

        // Generate QR code
        String qrCodeBase64 = gaService.generateQRUrl(secret, username);

        TwoFactorSetupResponse response = new TwoFactorSetupResponse();
        response.setSecret(secret);
        response.setQrCodeBase64(qrCodeBase64);

        return response;
    }

    @Override
    public JwtResponse verify2FA(TwoFactorVerifyRequest verifyRequest) {
        User user = userRepo.findByUsername(verifyRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean isValid = gaService.isValid(user.getTwoFactorSecret(), verifyRequest.getCode());

        if (!isValid) {
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

        return new JwtResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles,
                false);
    }

    public TokenRefreshResponse refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtUtils.validateJwtToken(refreshToken) || isTokenBlacklisted(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
        // Generate new access token
        String newAccessToken = jwtUtils.generateJwtToken(username);
        // Generate new refresh token
        String newRefreshToken = jwtUtils.generateRefreshToken(username);

        // Blacklist the old refresh token
        blacklistToken(refreshToken);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken, "Bearer");
    }

    public void logout(String token) {
        Date expiryDate = jwtUtils.getExpirationDateFromJwtToken(token);
        blacklistedTokenRegistryService.blacklistToken(token, expiryDate);
    }

    private void blacklistToken(String token) {
        Date expiryDate = jwtUtils.getExpirationDateFromJwtToken(token);
        TokenRegistry tokenRegistry = new TokenRegistry();
        tokenRegistry.setToken(token);
        tokenRegistry.setExpiryDate(expiryDate);
        tokenRegistryRepository.save(tokenRegistry);
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenRegistryRepository.existsByToken(token);
    }
}