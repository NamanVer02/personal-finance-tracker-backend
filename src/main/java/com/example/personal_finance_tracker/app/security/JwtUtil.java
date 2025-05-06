package com.example.personal_finance_tracker.app.security;

import com.example.personal_finance_tracker.app.models.TokenRegistry;
import com.example.personal_finance_tracker.app.services.TokenRegistryService;
import io.jsonwebtoken.*;
import com.example.personal_finance_tracker.app.exceptions.JwtAuthenticationException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${app.jwtRefreshExpirationMs}")
    private int jwtRefreshExpirationMs;

    private final TokenRegistryService tokenRegistryService;

    public JwtUtil(TokenRegistryService tokenRegistryService) {
        this.tokenRegistryService = tokenRegistryService;
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        logger.debug("Generating JWT for user: {}", userPrincipal.getUsername());

        String jwt = buildJwtToken(userPrincipal.getUsername(), jwtExpirationMs);
        handleTokenRegistration(userPrincipal.getUsername(), jwt);
        return jwt;
    }

    private String buildJwtToken(String username, long expiration) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private void handleTokenRegistration(String username, String jwt) {
        try {
            tokenRegistryService.invalidatePreviousTokens(username);

            TokenRegistry tokenRegistry = new TokenRegistry();
            tokenRegistry.setToken(jwt);
            tokenRegistry.setExpiryDate(new Date(System.currentTimeMillis() + jwtExpirationMs));
            tokenRegistry.setActive(true);
            tokenRegistry.setUsername(username);

            tokenRegistryService.saveTokenRegistry(tokenRegistry);
            logger.info("Successfully registered new JWT for user: {}", username);
        } catch (Exception e) {
            logger.error("Error registering token for user {}: {}", username, e.getMessage());
        }
    }

    public String generateRefreshToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        logger.debug("Generating refresh token for user: {}", userPrincipal.getUsername());

        return buildRefreshToken(userPrincipal.getUsername());
    }

    private String buildRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getUserNameFromJwtToken(String token) {
        try {
            String username = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            logger.debug("Successfully extracted username from JWT");
            return username;
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("Invalid JWT token format", e);
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("JWT token is expired", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("JWT token is unsupported", e);
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT claims string is empty", e);
        } catch (JwtException e) {
            throw new JwtAuthenticationException("Failed to extract username from JWT token", e);
        }
    }

    public boolean validateJwtToken(String authToken) {
        if (authToken == null || authToken.isEmpty()) {
            logger.error("JWT token is null or empty");
            return false;
        }

        try {
            Jws<Claims> claims = parseJwtToken(authToken);

            if (!isTokenAllowed(authToken)) {
                return false;
            }

            logger.debug("Valid JWT token for user: {}", claims.getBody().getSubject());
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected JWT validation error: {}", e.getMessage(), e);
        }
        return false;
    }

    private Jws<Claims> parseJwtToken(String authToken) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(authToken);
    }

    private boolean isTokenAllowed(String authToken) {
        try {
            if (tokenRegistryService.isTokenBlacklisted(authToken)) {
                logger.warn("Blacklisted token attempt: {}", authToken);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Error checking token blacklist status: {}", e.getMessage(), e);
            // Continue with validation despite blacklist check failure
            return true;
        }
    }


    public String generateJwtToken(String username) {
        logger.debug("Generating JWT for username: {}", username);
        String jwt = buildJwtToken(username, jwtExpirationMs);
        handleTokenRegistration(username, jwt);
        return jwt;
    }

    public String generateRefreshToken(String username) {
        logger.debug("Generating refresh token for username: {}", username);
        return buildRefreshToken(username);
    }

    public Date getExpirationDateFromJwtToken(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();

            logger.debug("Successfully extracted expiration date from JWT");
            return expiration;
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("Invalid JWT token format", e);
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("JWT token is expired", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("JWT token is unsupported", e);
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT claims string is empty", e);
        } catch (JwtException e) {
            throw new JwtAuthenticationException("Failed to extract username from JWT token", e);
        }
    }
}
