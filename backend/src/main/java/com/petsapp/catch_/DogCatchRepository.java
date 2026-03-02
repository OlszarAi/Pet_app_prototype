package com.petsapp.catch_;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repozytorium dla encji {@link DogCatch}.
 *
 * <p>Krok 5 definiuje zapytania potrzebne dla Pokedex. Krok 6 doda metody dla feed, profilu i
 * upload flow.
 */
public interface DogCatchRepository extends JpaRepository<DogCatch, UUID> {

  /**
   * Zwraca zestawienie ras zlowionych przez uzytkownika: ID rasy + liczba zlowien, posortowane
   * malejaco po liczbie zlowien. Uzywane do wyswietlania Pokdex (odkryte rasy).
   */
  @Query(
      """
      SELECT dc.breed.id, COUNT(dc.id)
      FROM DogCatch dc
      WHERE dc.user.id = :userId
        AND dc.deletedAt IS NULL
      GROUP BY dc.breed.id
      ORDER BY COUNT(dc.id) DESC
      """)
  List<Object[]> findBreedCatchCountsByUserId(@Param("userId") UUID userId);

  /**
   * Zwraca ID rasy najczesciej spotykanej przez uzytkownika (ulubiona rasa w statystykach). Null
   * jezeli uzytkownik nie zlowoil jeszcze zadnego psa.
   */
  @Query(
      """
      SELECT dc.breed.id
      FROM DogCatch dc
      WHERE dc.user.id = :userId
        AND dc.deletedAt IS NULL
      GROUP BY dc.breed.id
      ORDER BY COUNT(dc.id) DESC
      LIMIT 1
      """)
  Integer findFavoriteBreedIdByUserId(@Param("userId") UUID userId);

  /** Calkowita liczba niezdeaktywowanych ZLowien uzytkownika. */
  @Query(
      """
      SELECT COUNT(dc) FROM DogCatch dc
      WHERE dc.user.id = :userId AND dc.deletedAt IS NULL
      """)
  long countCatchesByUserId(@Param("userId") UUID userId);

  /** Liczba unikalnych ras zlowionych przez uzytkownika. */
  @Query(
      """
      SELECT COUNT(DISTINCT dc.breed.id) FROM DogCatch dc
      WHERE dc.user.id = :userId AND dc.deletedAt IS NULL
      """)
  long countUniqueBreedsByUserId(@Param("userId") UUID userId);

  /** Globalna liczba unikalnych uzytkownikow, ktorzy zlowili dana rase. */
  @Query(
      """
      SELECT COUNT(DISTINCT dc.user.id) FROM DogCatch dc
      WHERE dc.breed.id = :breedId AND dc.deletedAt IS NULL
      """)
  long countCatchersByBreedId(@Param("breedId") int breedId);

  /** Globalna calkowita liczba zlowien danej rasy. */
  @Query(
      """
      SELECT COUNT(dc) FROM DogCatch dc
      WHERE dc.breed.id = :breedId AND dc.deletedAt IS NULL
      """)
  long countTotalCatchesByBreedId(@Param("breedId") int breedId);

  /**
   * Zwraca daty zlowien uzytkownika (dzien, bez godziny) posortowane malejaco. Uzywane do
   * obliczania streak w statystykach.
   */
  @Query(
      value =
          """
          SELECT DISTINCT CAST(caught_at AS DATE)
          FROM dog_catch
          WHERE user_id = :userId AND deleted_at IS NULL
          ORDER BY 1 DESC
          """,
      nativeQuery = true)
  List<java.sql.Date> findDistinctCatchDatesByUserId(@Param("userId") UUID userId);
}
