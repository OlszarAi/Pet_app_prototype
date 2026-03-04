package com.petsapp.achievement;

import com.petsapp.auth.User;
import com.petsapp.catch_.DogCatchRepository;
import com.petsapp.friend.FriendshipRepository;
import com.petsapp.notification.NotificationService;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Komponent sprawdzajacy warunki achievementow po kazdym nowym polowaniu.
 *
 * <p>Wywolywany asynchronicznie przez CatchService po pomyslnym zapisaniu DogCatch.
 * Sprawdza wszystkie niezadane achievementy i odblokowuje te, ktorych warunki sa spelnione.
 *
 * <p>Wspierane typy warunkow (condition_type):
 * <ul>
 *   <li>{@code total_catches} — liczba wszystkich zlowien uzytkownika
 *   <li>{@code unique_breeds} — liczba unikalnych ras zlowionych przez uzytkownika
 *   <li>{@code rare_catch} — rarity score nowo zlapanej rasy >= condition_value
 *   <li>{@code friends_count} — liczba zaakceptowanych znajomych
 *   <li>{@code streak} — dlugosc aktualnego streaka (kolejne dni z co najmniej 1 catch)
 * </ul>
 */
@Component
public class AchievementChecker {

  private static final Logger log = LoggerFactory.getLogger(AchievementChecker.class);

  private final AchievementRepository achievementRepository;
  private final AchievementUnlockRepository unlockRepository;
  private final DogCatchRepository catchRepository;
  private final FriendshipRepository friendshipRepository;
  private final NotificationService notificationService;

  public AchievementChecker(
      AchievementRepository achievementRepository,
      AchievementUnlockRepository unlockRepository,
      DogCatchRepository catchRepository,
      FriendshipRepository friendshipRepository,
      NotificationService notificationService) {
    this.achievementRepository = achievementRepository;
    this.unlockRepository = unlockRepository;
    this.catchRepository = catchRepository;
    this.friendshipRepository = friendshipRepository;
    this.notificationService = notificationService;
  }

  /**
   * Sprawdza i odblokowuje osiagniecia po nowym polowaniu.
   *
   * @param user uzytkownik ktory zlopowal psa
   * @param caughtBreedRarity rarity score nowo zlapanej rasy (1-5)
   */
  @Async("taskExecutor")
  @Transactional
  public void checkAfterCatch(User user, int caughtBreedRarity) {
    log.debug("Checking achievements for userId={}", user.getId());

    List<Achievement> allAchievements = achievementRepository.findAll();

    for (Achievement achievement : allAchievements) {
      if (unlockRepository.existsByUserIdAndAchievementCode(
          user.getId(), achievement.getCode())) {
        continue;
      }

      boolean unlocked = evaluateCondition(user, achievement, caughtBreedRarity);

      if (unlocked) {
        AchievementUnlock unlock = AchievementUnlock.of(user, achievement);
        unlockRepository.save(unlock);
        log.info(
            "Achievement unlocked: userId={}, code={}", user.getId(), achievement.getCode());

        notificationService.notifyAchievement(
            user, achievement.getNamePl(), achievement.getCode());
      }
    }
  }

  /**
   * Sprawdza warunek achievementu dla uzytkownika.
   *
   * @param user uzytkownik
   * @param achievement definicja achievementu
   * @param caughtBreedRarity rarity score nowo zlapanej rasy
   * @return true jesli warunek jest spelniony
   */
  private boolean evaluateCondition(User user, Achievement achievement, int caughtBreedRarity) {
    ConditionType conditionType;
    try {
      conditionType = ConditionType.fromDbValue(achievement.getConditionType());
    } catch (IllegalArgumentException e) {
      log.warn(
          "Unknown condition_type '{}' for achievement code={}",
          achievement.getConditionType(),
          achievement.getCode());
      return false;
    }

    return switch (conditionType) {
      case TOTAL_CATCHES -> user.getTotalCatches() >= achievement.getConditionValue();
      case UNIQUE_BREEDS -> user.getUniqueBreeds() >= achievement.getConditionValue();
      case RARE_CATCH -> caughtBreedRarity >= achievement.getConditionValue();
      case FRIENDS_COUNT -> {
        long friendCount = friendshipRepository.countAcceptedFriends(user.getId());
        yield friendCount >= achievement.getConditionValue();
      }
      case STREAK -> {
        int streak = calculateStreak(user);
        yield streak >= achievement.getConditionValue();
      }
    };
  }

  /**
   * Oblicza aktualny streak uzytkownika (liczba kolejnych dni z co najmniej 1 catch).
   *
   * <p>Pobiera daty wszystkich zlowien (DISTINCT DATE) posortowane malejaco. Liczy od dzisiaj
   * wstecz ile kolejnych dni ma co najmniej jeden catch.
   *
   * @param user uzytkownik
   * @return dlugosc aktualnego streaka w dniach (min 1 jesli jest catch dzisiaj)
   */
  private int calculateStreak(User user) {
    List<Date> catchDates = catchRepository.findDistinctCatchDatesByUserId(user.getId());
    if (catchDates.isEmpty()) {
      return 0;
    }

    LocalDate today = LocalDate.now();
    int streak = 0;
    LocalDate expected = today;

    for (Date sqlDate : catchDates) {
      LocalDate catchDate = sqlDate.toLocalDate();
      if (catchDate.isEqual(expected)) {
        streak++;
        expected = expected.minusDays(1);
      } else if (catchDate.isBefore(expected)) {
        break;
      }
    }

    return streak;
  }
}
