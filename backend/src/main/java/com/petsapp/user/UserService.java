package com.petsapp.user;

import com.petsapp.common.ConflictException;
import com.petsapp.auth.RefreshTokenRepository;
import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import com.petsapp.auth.UserSettings;
import com.petsapp.auth.UserSettingsRepository;
import com.petsapp.common.UnsupportedFileFormatException;
import com.petsapp.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Logika biznesowa zarzadzania profilem i ustawieniami uzytkownika.
 *
 * <p>Odpowiada za aktualizacje profilu, zmiane hasla, upload / usuwanie avatara, soft delete konta
 * oraz wyszukiwanie uzytkownikow. Kazda operacja modyfikujaca baze jest {@code @Transactional}.
 * Upload avatara nie jest transakcyjny — zewnetrzne S3 API nie uczestniczy w transakcji JPA.
 */
@Service
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private static final Set<String> ALLOWED_AVATAR_MIME_TYPES = Set.of("image/jpeg", "image/png");
  private static final int AVATAR_DIMENSION = 400;
  private static final String AVATAR_KEY_PREFIX = "avatars/";
  private static final String AVATAR_CONTENT_TYPE = "image/jpeg";
  private static final int USER_SEARCH_LIMIT = 20;

  private final UserRepository userRepository;
  private final UserSettingsRepository userSettingsRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final StorageService storageService;
  private final PasswordEncoder passwordEncoder;

  public UserService(
      UserRepository userRepository,
      UserSettingsRepository userSettingsRepository,
      RefreshTokenRepository refreshTokenRepository,
      StorageService storageService,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.userSettingsRepository = userSettingsRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.storageService = storageService;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Pobiera pelny profil zalogowanego uzytkownika (wlacznie z emailem).
   *
   * @param userId ID zalogowanego uzytkownika
   * @return pelny profil z emailem
   * @throws UserNotFoundException gdy uzytkownik nie istnieje lub jest soft-deleted
   */
  @Transactional(readOnly = true)
  public UserProfileResponse getMyProfile(UUID userId) {
    User user = findActiveUser(userId);
    return UserProfileResponse.fromOwner(user);
  }

  /**
   * Aktualizuje username i / lub bio zalogowanego uzytkownika.
   *
   * <p>Oba pola sa opcjonalne — null oznacza brak zmiany. Jezeli nowy username jest juz zajety
   * przez innego uzytkownika, rzuca {@link ConflictException}.
   *
   * @param userId ID wlasciciela profilu
   * @param request dane do aktualizacji (username, bio)
   * @return zaktualizowany profil
   */
  @Transactional
  public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
    User user = findActiveUser(userId);

    if (request.username() != null && !request.username().equals(user.getUsername())) {
      if (userRepository.existsActiveByUsername(request.username())) {
        throw new ConflictException("Username is already taken.");
      }
    }

    user.updateProfile(request.username(), request.bio());
    userRepository.save(user);

    log.debug("Profile updated for user={}", userId);
    return UserProfileResponse.fromOwner(user);
  }

  /**
   * Wgrywa nowy avatar uzytkownika.
   *
   * <p>Obraz jest skalowany do {@value #AVATAR_DIMENSION}x{@value #AVATAR_DIMENSION} px (z
   * zachowaniem proporcji) i zapisywany jako JPEG. Stary avatar jest usuwany z magazynu przed
   * wygraniem nowego. Operacja na S3 nie jest transakcyjna — awaria po usunieciu a przed uploadem
   * jest mozliwa, ale akceptowalna w MVP.
   *
   * @param userId ID wlasciciela profilu
   * @param file plik obrazu (JPEG lub PNG)
   * @return zaktualizowany profil z nowym avatarUrl
   * @throws UnsupportedFileFormatException gdy MIME type jest niedozwolony
   * @throws AvatarProcessingException gdy nie mozna przetworzyc obrazu
   */
  public UserProfileResponse uploadAvatar(UUID userId, MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_AVATAR_MIME_TYPES.contains(contentType)) {
      throw new UnsupportedFileFormatException(
          "Avatar must be JPEG or PNG. Received: " + contentType);
    }

    User user = findActiveUser(userId);
    String storageKey = AVATAR_KEY_PREFIX + userId + ".jpg";

    // Usuniecie starego avatara przed uploadem
    if (user.getAvatarUrl() != null) {
      try {
        storageService.delete(storageKey);
      } catch (Exception ex) {
        log.warn("Could not delete old avatar for user={}: {}", userId, ex.getMessage());
      }
    }

    byte[] processedImage = resizeToJpeg(file, userId);
    String publicUrl =
        storageService.upload(
            storageKey,
            new ByteArrayInputStream(processedImage),
            AVATAR_CONTENT_TYPE,
            processedImage.length);

    updateAvatarUrl(userId, publicUrl);

    log.info("Avatar uploaded for user={}, url={}", userId, publicUrl);
    return UserProfileResponse.fromOwner(findActiveUser(userId));
  }

  /**
   * Usuwa avatar uzytkownika z magazynu i czysci pole avatar_url.
   *
   * @param userId ID wlasciciela profilu
   */
  public void deleteAvatar(UUID userId) {
    User user = findActiveUser(userId);
    if (user.getAvatarUrl() == null) {
      return;
    }

    String storageKey = AVATAR_KEY_PREFIX + userId + ".jpg";
    try {
      storageService.delete(storageKey);
    } catch (Exception ex) {
      log.warn("Could not delete avatar from storage for user={}: {}", userId, ex.getMessage());
    }

    updateAvatarUrl(userId, null);
    log.info("Avatar deleted for user={}", userId);
  }

  /**
   * Zmienia haslo zalogowanego uzytkownika.
   *
   * <p>Wymaga podania aktualnego hasla jako dodatkowej weryfikacji. Po zmianie hasla wszystkie
   * refresh tokeny sa uniewazniane — wymusza ponowne logowanie na wszystkich urzadzeniach.
   *
   * @param userId ID wlasciciela konta
   * @param request aktualne i nowe haslo
   * @throws PasswordMismatchException gdy aktualne haslo jest nieprawidlowe
   */
  @Transactional
  public void updatePassword(UUID userId, UpdatePasswordRequest request) {
    User user = findActiveUser(userId);

    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new PasswordMismatchException("Current password is incorrect.");
    }

    user.updatePassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);

    // Uniewaznij wszystkie sesje — po zmianie hasla uzytkownik musi sie ponownie zalogowac
    refreshTokenRepository.deleteAllByUserId(userId);

    log.info("Password changed for user={}", userId);
  }

  /**
   * Soft-deletes uzytkownika i uniewaznija wszystkie jego refresh tokeny.
   *
   * <p>Dane uzytkownika sa zachowane w bazie (GDPR — na zadanie mozna usunac). deleted_at jest
   * ustawiany natychmiast, wiec kolejne zadania z JWT tego usera beda odrzucane przez JwtFilter.
   *
   * @param userId ID konta do usuniecia
   */
  @Transactional
  public void deleteAccount(UUID userId) {
    User user = findActiveUser(userId);

    refreshTokenRepository.deleteAllByUserId(userId);
    user.softDelete();
    userRepository.save(user);

    log.info("Account soft-deleted for user={}", userId);
  }

  /**
   * Zwraca publiczny profil innego uzytkownika.
   *
   * <p>Profil prywatny jest nadal zwracany (widoczny jest username i avatar), ale zawartosc
   * (catches) bedzie ukryta w endpointach dedicated (krok 6). Uzytkownik usuniety (soft-deleted)
   * zwraca 404.
   *
   * @param targetUserId ID szukanego uzytkownika
   * @return publiczny profil uzytkownika
   * @throws UserNotFoundException gdy uzytkownik nie istnieje lub jest soft-deleted
   */
  @Transactional(readOnly = true)
  public UserProfileResponse getUserProfile(UUID targetUserId) {
    User user = findActiveUser(targetUserId);
    return UserProfileResponse.fromPublic(user);
  }

  /**
   * Wyszukuje aktywnych uzytkownikow po fragmencie username (case-insensitive).
   *
   * <p>Zwraca co najwyzej {@value #USER_SEARCH_LIMIT} wynikow posortowanych alfabetycznie.
   *
   * @param query fragment nazwy uzytkownika (minimum 1 znak)
   * @return lista pasujacych profili
   */
  @Transactional(readOnly = true)
  public List<UserProfileResponse> searchUsers(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }

    return userRepository.searchActiveByUsernamePrefix(query.trim(), USER_SEARCH_LIMIT).stream()
        .map(UserProfileResponse::fromPublic)
        .toList();
  }

  /**
   * Pobiera ustawienia zalogowanego uzytkownika.
   *
   * @param userId ID zalogowanego uzytkownika
   * @return ustawienia powiadomien i interfejsu
   */
  @Transactional(readOnly = true)
  public UserSettingsResponse getMySettings(UUID userId) {
    UserSettings settings = findSettings(userId);
    return UserSettingsResponse.from(settings);
  }

  /**
   * Aktualizuje ustawienia zalogowanego uzytkownika.
   *
   * @param userId ID zalogowanego uzytkownika
   * @param request nowe wartosci ustawien
   * @return zaktualizowane ustawienia
   */
  @Transactional
  public UserSettingsResponse updateSettings(UUID userId, UpdateUserSettingsRequest request) {
    UserSettings settings = findSettings(userId);

    settings.update(
        request.pushLikes(),
        request.pushComments(),
        request.pushFriendRequests(),
        request.pushAchievements(),
        request.language(),
        request.darkMode());

    userSettingsRepository.save(settings);
    log.debug("Settings updated for user={}", userId);
    return UserSettingsResponse.from(settings);
  }

  // ------------------- Metody prywatne -------------------

  private User findActiveUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(u -> u.getDeletedAt() == null)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
  }

  private UserSettings findSettings(UUID userId) {
    return userSettingsRepository
        .findById(userId)
        .orElseThrow(() -> new UserNotFoundException("Settings not found for user: " + userId));
  }

  @Transactional
  private void updateAvatarUrl(UUID userId, String url) {
    User user = findActiveUser(userId);
    user.updateAvatarUrl(url);
    userRepository.save(user);
  }

  private byte[] resizeToJpeg(MultipartFile file, UUID userId) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      Thumbnails.of(file.getInputStream())
          .size(AVATAR_DIMENSION, AVATAR_DIMENSION)
          .keepAspectRatio(true)
          .outputFormat("jpg")
          .outputQuality(0.85)
          .toOutputStream(baos);
    } catch (IOException ex) {
      throw new AvatarProcessingException("Failed to process avatar image for user=" + userId, ex);
    }
    return baos.toByteArray();
  }
}
