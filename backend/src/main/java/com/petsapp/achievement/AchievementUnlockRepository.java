package com.petsapp.achievement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repozytorium dla encji {@link AchievementUnlock}.
 */
public interface AchievementUnlockRepository extends JpaRepository<AchievementUnlock, UUID> {

  /** Sprawdza czy uzytkownik ma juz dany achievement (unikat). */
  boolean existsByUserIdAndAchievementCode(UUID userId, String achievementCode);

  /** Zwraca liste odblokowanych achievementow uzytkownika posortowanych po dacie. */
  @Query(
      """
      SELECT au FROM AchievementUnlock au
      WHERE au.user.id = :userId
      ORDER BY au.unlockedAt DESC
      """)
  List<AchievementUnlock> findByUserIdOrdered(@Param("userId") UUID userId);

  /** Szuka konkretnego odblokowania. */
  Optional<AchievementUnlock> findByUserIdAndAchievementCode(UUID userId, String achievementCode);

  /** Liczy osiagniecia uzytkownika. */
  long countByUserId(UUID userId);
}
