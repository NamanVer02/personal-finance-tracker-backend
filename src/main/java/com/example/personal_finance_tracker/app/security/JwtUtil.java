package com.example.personal_finance_tracker.app.security;

import com.example.personal_finance_tracker.app.models.TokenRegistry;
import com.example.personal_finance_tracker.app.services.TokenRegistryService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private TokenRegistryService tokenRegistryService;

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
        } catch (JwtException e) {
            logger.error("Error extracting username from token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(authToken);

            if (tokenRegistryService.isTokenBlacklisted(authToken)) {
                logger.warn("Blacklisted token attempt: {}", authToken);
                return false;
            }

            logger.debug("Valid JWT token for user: {}", claims.getBody().getSubject());
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Malformed JWT: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("Expired JWT: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Empty JWT claims: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected JWT error: {}", e.getMessage(), e);
        }
        return false;
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
        } catch (JwtException e) {
            logger.error("Error extracting expiration date: {}", e.getMessage());
            throw e;
        }
    }
}
