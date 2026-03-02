package com.petsapp.achievement;

import com.petsapp.auth.User;
import com.petsapp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpointy achievementow.
 *
 * <p>Wszystkie endpointy wymagaja uwierzytelnienia JWT.
 */
@RestController
@Tag(name = "Achievements", description = "Osiagniecia i postepy uzytkownika")
public class AchievementController {

  private final AchievementRepository achievementRepository;
  private final AchievementUnlockRepository unlockRepository;

  public AchievementController(
      AchievementRepository achievementRepository,
      AchievementUnlockRepository unlockRepository) {
    this.achievementRepository = achievementRepository;
    this.unlockRepository = unlockRepository;
  }

  @GetMapping("/achievements")
  @Operation(summary = "Pobierz pelna liste achievementow z informacja o odblokowaniu")
  public ApiResponse<List<AchievementResponse>> getAll(@AuthenticationPrincipal User currentUser) {
    Set<String> unlockedCodes = getUnlockedCodes(currentUser.getId());
    List<AchievementResponse> responses =
        achievementRepository.findAll().stream()
            .map(a -> AchievementResponse.from(a, unlockedCodes.contains(a.getCode())))
            .toList();
    return ApiResponse.ok(responses);
  }

  @GetMapping("/users/me/achievements")
  @Operation(summary = "Pobierz odblokowane osiagniecia biezacego uzytkownika")
  public ApiResponse<List<AchievementResponse>> getMyAchievements(
      @AuthenticationPrincipal User currentUser) {
    return getAchievementsForUser(currentUser.getId());
  }

  @GetMapping("/users/{userId}/achievements")
  @Operation(summary = "Pobierz odblokowane osiagniecia uzytkownika")
  public ApiResponse<List<AchievementResponse>> getUserAchievements(@PathVariable UUID userId) {
    return getAchievementsForUser(userId);
  }

  private ApiResponse<List<AchievementResponse>> getAchievementsForUser(UUID userId) {
    Set<String> unlockedCodes = getUnlockedCodes(userId);
    List<AchievementResponse> responses =
        achievementRepository.findAll().stream()
            .map(a -> AchievementResponse.from(a, unlockedCodes.contains(a.getCode())))
            .filter(AchievementResponse::unlocked)
            .toList();
    return ApiResponse.ok(responses);
  }

  private Set<String> getUnlockedCodes(UUID userId) {
    return unlockRepository.findByUserIdOrdered(userId).stream()
        .map(au -> au.getAchievement().getCode())
        .collect(Collectors.toSet());
  }
}
