package com.petsapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import com.petsapp.AbstractIntegrationTest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla pelnego flow uwierzytelniania.
 *
 * <p>
 * Testujemy przez AuthService (nie HTTP) z prawdziwa baza PostgreSQL
 * (Testcontainers).
 * EmailService jest mockowany — nie chcemy wysylac emaili w testach. Kazdy test
 * uzywa
 * unikalnego emaila (UUID suffix) aby uniknac konfliktow miedzy testami w tej
 * samej bazie.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private EmailService emailService;

    private static final String TEST_PASSWORD = "SecurePassword123";

    /**
     * Kazdy test ma swoj unikalny email — baza nie jest czyszczona miedzy testami.
     */
    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private String uniqueUsername() {
        return "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    @BeforeEach
    void setupEmailServiceMock() {
        doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    }

    private void mockEmailServiceForCode(String code) {
        org.mockito.Mockito.when(emailService.generateVerificationCode()).thenReturn(code);
        org.mockito.Mockito.when(emailService.verificationCodeExpiry())
                .thenReturn(Instant.now().plusSeconds(900));
    }

    // -------------------------
    // Rejestracja
    // -------------------------

    @Test
    void register_withValidData_createsUser() {
        mockEmailServiceForCode("123456");
        String email = uniqueEmail();

        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));

        Optional<User> user = userRepository.findActiveByEmail(email);
        assertThat(user).isPresent();
        assertThat(user.get().isEmailVerified()).isFalse();
        assertThat(user.get().getPasswordHash()).isNotEqualTo(TEST_PASSWORD);
    }

    @Test
    void register_withExistingEmail_throwsConflictException() {
        mockEmailServiceForCode("111111");
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));

        assertThatThrownBy(
                () -> authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email is already registered");
    }

    @Test
    void register_withExistingUsername_throwsConflictException() {
        mockEmailServiceForCode("222222");
        String username = uniqueUsername();
        authService.register(new RegisterRequest(uniqueEmail(), username, TEST_PASSWORD));

        assertThatThrownBy(
                () -> authService.register(new RegisterRequest(uniqueEmail(), username, TEST_PASSWORD)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username is already taken");
    }

    // -------------------------
    // Weryfikacja emaila
    // -------------------------

    @Test
    void verifyEmail_withValidCode_activatesAccountAndReturnsTokens() {
        String code = "654321";
        mockEmailServiceForCode(code);
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));

        AuthResponse tokens = authService.verifyEmail(new VerifyEmailRequest(email, code));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(userRepository.findActiveByEmail(email).get().isEmailVerified()).isTrue();
    }

    @Test
    void verifyEmail_withWrongCode_throwsAuthException() {
        mockEmailServiceForCode("777777");
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(email, "000000")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void verifyEmail_withAlreadyVerifiedAccount_throwsAuthException() {
        String code = "888888";
        mockEmailServiceForCode(code);
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));
        authService.verifyEmail(new VerifyEmailRequest(email, code));

        // Proba ponownej weryfikacji
        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(email, code)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("already verified");
    }

    // -------------------------
    // Logowanie
    // -------------------------

    @Test
    void login_withValidCredentials_returnsTokens() {
        String code = "101010";
        mockEmailServiceForCode(code);
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));
        authService.verifyEmail(new VerifyEmailRequest(email, code));

        AuthResponse tokens = authService.login(new LoginRequest(email, TEST_PASSWORD));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.expiresIn()).isPositive();
    }

    @Test
    void login_withUnverifiedEmail_throwsAuthException() {
        mockEmailServiceForCode("202020");
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));

        // Nie wywolujemy verifyEmail — konto niezweryfikowane
        assertThatThrownBy(() -> authService.login(new LoginRequest(email, TEST_PASSWORD)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("not verified");
    }

    @Test
    void login_withWrongPassword_throwsAuthException() {
        String code = "303030";
        mockEmailServiceForCode(code);
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));
        authService.verifyEmail(new VerifyEmailRequest(email, code));

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "WrongPassword!")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Invalid email or password");
    }

    // -------------------------
    // Refresh token
    // -------------------------

    @Test
    void refreshToken_withValidToken_returnsNewAccessToken() {
        String code = "404040";
        mockEmailServiceForCode(code);
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));
        authService.verifyEmail(new VerifyEmailRequest(email, code));
        AuthResponse initial = authService.login(new LoginRequest(email, TEST_PASSWORD));

        AuthResponse refreshed = authService.refresh(new RefreshTokenRequest(initial.refreshToken()));

        // Nowy access token musi byc waznym JWT (niepusty)
        assertThat(refreshed.accessToken()).isNotBlank();
        // Refresh token nie zmienia sie (brak rotation w Kroku 2)
        assertThat(refreshed.refreshToken()).isEqualTo(initial.refreshToken());
        assertThat(refreshed.expiresIn()).isPositive();
    }

    @Test
    void refreshToken_withInvalidToken_throwsAuthException() {
        assertThatThrownBy(
                () -> authService.refresh(new RefreshTokenRequest("invalid-refresh-token-value")))
                .isInstanceOf(AuthException.class);
    }

    // -------------------------
    // Logout
    // -------------------------

    @Test
    void logout_withValidToken_invalidatesRefreshToken() {
        String code = "505050";
        mockEmailServiceForCode(code);
        String email = uniqueEmail();
        authService.register(new RegisterRequest(email, uniqueUsername(), TEST_PASSWORD));
        authService.verifyEmail(new VerifyEmailRequest(email, code));
        AuthResponse tokens = authService.login(new LoginRequest(email, TEST_PASSWORD));

        authService.logout(new LogoutRequest(tokens.refreshToken()));

        // Po wylogowaniu refresh token nie powinien dzialac
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(tokens.refreshToken())))
                .isInstanceOf(AuthException.class);
    }
}
