package com.petsapp.config;

import com.petsapp.auth.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Konfiguracja Spring Security dla Kroku 2.
 *
 * <p>
 * Architektura stateless JWT: brak sesji HTTP, brak CSRF (cookies nie sa
 * uzywane).
 * JwtFilter weryfikuje token przy kazdym zadaniu przed wykonaniem logiki
 * endpointu.
 * BCrypt cost=12: dostateczna odpornosc na brute-force przy akceptowalnym
 * czasie
 * (~300ms na nowoczesnym CPU) — celowe spowalnianie jest wadza z perspektywy
 * UX, ale
 * koniecznoscia z perspektywy bezpieczenstwa hasel.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/health",
            "/health/ready",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs",
            "/api-docs/**",
            "/auth/register",
            "/auth/verify-email",
            "/auth/resend-verification",
            "/auth/login",
            "/auth/refresh"
    };

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(PUBLIC_ENDPOINTS)
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt z cost=12. Uzywany przez AuthService do hashowania hasel przy
     * rejestracji i weryfikacji
     * przy logowaniu. Cost 12 jest standardem produkcyjnym (2024).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationManager potrzebny przez Spring Security do wewnetrznej
     * weryfikacji.
     * W naszym flow JWT nie uzywamy DaoAuthenticationProvider bezposrednio w
     * kontrolerze,
     * ale bean jest wymagany przez framework.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
