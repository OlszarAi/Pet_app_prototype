package com.petsapp.auth;

import com.petsapp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpointy uwierzytelniania — rejestracja, weryfikacja emaila, logowanie, refresh, logout.
 *
 * <p>Controller jest odpowiedzialny wylacznie za routing i walidacje DTO. Cala logika biznesowa
 * jest w AuthService. IP klienta jest uzywane do rate limitingu.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Rejestracja, logowanie i zarzadzanie sesjami")
public class AuthController {

  private final AuthService authService;
  private final RateLimitService rateLimitService;

  public AuthController(AuthService authService, RateLimitService rateLimitService) {
    this.authService = authService;
    this.rateLimitService = rateLimitService;
  }

  @PostMapping("/register")
  @Operation(summary = "Rejestracja nowego uzytkownika emailem")
  public ResponseEntity<ApiResponse<Map<String, String>>> register(
      @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
    rateLimitService.checkRegisterRateLimit(getClientIp(httpRequest));
    authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.ok(
                Map.of(
                    "message",
                    "Registration successful. Please check your email for the verification code.")));
  }

  @PostMapping("/verify-email")
  @Operation(summary = "Weryfikacja emaila kodem z wiadomosci")
  public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
      @Valid @RequestBody VerifyEmailRequest request) {
    AuthResponse tokens = authService.verifyEmail(request);
    return ResponseEntity.ok(ApiResponse.ok(tokens));
  }

  @PostMapping("/resend-verification")
  @Operation(summary = "Ponowne wyslanie kodu weryfikacyjnego")
  public ResponseEntity<ApiResponse<Map<String, String>>> resendVerification(
      @Valid @RequestBody ResendVerificationRequest request, HttpServletRequest httpRequest) {
    rateLimitService.checkResendRateLimit(getClientIp(httpRequest));
    authService.resendVerification(request);
    return ResponseEntity.ok(
        ApiResponse.ok(Map.of("message", "Verification code resent. Please check your emails.")));
  }

  @PostMapping("/login")
  @Operation(summary = "Logowanie emailem i haslem")
  public ResponseEntity<ApiResponse<AuthResponse>> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    rateLimitService.checkLoginRateLimit(getClientIp(httpRequest));
    AuthResponse tokens = authService.login(request);
    return ResponseEntity.ok(ApiResponse.ok(tokens));
  }

  @PostMapping("/refresh")
  @Operation(summary = "Odswiezenie access tokena przy uzyciu refresh tokena")
  public ResponseEntity<ApiResponse<AuthResponse>> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    AuthResponse tokens = authService.refresh(request);
    return ResponseEntity.ok(ApiResponse.ok(tokens));
  }

  @PostMapping("/logout")
  @Operation(summary = "Wylogowanie — uniewaznienie refresh tokena")
  public ResponseEntity<ApiResponse<Map<String, String>>> logout(
      @Valid @RequestBody LogoutRequest request) {
    authService.logout(request);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Logged out successfully.")));
  }

  @PostMapping("/forgot-password")
  @Operation(
      summary = "Inicjacja resetu hasla — wyslanie emaila z linkiem",
      description =
          "Zawsze zwraca 200 OK — nie informuje czy email istnieje w systemie"
              + " (ochrona przed user enumeration).")
  public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
    rateLimitService.checkForgotPasswordRateLimit(getClientIp(httpRequest));
    authService.forgotPassword(request);
    return ResponseEntity.ok(
        ApiResponse.ok(
            Map.of(
                "message",
                "If that email address is registered, a password reset link has been sent.")));
  }

  @PostMapping("/reset-password")
  @Operation(summary = "Ustawienie nowego hasla z uzyciem tokena z emaila")
  public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok(
        ApiResponse.ok(Map.of("message", "Password has been reset successfully. Please log in.")));
  }

  @PostMapping("/google")
  @Operation(
      summary = "Logowanie lub rejestracja przez Google Sign-In",
      description =
          "Weryfikuje Google ID token. Jesli konto nie istnieje, tworzy je automatycznie.")
  public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
      @Valid @RequestBody GoogleAuthRequest request) {
    AuthResponse tokens = authService.googleLogin(request);
    return ResponseEntity.ok(ApiResponse.ok(tokens));
  }

  /**
   * Wyciaga IP klienta z naglowka X-Forwarded-For (jesli jest proxy) lub bezposrednio z polaczenia.
   *
   * @param request HTTP request
   * @return adres IP klienta
   */
  private String getClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      // X-Forwarded-For moze zawierac lancuch proxy — bierzemy pierwszy (oryginalny
      // klient)
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
