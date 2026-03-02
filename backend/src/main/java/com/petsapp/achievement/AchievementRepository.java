package com.petsapp.achievement;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium dla encji {@link Achievement}.
 */
public interface AchievementRepository extends JpaRepository<Achievement, Integer> {

  Optional<Achievement> findByCode(String code);
}
