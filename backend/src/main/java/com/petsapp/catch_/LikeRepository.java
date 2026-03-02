package com.petsapp.catch_;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repozytorium dla encji {@link Like}.
 *
 * <p>Uzywane do sprawdzania czy dany uzytkownik polubil catch oraz do usuwania polubienia.
 */
public interface LikeRepository extends JpaRepository<Like, UUID> {

  Optional<Like> findByUserIdAndDogCatchId(UUID userId, UUID catchId);

  boolean existsByUserIdAndDogCatchId(UUID userId, UUID catchId);

  /**
   * Zwraca zbior UUID catchy polubionych przez uzytkownika z podanej listy.
   *
   * <p>Jedno zapytanie zamiast N — uzywane przy budowaniu stron feeda do oznaczenia pola
   * {@code liked} w {@link com.petsapp.catch_.CatchResponse} bez N+1.
   *
   * @param userId ID uzytkownika
   * @param catchIds lista UUID catchy na aktualnej stronie
   * @return zbior UUID catchy ktore dany uzytkownik polubil
   */
  @Query(
      """
      SELECT l.dogCatch.id FROM Like l
      WHERE l.user.id = :userId
        AND l.dogCatch.id IN :catchIds
      """)
  Set<UUID> findLikedCatchIdsByUserIdIn(
      @Param("userId") UUID userId, @Param("catchIds") Collection<UUID> catchIds);
}
