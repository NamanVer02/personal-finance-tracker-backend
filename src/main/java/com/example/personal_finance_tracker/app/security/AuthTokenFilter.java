package com.example.personal_finance_tracker.app.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Enhanced logging to debug the issue
            log.info("Processing request: {}", request.getRequestURI());

            String headerAuth = request.getHeader("Authorization");
            log.debug("Authorization header present: {}", StringUtils.hasText(headerAuth));

            String jwt = parseJwt(request);
            
            if (jwt != null) {
                try {
                    boolean isValid = jwtUtil.validateJwtToken(jwt);
                    log.debug("JWT validation result: {}", isValid);

                    if (isValid) {
                        String username = jwtUtil.getUserNameFromJwtToken(jwt);
                        log.debug("Username extracted from token: {}", username);

                        try {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities());

                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("Authentication set in SecurityContext for user: {}", username);
                        } catch (UsernameNotFoundException e) {
                            log.error("User not found with username: {}", username, e);
                            // Don't set authentication for non-existent users
                        } catch (LockedException e) {
                            log.error("Account locked for user: {}", username, e);
                            // Don't set authentication for locked accounts
                        }
                    }
                } catch (MalformedJwtException e) {
                    log.error("Invalid JWT token format: {}", e.getMessage());
                } catch (ExpiredJwtException e) {
                    log.warn("JWT token is expired: {}", e.getMessage());
                } catch (UnsupportedJwtException e) {
                    log.error("JWT token is unsupported: {}", e.getMessage());
                } catch (IllegalArgumentException e) {
                    log.error("JWT claims string is empty: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage(), e);
            // We catch the exception but allow the request to proceed to maintain filter chain integrity
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
