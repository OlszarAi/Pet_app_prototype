package com.petsapp.user;

import com.petsapp.auth.User;
import com.petsapp.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpointy zarzadzania profilem uzytkownika.
 *
 * <p>Kontroler odpowiada wylacznie za routing, walidacje wejscia i mapowanie na ApiResponse. Cala
 * logika biznesowa jest w {@link UserService}. Aktualnie zalogowany uzytkownik jest wstrzykiwany
 * przez {@code @AuthenticationPrincipal} — JwtFilter ustawia encje {@link User} jako principal.
 */
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /** Pobiera pelny profil zalogowanego uzytkownika (z emailem). */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
      @AuthenticationPrincipal User currentUser) {
    UserProfileResponse profile = userService.getMyProfile(currentUser.getId());
    return ResponseEntity.ok(ApiResponse.ok(profile));
  }

  /** Aktualizuje username i / lub bio. Oba pola sa opcjonalne — null = bez zmiany. */
  @PatchMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
      @AuthenticationPrincipal User currentUser, @Valid @RequestBody UpdateProfileRequest request) {
    UserProfileResponse profile = userService.updateProfile(currentUser.getId(), request);
    return ResponseEntity.ok(ApiResponse.ok(profile));
  }

  /**
   * Wgrywa lub podmienia avatar zalogowanego uzytkownika.
   *
   * <p>Akceptuje multipart/form-data z polem "file". Obraz jest skalowany do 400px i zapisywany
   * jako JPEG.
   */
  @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<UserProfileResponse>> uploadAvatar(
      @AuthenticationPrincipal User currentUser, @RequestPart("file") MultipartFile file) {
    UserProfileResponse profile = userService.uploadAvatar(currentUser.getId(), file);
    return ResponseEntity.ok(ApiResponse.ok(profile));
  }

  /** Usuwa avatar zalogowanego uzytkownika. */
  @DeleteMapping("/me/avatar")
  public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal User currentUser) {
    userService.deleteAvatar(currentUser.getId());
    return ResponseEntity.noContent().build();
  }

  /**
   * Zmienia haslo zalogowanego uzytkownika.
   *
   * <p>Wymaga aktualnego hasla. Po zmianie wszystkie sesje (refresh tokeny) sa uniewazniane.
   */
  @PatchMapping("/me/password")
  public ResponseEntity<Void> updatePassword(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody UpdatePasswordRequest request) {
    userService.updatePassword(currentUser.getId(), request);
    return ResponseEntity.noContent().build();
  }

  /**
   * Soft-deletes konto zalogowanego uzytkownika.
   *
   * <p>Dane sa zachowane w bazie (GDPR). Wszystkie sesje sa natychmiast uniewazniane.
   */
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User currentUser) {
    userService.deleteAccount(currentUser.getId());
    return ResponseEntity.noContent().build();
  }

  /** Pobiera publiczny profil innego uzytkownika. */
  @GetMapping("/{userId}")
  public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
      @PathVariable UUID userId) {
    UserProfileResponse profile = userService.getUserProfile(userId);
    return ResponseEntity.ok(ApiResponse.ok(profile));
  }

  /**
   * Wyszukuje uzytkownikow po fragmencie nazwy (case-insensitive, min 1 znak).
   *
   * <p>Zwraca co najwyzej 20 wynikow. Uzyj tego endpointu przy polu "dodaj znajomego".
   */
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<List<UserProfileResponse>>> searchUsers(
      @RequestParam("q") String query) {
    List<UserProfileResponse> results = userService.searchUsers(query);
    return ResponseEntity.ok(ApiResponse.ok(results));
  }

  /** Pobiera ustawienia powiadomien i interfejsu zalogowanego uzytkownika. */
  @GetMapping("/me/settings")
  public ResponseEntity<ApiResponse<UserSettingsResponse>> getMySettings(
      @AuthenticationPrincipal User currentUser) {
    UserSettingsResponse settings = userService.getMySettings(currentUser.getId());
    return ResponseEntity.ok(ApiResponse.ok(settings));
  }

  /** Aktualizuje ustawienia zalogowanego uzytkownika. */
  @PatchMapping("/me/settings")
  public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody UpdateUserSettingsRequest request) {
    UserSettingsResponse settings = userService.updateSettings(currentUser.getId(), request);
    return ResponseEntity.ok(ApiResponse.ok(settings));
  }
}
