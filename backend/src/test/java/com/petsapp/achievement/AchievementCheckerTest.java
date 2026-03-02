package com.petsapp.achievement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.petsapp.AbstractIntegrationTest;
import com.petsapp.auth.AuthService;
import com.petsapp.auth.EmailService;
import com.petsapp.auth.RegisterRequest;
import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import com.petsapp.auth.VerifyEmailRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla AchievementChecker.
 *
 * <p>AchievementChecker jest @Async — po wywolaniu metody czekamy krotko (Thread.sleep)
 * na wykonanie w puli watkow przed weryfikacja stanu bazy.
 *
 * <p>V4 migracja automatycznie seeduje 9 achievementow. Testy weryfikuja:
 * <ul>
 *   <li>Odblokowanie FIRST_CATCH po pierwszym polowaniu (total_catches=1)
 *   <li>Odblokowanie RARE_HUNTER dla rzadkiego zlowienia (rare_catch>=4)
 *   <li>Brak duplikatu jesli achievement juz odblokowany
 *   <li>Brak odblokowania przy niespenioonym warunku
 * </ul>
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AchievementCheckerTest extends AbstractIntegrationTest {

  @Autowired private AchievementChecker achievementChecker;
  @Autowired private AchievementUnlockRepository unlockRepository;
  @Autowired private AchievementRepository achievementRepository;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;

  @MockBean private EmailService emailService;

  @BeforeEach
  void setupMocks() {
    doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  private User createVerifiedUser() {
    String email = "achiev-" + UUID.randomUUID() + "@example.com";
    String username = "ac" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    String code = "111222";
    when(emailService.generateVerificationCode()).thenReturn(code);
    when(emailService.verificationCodeExpiry()).thenReturn(Instant.now().plusSeconds(900));

    authService.register(new RegisterRequest(email, username, "Password123!"));
    authService.verifyEmail(new VerifyEmailRequest(email, code));

    return userRepository.findActiveByEmail(email).orElseThrow();
  }

  @Test
  void checkAfterCatch_firstCatch_unlocksFirstCatchAchievement() throws InterruptedException {
    User user = createVerifiedUser();

    // Symuluj 1 polowanie przez inkrementacje denormalizowanego licznika
    user.incrementTotalCatches();
    userRepository.save(user);

    achievementChecker.checkAfterCatch(user, 1);
    Thread.sleep(600);

    List<AchievementUnlock> unlocks = unlockRepository.findByUserIdOrdered(user.getId());
    assertThat(unlocks).extracting(u -> u.getAchievement().getCode())
        .contains("FIRST_CATCH");
  }

  @Test
  void checkAfterCatch_rareCatch_unlocksRareHunterAchievement() throws InterruptedException {
    User user = createVerifiedUser();

    // RARE_HUNTER wymaga rare_catch >= 4 — przekazujemy rarity = 4
    achievementChecker.checkAfterCatch(user, 4);
    Thread.sleep(600);

    List<AchievementUnlock> unlocks = unlockRepository.findByUserIdOrdered(user.getId());
    assertThat(unlocks).extracting(u -> u.getAchievement().getCode())
        .contains("RARE_HUNTER");
  }

  @Test
  void checkAfterCatch_alreadyUnlocked_doesNotCreateDuplicate() throws InterruptedException {
    User user = createVerifiedUser();
    user.incrementTotalCatches();
    userRepository.save(user);

    // Wywolaj dwa razy — achievement powinien byc odblokowany tylko raz
    achievementChecker.checkAfterCatch(user, 1);
    Thread.sleep(600);
    achievementChecker.checkAfterCatch(user, 1);
    Thread.sleep(600);

    long firstCatchCount = unlockRepository.findByUserIdOrdered(user.getId()).stream()
        .filter(u -> "FIRST_CATCH".equals(u.getAchievement().getCode()))
        .count();
    assertThat(firstCatchCount).isEqualTo(1);
  }

  @Test
  void checkAfterCatch_conditionNotMet_doesNotUnlockAchievement() throws InterruptedException {
    User user = createVerifiedUser();

    // totalCatches = 0, rarity = 1 — zaden achievement nie powinien byc odblokowany
    achievementChecker.checkAfterCatch(user, 1);
    Thread.sleep(600);

    List<AchievementUnlock> unlocks = unlockRepository.findByUserIdOrdered(user.getId());
    assertThat(unlocks).isEmpty();
  }

  @Test
  void checkAfterCatch_tenCatches_unlocksTenCatchesAchievement() throws InterruptedException {
    User user = createVerifiedUser();

    // Ustaw totalCatches = 10
    for (int i = 0; i < 10; i++) {
      user.incrementTotalCatches();
    }
    userRepository.save(user);

    achievementChecker.checkAfterCatch(user, 1);
    Thread.sleep(600);

    List<AchievementUnlock> unlocks = unlockRepository.findByUserIdOrdered(user.getId());
    assertThat(unlocks).extracting(u -> u.getAchievement().getCode())
        .contains("FIRST_CATCH", "TEN_CATCHES");
  }

  @Test
  void checkAfterCatch_rarity5_unlocksRareHunterAndNotLower() throws InterruptedException {
    User user = createVerifiedUser();

    // RARE_HUNTER warunek: rare_catch >= 4
    achievementChecker.checkAfterCatch(user, 5);
    Thread.sleep(600);

    List<AchievementUnlock> unlocks = unlockRepository.findByUserIdOrdered(user.getId());
    // Z ras nie-catch powinien byc tylko RARE_HUNTER (nie SOCIAL, EXPLORER itd.)
    assertThat(unlocks).extracting(u -> u.getAchievement().getCode())
        .containsExactly("RARE_HUNTER");
  }

  @Test
  void allAchievements_seededCorrectly_nineAchievementsPresent() {
    List<Achievement> achievements = achievementRepository.findAll();

    assertThat(achievements).hasSize(9);
    assertThat(achievements).extracting(Achievement::getCode)
        .containsExactlyInAnyOrder(
            "FIRST_CATCH", "TEN_CATCHES", "FIFTY_CATCHES", "HUNDRED_CATCHES",
            "EXPLORER", "BREED_COLLECTOR", "RARE_HUNTER", "SOCIAL", "WEEK_STREAK");
  }
}
