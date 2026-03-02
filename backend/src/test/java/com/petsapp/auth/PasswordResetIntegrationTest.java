package com.petsapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.petsapp.AbstractIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla flow resetu hasla.
 *
 * <p>Testujemy przez AuthService z prawdziwa baza PostgreSQL (Testcontainers). EmailService jest
 * mockowany — nie chcemy wysylac emaili w testach.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordResetIntegrationTest extends AbstractIntegrationTest {

  @Autowired private AuthService authService;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordResetRepository passwordResetRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private JwtProvider jwtProvider;

  @MockBean private EmailService emailService;

  private static final String TEST_PASSWORD = "SecurePassword123";
  private static final String NEW_PASSWORD = "NewSecurePassword456";

  private String uniqueEmail() {
    return "reset-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueUsername() {
    return "usr" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
  }

  @BeforeEach
  void setupEmailServiceMock() {
    doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  private void mockEmailServiceForCode(String code) {
    org.mockito.Mockito.when(emailService.generateVerificationCode()).thenReturn(code);
    org.mockito.Mockito.when(emailService.verificationCodeExpiry())
        .thenReturn(Instant.now().plusSeconds(900));
  }

  private void mockResetTokenService(String rawToken) {
    org.mockito.Mockito.when(emailService.generateResetToken()).thenReturn(rawToken);
    org.mockito.Mockito.when(emailService.resetTokenExpiry())
        .thenReturn(Instant.now().plusSeconds(3600));
  }

  /** Rejestruje i weryfikuje konto, zwraca email. */
  private String registerAndVerifyUser() {
    // Kod musi miec dokladnie 6 cyfr (ograniczenie VARCHAR(6) w bazie)
    String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
    mockEmailServiceForCode(code);
    String email = uniqueEmail();
    authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));
    authService.verifyEmail(new VerifyEmailRequest(email, code));
    return email;
  }

  // -------------------------
  // forgotPassword
  // -------------------------

  @Test
  void forgotPassword_withRegisteredEmail_createsResetToken() {
    String email = registerAndVerifyUser();
    String rawToken = UUID.randomUUID().toString();
    mockResetTokenService(rawToken);

    authService.forgotPassword(new ForgotPasswordRequest(email));

    String tokenHash = jwtProvider.hashToken(rawToken);
    assertThat(passwordResetRepository.findByTokenHash(tokenHash)).isPresent();
  }

  @Test
  void forgotPassword_withUnknownEmail_doesNotThrowAndCreatesNoToken() {
    mockResetTokenService(UUID.randomUUID().toString());
    long tokensBefore = passwordResetRepository.count();

    // Nie moze ujawnic ze email nie istnieje — brak wyjatku
    authService.forgotPassword(new ForgotPasswordRequest("nonexistent@example.com"));

    assertThat(passwordResetRepository.count()).isEqualTo(tokensBefore);
  }

  @Test
  void forgotPassword_calledTwice_invalidatesPreviousToken() {
    String email = registerAndVerifyUser();
    String firstToken = UUID.randomUUID().toString();
    mockResetTokenService(firstToken);
    authService.forgotPassword(new ForgotPasswordRequest(email));

    String secondToken = UUID.randomUUID().toString();
    mockResetTokenService(secondToken);
    authService.forgotPassword(new ForgotPasswordRequest(email));

    // Stary token nie powinien juz istniec
    String firstHash = jwtProvider.hashToken(firstToken);
    assertThat(passwordResetRepository.findByTokenHash(firstHash)).isEmpty();

    // Nowy token istnieje
    String secondHash = jwtProvider.hashToken(secondToken);
    assertThat(passwordResetRepository.findByTokenHash(secondHash)).isPresent();
  }

  // -------------------------
  // resetPassword
  // -------------------------

  @Test
  void resetPassword_withValidToken_changesPasswordAndInvalidatesSessions() {
    String email = registerAndVerifyUser();
    // Zaloguj sie zeby stworzyc sesje
    AuthResponse session = authService.login(new LoginRequest(email, TEST_PASSWORD));
    assertThat(refreshTokenRepository.count()).isPositive();

    String rawToken = UUID.randomUUID().toString();
    mockResetTokenService(rawToken);
    authService.forgotPassword(new ForgotPasswordRequest(email));

    authService.resetPassword(new ResetPasswordRequest(rawToken, NEW_PASSWORD));

    // Stare haslo nie dziala
    assertThatThrownBy(() -> authService.login(new LoginRequest(email, TEST_PASSWORD)))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining("Invalid email or password");

    // Nowe haslo dziala
    AuthResponse newSession = authService.login(new LoginRequest(email, NEW_PASSWORD));
    assertThat(newSession.accessToken()).isNotBlank();

    // Stary refresh token zostal uniewazniony
    assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(session.refreshToken())))
        .isInstanceOf(AuthException.class);
  }

  @Test
  void resetPassword_withInvalidToken_throwsAuthException() {
    assertThatThrownBy(
            () ->
                authService.resetPassword(
                    new ResetPasswordRequest("totally-invalid-token", NEW_PASSWORD)))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining("Invalid or expired");
  }

  @Test
  void resetPassword_withExpiredToken_throwsAuthException() {
    String email = registerAndVerifyUser();
    String rawToken = UUID.randomUUID().toString();
    // Token wygasl w przeszlosci
    org.mockito.Mockito.when(emailService.generateResetToken()).thenReturn(rawToken);
    org.mockito.Mockito.when(emailService.resetTokenExpiry())
        .thenReturn(Instant.now().minusSeconds(1));
    authService.forgotPassword(new ForgotPasswordRequest(email));

    assertThatThrownBy(
            () -> authService.resetPassword(new ResetPasswordRequest(rawToken, NEW_PASSWORD)))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void resetPassword_usedTwice_throwsAuthExceptionOnSecondUse() {
    String email = registerAndVerifyUser();
    String rawToken = UUID.randomUUID().toString();
    mockResetTokenService(rawToken);
    authService.forgotPassword(new ForgotPasswordRequest(email));
    authService.resetPassword(new ResetPasswordRequest(rawToken, NEW_PASSWORD));

    // Powtorne uzycie tokena musi byc odrzucone
    assertThatThrownBy(
            () ->
                authService.resetPassword(new ResetPasswordRequest(rawToken, "AnotherPassword789")))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining("already been used");
  }
}
