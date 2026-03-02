package com.petsapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsapp.auth.JwtFilter;
import com.petsapp.common.ApiResponse;
import com.petsapp.common.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
 * <p>Architektura stateless JWT: brak sesji HTTP, brak CSRF (cookies nie sa uzywane). JwtFilter
 * weryfikuje token przy kazdym zadaniu przed wykonaniem logiki endpointu. BCrypt cost=12:
 * dostateczna odpornosc na brute-force przy akceptowalnym czasie (~300ms na nowoczesnym CPU) —
 * celowe spowalnianie jest wadza z perspektywy UX, ale koniecznoscia z perspektywy bezpieczenstwa
 * hasel.
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
    "/auth/refresh",
    "/auth/forgot-password",
    "/auth/reset-password",
    "/auth/google",
    "/users/search",
    "/breeds",
    "/breeds/**",
    "/achievements"
  };

  private final JwtFilter jwtFilter;
  private final ObjectMapper objectMapper;

  public SecurityConfig(JwtFilter jwtFilter, ObjectMapper objectMapper) {
    this.jwtFilter = jwtFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll().anyRequest().authenticated())
        .exceptionHandling(
            ex ->
                ex
                    // Brak tokena lub nie-parsable token -> 401
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          objectMapper.writeValue(
                              response.getWriter(),
                              ApiResponse.error(
                                  ApiResponse.ErrorDetail.of(
                                      ErrorCode.UNAUTHORIZED, "Authentication required")));
                        })
                    // Zalogowany, ale brak wymaganych uprawnien -> 403
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          objectMapper.writeValue(
                              response.getWriter(),
                              ApiResponse.error(
                                  ApiResponse.ErrorDetail.of(
                                      ErrorCode.FORBIDDEN, "Access denied")));
                        }))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * BCrypt z cost=12. Uzywany przez AuthService do hashowania hasel przy rejestracji i weryfikacji
   * przy logowaniu. Cost 12 jest standardem produkcyjnym (2024).
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  /**
   * AuthenticationManager potrzebny przez Spring Security do wewnetrznej weryfikacji. W naszym flow
   * JWT nie uzywamy DaoAuthenticationProvider bezposrednio w kontrolerze, ale bean jest wymagany
   * przez framework.
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }
}
