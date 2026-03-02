package com.petsapp.catch_;

import java.time.Instant;
import java.util.Collection;
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

  /**
   * Zwraca paginowana liste catchy uzytkownika — pierwsza strona (bez kursora).
   *
   * <p>Uwzglednia widocznosc: dla wlasciciela zwraca wszystkie (isPublic ignorowane), dla innych
   * uzytkownikow filtr jest aplikowany przez serwis po stronie Javy.
   *
   * @param userId ID uzytkownika
   * @param limit max liczba wynikow
   * @return lista catchy posortowana malejaco po caught_at
   */
  @Query(
      """
      SELECT dc FROM DogCatch dc
      WHERE dc.user.id = :userId
        AND dc.deletedAt IS NULL
      ORDER BY dc.caughtAt DESC
      LIMIT :limit
      """)
  List<DogCatch> findFirstPageByUserId(@Param("userId") UUID userId, @Param("limit") int limit);

  /**
   * Zwraca paginowana liste catchy uzytkownika — kolejna strona (z kursorem).
   *
   * @param userId ID uzytkownika
   * @param cursor wartosc caught_at ostatniego elementu poprzedniej strony
   * @param limit max liczba wynikow
   * @return lista catchy posortowana malejaco po caught_at
   */
  @Query(
      """
      SELECT dc FROM DogCatch dc
      WHERE dc.user.id = :userId
        AND dc.deletedAt IS NULL
        AND dc.caughtAt < :cursor
      ORDER BY dc.caughtAt DESC
      LIMIT :limit
      """)
  List<DogCatch> findNextPageByUserId(
      @Param("userId") UUID userId,
      @Param("cursor") Instant cursor,
      @Param("limit") int limit);

  // =====================
  // Feed publiczny — fallback DB gdy Redis nie jest dostepny lub cache jest pusty
  // =====================

  /**
   * Pierwsza strona publicznego feeda z bazy danych (fallback gdy Redis pusty).
   *
   * <p>Wyniki posortowane malejaco po feed_score — ta sama kolejnosc co Redis Sorted Set.
   */
  @Query(
      """
      SELECT dc FROM DogCatch dc
      JOIN FETCH dc.breed
      WHERE dc.isPublic = true
        AND dc.deletedAt IS NULL
      ORDER BY dc.feedScore DESC
      LIMIT :limit
      """)
  List<DogCatch> findPublicFeedFirstPage(@Param("limit") int limit);

  /**
   * Kolejna strona publicznego feeda z kursorem feedScore (fallback DB).
   *
   * <p>Cursor to feedScore ostatniego elementu poprzedniej strony. Uzywamy kursorow zamiast OFFSET
   * aby uniknac dryftu paginacji przy zmieniajacych sie scorach.
   */
  @Query(
      """
      SELECT dc FROM DogCatch dc
      JOIN FETCH dc.breed
      WHERE dc.isPublic = true
        AND dc.deletedAt IS NULL
        AND dc.feedScore < :cursorScore
      ORDER BY dc.feedScore DESC
      LIMIT :limit
      """)
  List<DogCatch> findPublicFeedNextPage(
      @Param("cursorScore") double cursorScore, @Param("limit") int limit);

  /**
   * Pierwsza strona trending feeda — tylko polowania z ostatnich 24h.
   *
   * <p>Uzywana gdy cache Redis feed:trending jest pusty (zimny start lub po restarcie).
   *
   * @param since moment graniczny (zwykle now() - 24h)
   */
  @Query(
      """
      SELECT dc FROM DogCatch dc
      JOIN FETCH dc.breed
      WHERE dc.isPublic = true
        AND dc.deletedAt IS NULL
        AND dc.caughtAt >= :since
      ORDER BY dc.feedScore DESC
      LIMIT :limit
      """)
  List<DogCatch> findTrendingFirstPage(
      @Param("since") Instant since, @Param("limit") int limit);

  /**
   * Kolejna strona trending feeda z kursorem feedScore.
   *
   * @param since moment graniczny (now() - 24h)
   * @param cursorScore feedScore ostatniego elementu poprzedniej strony
   */
  @Query(
      """
      SELECT dc FROM DogCatch dc
      JOIN FETCH dc.breed
      WHERE dc.isPublic = true
        AND dc.deletedAt IS NULL
        AND dc.caughtAt >= :since
        AND dc.feedScore < :cursorScore
      ORDER BY dc.feedScore DESC
      LIMIT :limit
      """)
  List<DogCatch> findTrendingNextPage(
      @Param("since") Instant since,
      @Param("cursorScore") double cursorScore,
      @Param("limit") int limit);

  /**
   * Pierwsza strona feeda znajomych — chronologiczna, tylko zaakceptowane znajomosci.
   *
   * <p>Uzywamy natywnego SQL bo encja Friendship (Krok 8) jeszcze nie istnieje jako JPA entity.
   * JOIN z tabel friendship (V1__init_schema.sql) pobiera catche obustronnie — A→B i B→A.
   *
   * @param userId ID zalogowanego uzytkownika
   */
  @Query(
      value =
          """
          SELECT dc.* FROM dog_catch dc
          INNER JOIN friendship f
            ON (f.requester_id = :userId AND f.addressee_id = dc.user_id)
            OR (f.addressee_id = :userId AND f.requester_id = dc.user_id)
          WHERE f.status = 'ACCEPTED'
            AND dc.deleted_at IS NULL
            AND dc.is_public = true
          ORDER BY dc.caught_at DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<DogCatch> findFriendsFirstPage(
      @Param("userId") UUID userId, @Param("limit") int limit);

  /**
   * Kolejna strona feeda znajomych z kursorem caught_at.
   *
   * @param userId ID zalogowanego uzytkownika
   * @param cursor caught_at ostatniego elementu poprzedniej strony (epoch millis)
   */
  @Query(
      value =
          """
          SELECT dc.* FROM dog_catch dc
          INNER JOIN friendship f
            ON (f.requester_id = :userId AND f.addressee_id = dc.user_id)
            OR (f.addressee_id = :userId AND f.requester_id = dc.user_id)
          WHERE f.status = 'ACCEPTED'
            AND dc.deleted_at IS NULL
            AND dc.is_public = true
            AND dc.caught_at < :cursor
          ORDER BY dc.caught_at DESC
          LIMIT :limit
          """,
      nativeQuery = true)
  List<DogCatch> findFriendsNextPage(
      @Param("userId") UUID userId,
      @Param("cursor") Instant cursor,
      @Param("limit") int limit);

  /**
   * Wszystkie publiczne polowania od zadanego momentu — uzywane przez FeedRebuildJob.
   *
   * <p>JOIN FETCH breed laduje rase w jednym zapytaniu — unikamy N+1 przy obliczaniu scorow.
   *
   * @param since moment graniczny (zwykle now() - 7 dni)
   */
  @Query(
      """
      SELECT dc FROM DogCatch dc
      JOIN FETCH dc.breed
      WHERE dc.isPublic = true
        AND dc.deletedAt IS NULL
        AND dc.caughtAt >= :since
      ORDER BY dc.caughtAt DESC
      """)
  List<DogCatch> findAllPublicForFeedRebuild(@Param("since") Instant since);

  /**
   * Laduje catche po liscie UUID — uzywane do hydratacji wynikow z Redis Sorted Set.
   *
   * <p>JOIN FETCH breed eliminuje N+1 dla mapowania na CatchResponse (rarity_score potrzebne do
   * wyswietlenia). Kolejnosc wynikow jest nieokreslona — serwis sortuje po ID list.
   *
   * @param ids lista UUID catchy do zaladowania
   */
  @Query(
      """
      SELECT dc FROM DogCatch dc
      JOIN FETCH dc.breed
      JOIN FETCH dc.user
      WHERE dc.id IN :ids
        AND dc.deletedAt IS NULL
      """)
  List<DogCatch> findAllByIdInWithBreedAndUser(@Param("ids") Collection<UUID> ids);
}
