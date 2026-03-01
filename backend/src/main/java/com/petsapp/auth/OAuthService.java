package com.petsapp.auth;

import java.text.Normalizer;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logika uwierzytelniania przez Google OAuth2.
 *
 * <p>Weryfikuje Google ID token, a nastepnie: 1. Jesli uzytkownik z danym oauth_id istnieje —
 * zwraca go (logowanie). 2. Jesli email istnieje bez oauth_id — lacze konto Google z istniejacym
 * kontem. 3. Jesli uzytkownik nie istnieje — tworzy nowe konto (rejestracja). W przypadku (2) i (3)
 * konto jest automatycznie zweryfikowane (Google gwarantuje zweryfikowany email).
 */
@Service
public class OAuthService {

  private static final Logger log = LoggerFactory.getLogger(OAuthService.class);
  private static final int MAX_USERNAME_GENERATION_ATTEMPTS = 10;
  private static final String OAUTH_PROVIDER_GOOGLE = "google";

  private final GoogleTokenVerifier googleTokenVerifier;
  private final UserRepository userRepository;
  private final UserSettingsRepository userSettingsRepository;

  public OAuthService(
      GoogleTokenVerifier googleTokenVerifier,
      UserRepository userRepository,
      UserSettingsRepository userSettingsRepository) {
    this.googleTokenVerifier = googleTokenVerifier;
    this.userRepository = userRepository;
    this.userSettingsRepository = userSettingsRepository;
  }

  /**
   * Weryfikuje Google ID token i zwraca uzytkownika (istniejacego lub nowego).
   *
   * @param idToken raw Google ID token z klienta mobilnego/webowego
   * @return uzytkownik — gotowy do wystawienia tokenow sesji
   * @throws AuthException gdy token jest nieprawidlowy
   */
  @Transactional
  public User authenticateWithGoogle(String idToken) {
    GoogleIdPayload payload = googleTokenVerifier.verify(idToken);
    log.debug("Google token verified for subject: {}", payload.subject());
    return findOrCreateUser(payload);
  }

  private User findOrCreateUser(GoogleIdPayload payload) {
    // Przypadek 1: konto juz polaczone z tym kontem Google
    Optional<User> byOauth =
        userRepository.findByOauthProviderAndOauthId(OAUTH_PROVIDER_GOOGLE, payload.subject());
    if (byOauth.isPresent()) {
      log.debug("Existing Google user found: {}", payload.email());
      return byOauth.get();
    }

    // Przypadek 2: email juz zarejestrowany przez email/haslo — lacz konta
    Optional<User> byEmail = userRepository.findActiveByEmail(payload.email());
    if (byEmail.isPresent()) {
      User existingUser = byEmail.get();
      existingUser.linkOAuth(OAUTH_PROVIDER_GOOGLE, payload.subject());
      User linked = userRepository.save(existingUser);
      log.info("Linked Google account to existing user: {}", payload.email());
      return linked;
    }

    // Przypadek 3: nowy uzytkownik — tworz konto
    return createGoogleUser(payload);
  }

  private User createGoogleUser(GoogleIdPayload payload) {
    String username = resolveUniqueUsername(payload);
    User newUser =
        User.builder()
            .email(payload.email())
            .username(username)
            .oauthProvider(OAUTH_PROVIDER_GOOGLE)
            .oauthId(payload.subject())
            .build();
    // Konta Google maja zawsze zweryfikowany email
    newUser.markEmailVerified();
    userRepository.save(newUser);

    UserSettings settings = new UserSettings(newUser);
    userSettingsRepository.save(settings);

    log.info("New user created via Google OAuth: {}", payload.email());
    return newUser;
  }

  /**
   * Generuje unikalny username na podstawie imienia z Google.
   *
   * <p>Algorytm: bierze czesc imienia z Google, normalizuje do ascii, skraca do 20 znakow, jesli
   * zajety dodaje losowy suffix UUID.
   */
  private String resolveUniqueUsername(GoogleIdPayload payload) {
    String base = buildUsernameBase(payload);

    if (!userRepository.existsActiveByUsername(base)) {
      return base;
    }

    for (int attempt = 0; attempt < MAX_USERNAME_GENERATION_ATTEMPTS; attempt++) {
      String candidate = base + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
      if (!userRepository.existsActiveByUsername(candidate)) {
        return candidate;
      }
    }

    // Ostatnia deska ratunku — UUID bez prefiksu nazwy gwarantuje unikalnosc
    return "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }

  private String buildUsernameBase(GoogleIdPayload payload) {
    String source = payload.name() != null ? payload.name() : payload.email().split("@")[0];
    // Normalizacja unicode do ASCII, usun niedozwolone znaki, zamien spacje na
    // podkreslniki
    String normalized =
        Normalizer.normalize(source, Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .replaceAll("[^a-zA-Z0-9_]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "")
            .toLowerCase();

    if (normalized.isEmpty()) {
      return "user";
    }
    // Ogranicz do 20 znakow (zostawiamy miejsce na suffix)
    return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
  }
}
