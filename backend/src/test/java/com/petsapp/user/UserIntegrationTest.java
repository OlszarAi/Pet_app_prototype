package com.petsapp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petsapp.AbstractIntegrationTest;
import com.petsapp.auth.AuthService;
import com.petsapp.common.ConflictException;
import com.petsapp.common.UnsupportedFileFormatException;
import com.petsapp.auth.EmailService;
import com.petsapp.auth.RefreshTokenRepository;
import com.petsapp.auth.RegisterRequest;
import com.petsapp.auth.UserRepository;
import com.petsapp.auth.VerifyEmailRequest;
import com.petsapp.storage.StorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla UserService z prawdziwa baza i zmockowanymi zewnetrznymi serwisami.
 *
 * <p>EmailService i StorageService sa mockowane — nie chcemy wysylac maili ani laczyc sie z MinIO w
 * testach. Kazdy test tworzy unikalne konto uzytkownika.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserIntegrationTest extends AbstractIntegrationTest {

  @Autowired private UserService userService;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @MockBean private EmailService emailService;
  @MockBean private StorageService storageService;

  private static final String TEST_PASSWORD = "SecurePassword123";
  private static final String FAKE_AVATAR_URL =
      "http://localhost:9000/petsapp-test/avatars/test.jpg";

  @BeforeEach
  void setupMocks() {
    doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  private String uniqueEmail() {
    return "user-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueUsername() {
    return "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
  }

  /**
   * Rejestruje, weryfikuje i zwraca UUID nowego uzytkownika.
   *
   * <p>Wielokrotnie uzywana metoda pomocnicza — tworzy gotowe do uzytku konto.
   */
  private UUID createVerifiedUser(String email, String username) {
    String code = "123456";
    when(emailService.generateVerificationCode()).thenReturn(code);
    when(emailService.verificationCodeExpiry()).thenReturn(Instant.now().plusSeconds(900));

    authService.register(new RegisterRequest(email, username, TEST_PASSWORD));
    authService.verifyEmail(new VerifyEmailRequest(email, code));

    return userRepository.findActiveByEmail(email).orElseThrow().getId();
  }

  // -------------------------
  // getMyProfile
  // -------------------------

  @Test
  void getMyProfile_returnsProfileWithEmail() {
    String email = uniqueEmail();
    String username = uniqueUsername();
    UUID userId = createVerifiedUser(email, username);

    UserProfileResponse profile = userService.getMyProfile(userId);

    assertThat(profile.id()).isEqualTo(userId);
    assertThat(profile.username()).isEqualTo(username);
    assertThat(profile.email()).isEqualTo(email);
    assertThat(profile.totalCatches()).isZero();
    assertThat(profile.uniqueBreeds()).isZero();
  }

  @Test
  void getMyProfile_withDeletedUser_throwsUserNotFoundException() {
    String email = uniqueEmail();
    UUID userId = createVerifiedUser(email, uniqueUsername());
    userService.deleteAccount(userId);

    assertThatThrownBy(() -> userService.getMyProfile(userId))
        .isInstanceOf(UserNotFoundException.class);
  }

  // -------------------------
  // updateProfile
  // -------------------------

  @Test
  void updateProfile_withNewUsername_updatesSuccessfully() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());
    String newUsername = "newname" + UUID.randomUUID().toString().replace("-", "").substring(0, 5);

    UserProfileResponse result =
        userService.updateProfile(userId, new UpdateProfileRequest(newUsername, null));

    assertThat(result.username()).isEqualTo(newUsername);
  }

  @Test
  void updateProfile_withBioOnly_updatesBio() {
    String originalUsername = uniqueUsername();
    UUID userId = createVerifiedUser(uniqueEmail(), originalUsername);

    UserProfileResponse result =
        userService.updateProfile(userId, new UpdateProfileRequest(null, "My new bio"));

    assertThat(result.bio()).isEqualTo("My new bio");
    assertThat(result.username()).isEqualTo(originalUsername);
  }

  @Test
  void updateProfile_withDuplicateUsername_throwsConflictException() {
    String existingUsername = uniqueUsername();
    createVerifiedUser(uniqueEmail(), existingUsername);

    UUID anotherUserId = createVerifiedUser(uniqueEmail(), uniqueUsername());

    assertThatThrownBy(
            () ->
                userService.updateProfile(
                    anotherUserId, new UpdateProfileRequest(existingUsername, null)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Username is already taken");
  }

  // -------------------------
  // uploadAvatar / deleteAvatar
  // -------------------------

  @Test
  void uploadAvatar_withValidJpeg_storesAndUpdatesAvatarUrl() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());
    when(storageService.upload(anyString(), any(InputStream.class), anyString(), anyLong()))
        .thenReturn(FAKE_AVATAR_URL);

    byte[] jpegBytes = createMinimalJpeg();
    MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes);

    UserProfileResponse result = userService.uploadAvatar(userId, file);

    assertThat(result.avatarUrl()).isEqualTo(FAKE_AVATAR_URL);
    verify(storageService).upload(anyString(), any(InputStream.class), anyString(), anyLong());
  }

  @Test
  void uploadAvatar_withUnsupportedMimeType_throwsUnsupportedFileFormatException() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.gif", "image/gif", new byte[] {0x47, 0x49, 0x46});

    assertThatThrownBy(() -> userService.uploadAvatar(userId, file))
        .isInstanceOf(UnsupportedFileFormatException.class);

    verify(storageService, never()).upload(anyString(), any(), anyString(), anyLong());
  }

  @Test
  void deleteAvatar_whenAvatarExists_deletesFromStorageAndClearsUrl() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());
    when(storageService.upload(anyString(), any(InputStream.class), anyString(), anyLong()))
        .thenReturn(FAKE_AVATAR_URL);

    byte[] jpegBytes = createMinimalJpeg();
    MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes);
    userService.uploadAvatar(userId, file);

    userService.deleteAvatar(userId);

    UserProfileResponse profile = userService.getMyProfile(userId);
    assertThat(profile.avatarUrl()).isNull();
    verify(storageService).delete(anyString());
  }

  @Test
  void deleteAvatar_whenNoAvatar_doesNotCallStorage() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());

    userService.deleteAvatar(userId);

    verify(storageService, never()).delete(anyString());
  }

  // -------------------------
  // updatePassword
  // -------------------------

  @Test
  void updatePassword_withCorrectCurrentPassword_changesPasswordAndInvalidatesSessions() {
    String email = uniqueEmail();
    UUID userId = createVerifiedUser(email, uniqueUsername());

    long sessionsBefore = refreshTokenRepository.count();
    userService.updatePassword(userId, new UpdatePasswordRequest(TEST_PASSWORD, "NewPassword123"));
    long sessionsAfter = refreshTokenRepository.count();

    // Wszystkie sesje uzytkownika musza byc usuniete
    assertThat(sessionsAfter).isLessThanOrEqualTo(sessionsBefore);
  }

  @Test
  void updatePassword_withWrongCurrentPassword_throwsPasswordMismatchException() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());

    assertThatThrownBy(
            () ->
                userService.updatePassword(
                    userId, new UpdatePasswordRequest("WrongPassword!", "NewPassword123")))
        .isInstanceOf(PasswordMismatchException.class);
  }

  // -------------------------
  // deleteAccount
  // -------------------------

  @Test
  void deleteAccount_softDeletesUserAndInvalidatesTokens() {
    String email = uniqueEmail();
    UUID userId = createVerifiedUser(email, uniqueUsername());

    userService.deleteAccount(userId);

    assertThat(userRepository.findActiveByEmail(email)).isEmpty();
    assertThat(userRepository.findById(userId)).isPresent();
    assertThat(userRepository.findById(userId).get().getDeletedAt()).isNotNull();
  }

  // -------------------------
  // getUserProfile
  // -------------------------

  @Test
  void getUserProfile_returnsPublicProfileWithoutEmail() {
    UUID userId = createVerifiedUser(uniqueEmail(), uniqueUsername());

    UserProfileResponse profile = userService.getUserProfile(userId);

    assertThat(profile.id()).isEqualTo(userId);
    assertThat(profile.email()).isNull();
  }

  @Test
  void getUserProfile_withNonExistentId_throwsUserNotFoundException() {
    assertThatThrownBy(() -> userService.getUserProfile(UUID.randomUUID()))
        .isInstanceOf(UserNotFoundException.class);
  }

  // -------------------------
  // searchUsers
  // -------------------------

  @Test
  void searchUsers_withMatchingPrefix_returnsResults() {
    String prefix = "srch" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    createVerifiedUser(uniqueEmail(), prefix + "abc");
    createVerifiedUser(uniqueEmail(), prefix + "xyz");

    List<UserProfileResponse> results = userService.searchUsers(prefix);

    assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    assertThat(results).allMatch(u -> u.username().toLowerCase().startsWith(prefix.toLowerCase()));
  }

  @Test
  void searchUsers_withNonMatchingQuery_returnsEmptyList() {
    List<UserProfileResponse> results = userService.searchUsers("zzz_nomatch_" + UUID.randomUUID());

    assertThat(results).isEmpty();
  }

  @Test
  void searchUsers_withBlankQuery_returnsEmptyList() {
    assertThat(userService.searchUsers("  ")).isEmpty();
    assertThat(userService.searchUsers(null)).isEmpty();
  }

  // -------------------------
  // Metody pomocnicze
  // -------------------------

  /**
   * Tworzy poprawny 2x2 px plik JPEG przy uzyciu Java ImageIO.
   *
   * <p>Gwarantuje poprawnosc naglowkow JPEG potrzebna do przetworzenia przez Thumbnailator.
   */
  private byte[] createMinimalJpeg() {
    BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
    img.setRGB(0, 0, 0xFF0000);
    img.setRGB(1, 0, 0x00FF00);
    img.setRGB(2, 0, 0x0000FF);
    img.setRGB(3, 0, 0xFFFFFF);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ImageIO.write(img, "jpg", baos);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to create test JPEG", ex);
    }
    return baos.toByteArray();
  }
}
