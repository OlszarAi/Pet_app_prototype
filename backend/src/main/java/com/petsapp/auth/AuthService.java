package com.petsapp.auth;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logika biznesowa uwierzytelniania i zarzadzania sesjami.
 *
 * <p>
 * Odpowiada za pelny cykl zycia konta: rejestracja, weryfikacja emaila,
 * logowanie, odswiezanie
 * sesji i wylogowanie. Kazda metoda modyfikujaca dane bazy jest @Transactional.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailVerificationRepository emailVerificationRepository,
            UserSettingsRepository userSettingsRepository,
            JwtProvider jwtProvider,
            JwtProperties jwtProperties,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.jwtProvider = jwtProvider;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Rejestruje nowego uzytkownika.
     *
     * <p>
     * Hashuje haslo BCrypt (cost 12 w konfiguracji PasswordEncoder), tworzy
     * ustawienia domyslne,
     * generuje kod weryfikacyjny i wysyla email.
     *
     * @param request dane rejestracji (email, username, password)
     * @throws ConflictException gdy email lub username sa juz zajete
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsActiveByEmail(request.email())) {
            throw new ConflictException("Email is already registered.");
        }
        if (userRepository.existsActiveByUsername(request.username())) {
            throw new ConflictException("Username is already taken.");
        }

        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        userRepository.save(user);

        // Domyslne ustawienia — tworzone razem z kontem
        UserSettings settings = new UserSettings(user);
        userSettingsRepository.save(settings);

        // Kod weryfikacyjny wazny 15 minut
        String code = emailService.generateVerificationCode();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .code(code)
                .expiresAt(emailService.verificationCodeExpiry())
                .build();
        emailVerificationRepository.save(verification);

        // Wysylka asynchroniczna — nie blokuje odpowiedzi API
        emailService.sendVerificationCode(user.getEmail(), code, user.getUsername());

        log.info("User registered: {}", user.getEmail());
    }

    /**
     * Weryfikuje email i aktywuje konto. Zwraca tokeny sesji.
     *
     * @param request email i kod weryfikacyjny
     * @return tokeny dostepowe
     * @throws AuthException gdy kod jest nieprawidlowy, przeterminowany lub juz
     *                       uzyty
     */
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository
                .findActiveByEmail(request.email())
                .orElseThrow(() -> new AuthException("Invalid email or code."));

        if (user.isEmailVerified()) {
            throw new AuthException("Email is already verified.");
        }

        EmailVerification verification = emailVerificationRepository
                .findByUserIdAndCode(user.getId(), request.code())
                .orElseThrow(() -> new AuthException("Invalid email or code."));

        if (verification.isExpired()) {
            throw new AuthException("Verification code has expired. Please request a new one.");
        }
        if (verification.isUsed()) {
            throw new AuthException("Verification code has already been used.");
        }

        verification.markUsed();
        emailVerificationRepository.save(verification);

        user.markEmailVerified();
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
        return generateAndPersistTokens(user);
    }

    /**
     * Ponownie wysyla kod weryfikacyjny na email.
     *
     * @param request email uzytkownika
     * @throws AuthException gdy konto nie istnieje lub jest juz zweryfikowane
     */
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        User user = userRepository
                .findActiveByEmail(request.email())
                .orElseThrow(() -> new AuthException("Account not found."));

        if (user.isEmailVerified()) {
            throw new AuthException("Email is already verified.");
        }

        String code = emailService.generateVerificationCode();
        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .code(code)
                .expiresAt(emailService.verificationCodeExpiry())
                .build();
        emailVerificationRepository.save(verification);

        emailService.sendVerificationCode(user.getEmail(), code, user.getUsername());
        log.debug("Verification code resent to: {}", user.getEmail());
    }

    /**
     * Loguje uzytkownika i zwraca tokeny sesji.
     *
     * @param request email i haslo
     * @return tokeny dostepowe
     * @throws AuthException gdy dane bledne lub email niezweryfikowany
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findActiveByEmail(request.email())
                // Celowo ogolny komunikat — nie ujawniamy czy email istnieje
                .orElseThrow(() -> new AuthException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password.");
        }

        if (!user.isEmailVerified()) {
            throw new AuthException("Email address is not verified. Please check your inbox.");
        }

        log.info("User logged in: {}", user.getEmail());
        return generateAndPersistTokens(user);
    }

    /**
     * Wystawia nowy access token na podstawie waznego refresh tokena.
     *
     * @param request refresh token
     * @return nowy access token (refresh token bez zmian)
     * @throws AuthException gdy refresh token jest nieprawidlowy lub
     *                       przeterminowany
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = jwtProvider.hashToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid or expired refresh token."));

        if (refreshToken.isExpired()) {
            // Usun przeterminowany token zeby nie zasmiecac bazy
            refreshTokenRepository.delete(refreshToken);
            throw new AuthException("Refresh token has expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        if (user.getDeletedAt() != null) {
            throw new AuthException("Account has been deactivated.");
        }

        String newAccessToken = jwtProvider.generateAccessToken(user);
        long expiresIn = jwtProperties.getAccessTokenExpirationMs() / 1000;

        // Refresh token pozostaje bez zmian (sliding window nie jest stosowane celowo —
        // prostsze audyt)
        return new AuthResponse(newAccessToken, request.refreshToken(), expiresIn);
    }

    /**
     * Wylogowuje uzytkownika unieważniając podany refresh token.
     *
     * @param request refresh token do usuniecia
     */
    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = jwtProvider.hashToken(request.refreshToken());
        refreshTokenRepository.deleteByTokenHash(tokenHash);
        log.debug("Refresh token invalidated.");
    }

    private AuthResponse generateAndPersistTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(user);

        JwtProvider.RefreshTokenPair refreshTokenPair = jwtProvider.generateRefreshToken();
        Instant refreshExpiry = Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenPair.tokenHash())
                .expiresAt(refreshExpiry)
                .build();
        refreshTokenRepository.save(refreshToken);

        long expiresIn = jwtProperties.getAccessTokenExpirationMs() / 1000;
        return new AuthResponse(accessToken, refreshTokenPair.rawToken(), expiresIn);
    }
}
