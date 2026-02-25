package com.petsapp.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtr JWT wykonywany raz per zadanie HTTP.
 *
 * <p>
 * Czyta naglowek Authorization: Bearer {token}, weryfikuje token JWT i ustawia
 * uwierzytelniony
 * principal w SecurityContextHolder. Nie rzuca wyjatkow — bledne tokeny sa
 * ignorowane (Spring
 * Security odrzuci pozniej chronione zasoby przez 401).
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public JwtFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token != null) {
            authenticateFromToken(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void authenticateFromToken(String token, HttpServletRequest request) {
        try {
            if (!jwtProvider.isTokenValid(token)) {
                return;
            }

            UUID userId = jwtProvider.extractUserId(token);
            userRepository
                    .findById(userId)
                    .filter(user -> user.getDeletedAt() == null && user.isEmailVerified())
                    .ifPresent(
                            user -> {
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                                authentication.setDetails(
                                        new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            });

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT authentication failed for request {}: {}", request.getRequestURI(), e.getMessage());
        }
    }
}
