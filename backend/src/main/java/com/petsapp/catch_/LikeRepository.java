package com.petsapp.catch_;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium dla encji {@link Like}.
 *
 * <p>Uzywane do sprawdzania czy dany uzytkownik polubil catch oraz do usuwania polubienia.
 */
public interface LikeRepository extends JpaRepository<Like, UUID> {

  Optional<Like> findByUserIdAndDogCatchId(UUID userId, UUID catchId);

  boolean existsByUserIdAndDogCatchId(UUID userId, UUID catchId);
}
