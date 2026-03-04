package com.petsapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testy jednostkowe dla logiki find-or-create uzytkownika Google OAuth.
 *
 * <p>Czysty unit test — bez Spring context, bez bazy danych. Wszystkie zaleznosci sa mockowane
 * przez Mockito. GoogleTokenVerifier jest mockowany, wiec nie ma potrzeby prawdziwego Google API.
 */
@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

  @Mock private GoogleTokenVerifier googleTokenVerifier;

  @Mock private UserRepository userRepository;

  @Mock private UserSettingsRepository userSettingsRepository;

  private OAuthService oauthService;

  @BeforeEach
  void setUp() {
    oauthService = new OAuthService(googleTokenVerifier, userRepository, userSettingsRepository);
  }

  // -------------------------
  // authenticateWithGoogle — Przypadek 1: istniejace konto Google
  // -------------------------

  @Test
  void authenticateWithGoogle_existingGoogleAccount_returnsExistingUser() {
    GoogleIdPayload payload =
        new GoogleIdPayload("google-sub-123", "jan@gmail.com", "Jan Kowalski");
    User existingUser = buildMinimalUser("jan@gmail.com", "jankowalski");

    when(googleTokenVerifier.verify("valid-id-token")).thenReturn(payload);
    when(userRepository.findByOauthProviderAndOauthId("google", "google-sub-123"))
        .thenReturn(Optional.of(existingUser));

    User result = oauthService.authenticateWithGoogle("valid-id-token");

    assertThat(result).isSameAs(existingUser);
    verify(userRepository, never()).save(any());
  }

  // -------------------------
  // authenticateWithGoogle — Przypadek 2: email juz zarejestrowany, lacz konta
  // -------------------------

  @Test
  void authenticateWithGoogle_existingEmailAccount_linksOAuthAndReturnsUser() {
    GoogleIdPayload payload =
        new GoogleIdPayload("google-sub-456", "anna@example.com", "Anna Nowak");
    User existingUser = buildMinimalUser("anna@example.com", "anna_nowak");
    User linkedUser = buildMinimalUser("anna@example.com", "anna_nowak");

    when(googleTokenVerifier.verify("id-token")).thenReturn(payload);
    when(userRepository.findByOauthProviderAndOauthId("google", "google-sub-456"))
        .thenReturn(Optional.empty());
    when(userRepository.findActiveByEmail("anna@example.com"))
        .thenReturn(Optional.of(existingUser));
    when(userRepository.save(existingUser)).thenReturn(linkedUser);

    User result = oauthService.authenticateWithGoogle("id-token");

    assertThat(result).isSameAs(linkedUser);
    // Konto musi byc polaczone z Google
    verify(userRepository).save(existingUser);
  }

  // -------------------------
  // authenticateWithGoogle — Przypadek 3: nowy uzytkownik
  // -------------------------

  @Test
  void authenticateWithGoogle_newUser_createsAccountWithVerifiedEmail() {
    GoogleIdPayload payload =
        new GoogleIdPayload("google-sub-789", "new@gmail.com", "Nowy Uzytkownik");

    when(googleTokenVerifier.verify("id-token")).thenReturn(payload);
    when(userRepository.findByOauthProviderAndOauthId("google", "google-sub-789"))
        .thenReturn(Optional.empty());
    when(userRepository.findActiveByEmail("new@gmail.com")).thenReturn(Optional.empty());
    when(userRepository.existsActiveByUsername(anyString())).thenReturn(false);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    User savedUser = buildMinimalUser("new@gmail.com", "nowy_uzytkownik");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    oauthService.authenticateWithGoogle("id-token");

    verify(userRepository).save(userCaptor.capture());
    User capturedUser = userCaptor.getValue();
    assertThat(capturedUser.isEmailVerified()).isTrue();
    assertThat(capturedUser.getOauthProvider()).isEqualTo("google");
    assertThat(capturedUser.getOauthId()).isEqualTo("google-sub-789");
    verify(userSettingsRepository).save(any(UserSettings.class));
  }

  @Test
  void authenticateWithGoogle_newUserWithTakenUsername_generatesUniqueUsername() {
    GoogleIdPayload payload = new GoogleIdPayload("sub-001", "user@gmail.com", "Jan");

    when(googleTokenVerifier.verify("id-token")).thenReturn(payload);
    when(userRepository.findByOauthProviderAndOauthId(anyString(), anyString()))
        .thenReturn(Optional.empty());
    when(userRepository.findActiveByEmail(anyString())).thenReturn(Optional.empty());
    // Pierwsze sprawdzenie username zajete, drugie wolne
    when(userRepository.existsActiveByUsername(anyString())).thenReturn(true).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    oauthService.authenticateWithGoogle("id-token");

    verify(userRepository).save(any(User.class));
    // Verifikacja ze username NIE jest "jan" (bo bylo zajete) lecz z suffixem
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getUsername()).isNotEqualTo("jan");
    assertThat(captor.getValue().getUsername()).contains("jan");
  }

  @Test
  void authenticateWithGoogle_invalidToken_throwsAuthException() {
    when(googleTokenVerifier.verify("bad-token"))
        .thenThrow(new AuthException("Invalid Google ID token."));

    assertThatThrownBy(() -> oauthService.authenticateWithGoogle("bad-token"))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining("Invalid Google ID token");
  }

  @Test
  void authenticateWithGoogle_nullName_usesEmailPrefixAsUsername() {
    GoogleIdPayload payload = new GoogleIdPayload("sub-null-name", "prefix@gmail.com", null);

    when(googleTokenVerifier.verify("id-token")).thenReturn(payload);
    when(userRepository.findByOauthProviderAndOauthId(anyString(), anyString()))
        .thenReturn(Optional.empty());
    when(userRepository.findActiveByEmail(anyString())).thenReturn(Optional.empty());
    when(userRepository.existsActiveByUsername(anyString())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    oauthService.authenticateWithGoogle("id-token");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    // Username powinien byc oparty na "prefix" (czesc przed @ w emailu)
    assertThat(captor.getValue().getUsername()).contains("prefix");
  }

  /**
   * Buduje minimalny obiekt User na potrzeby testow (omijamy prywatny konstruktor przez builder).
   */
  private User buildMinimalUser(String email, String username) {
    return User.builder()
        .email(email)
        .username(username)
        .oauthProvider("google")
        .oauthId("test-sub")
        .build();
  }
}
