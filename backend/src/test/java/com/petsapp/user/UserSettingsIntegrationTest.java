package com.petsapp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.petsapp.AbstractIntegrationTest;
import com.petsapp.auth.AuthService;
import com.petsapp.auth.EmailService;
import com.petsapp.auth.RegisterRequest;
import com.petsapp.auth.UserRepository;
import com.petsapp.auth.VerifyEmailRequest;
import com.petsapp.storage.StorageService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla endpointow ustawien uzytkownika.
 *
 * <p>Sprawdzamy pobieranie domyslnych ustawien i aktualizacje kazdego pola. EmailService i
 * StorageService sa mockowane.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserSettingsIntegrationTest extends AbstractIntegrationTest {

  @Autowired private UserService userService;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;

  @MockBean private EmailService emailService;
  @MockBean private StorageService storageService;

  private static final String TEST_PASSWORD = "SecurePassword123";

  @BeforeEach
  void setupMocks() {
    doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  private String uniqueEmail() {
    return "settings-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueUsername() {
    return "stg" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
  }

  private UUID createVerifiedUser(String email, String username) {
    String code = "654321";
    when(emailService.generateVerificationCode()).thenReturn(code);
    when(emailService.verificationCodeExpiry()).thenReturn(Instant.now().plusSeconds(900));

    authService.register(new RegisterRequest(email, username, TEST_PASSWORD));
    authService.verifyEmail(new VerifyEmailRequest(email, code));

    return userRepository.findActiveByEmail(email).orElseThrow().getId();
  }

  // -------------------------
  // getMySettings
  // -------------------------

  @Test
  void getMySettings_returnsDefaultSettings() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());

    UserSettingsResponse settings = userService.getMySettings(userId);

    assertThat(settings.pushLikes()).isTrue();
    assertThat(settings.pushComments()).isTrue();
    assertThat(settings.pushFriendRequests()).isTrue();
    assertThat(settings.pushAchievements()).isTrue();
    assertThat(settings.language()).isEqualTo("pl");
    assertThat(settings.darkMode()).isFalse();
  }

  // -------------------------
  // updateSettings
  // -------------------------

  @Test
  void updateSettings_withAllFieldsChanged_updatesSuccessfully() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());
    UpdateUserSettingsRequest request =
        new UpdateUserSettingsRequest(false, false, false, false, "en", true);

    UserSettingsResponse result = userService.updateSettings(userId, request);

    assertThat(result.pushLikes()).isFalse();
    assertThat(result.pushComments()).isFalse();
    assertThat(result.pushFriendRequests()).isFalse();
    assertThat(result.pushAchievements()).isFalse();
    assertThat(result.language()).isEqualTo("en");
    assertThat(result.darkMode()).isTrue();
  }

  @Test
  void updateSettings_withPartialChange_persistsCorrectly() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());

    // Pierwsza aktualizacja — wylacz likes i zmien jezyk
    userService.updateSettings(
        userId, new UpdateUserSettingsRequest(false, true, true, true, "en", false));

    // Druga aktualizacja — wlacz likes z powrotem
    userService.updateSettings(
        userId, new UpdateUserSettingsRequest(true, true, true, true, "en", false));
    UserSettingsResponse settings = userService.getMySettings(userId);

    assertThat(settings.pushLikes()).isTrue();
    assertThat(settings.language()).isEqualTo("en");
  }

  @Test
  void updateSettings_persistsAcrossRequests() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());
    UpdateUserSettingsRequest request =
        new UpdateUserSettingsRequest(false, true, false, true, "en", true);

    userService.updateSettings(userId, request);
    // Drugie pobranie — sprawdzamy ze zapis byl trwaly
    UserSettingsResponse reloaded = userService.getMySettings(userId);

    assertThat(reloaded.pushLikes()).isFalse();
    assertThat(reloaded.pushComments()).isTrue();
    assertThat(reloaded.pushFriendRequests()).isFalse();
    assertThat(reloaded.pushAchievements()).isTrue();
    assertThat(reloaded.language()).isEqualTo("en");
    assertThat(reloaded.darkMode()).isTrue();
  }
}
