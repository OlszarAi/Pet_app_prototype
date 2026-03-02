package com.petsapp.friend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repozytorium dla encji {@link Friendship}.
 *
 * <p>Zapytania sprawdzaja oba kierunki (requester/addressee), bo relacja nie jest symetryczna
 * w bazie danych, ale logicznie jest — znajomosci sa obustronne po akceptacji.
 */
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

  /**
   * Szuka dowolnej relacji miedzy dwoma uzytkownikami (w obu kierunkach).
   * Uzywane do sprawdzenia czy znajomosc juz istnieje przed wysylaniem prosby.
   */
  @Query(
      """
      SELECT f FROM Friendship f
      WHERE (f.requester.id = :a AND f.addressee.id = :b)
         OR (f.requester.id = :b AND f.addressee.id = :a)
      """)
  Optional<Friendship> findBetween(@Param("a") UUID a, @Param("b") UUID b);

  /**
   * Zwraca prosby o znajomosc oczekujace na odpowiedz danego uzytkownika (addressee).
   */
  @Query(
      """
      SELECT f FROM Friendship f
      JOIN FETCH f.requester
      WHERE f.addressee.id = :userId AND f.status = com.petsapp.friend.FriendshipStatus.PENDING
      ORDER BY f.createdAt DESC
      """)
  List<Friendship> findPendingRequestsFor(@Param("userId") UUID userId);

  /**
   * Zwraca wszystkich zaakceptowanych znajomych uzytkownika (obie strony relacji).
   */
  @Query(
      """
      SELECT f FROM Friendship f
      JOIN FETCH f.requester
      JOIN FETCH f.addressee
      WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
        AND f.status = com.petsapp.friend.FriendshipStatus.ACCEPTED
      ORDER BY f.createdAt DESC
      """)
  List<Friendship> findAcceptedFriendships(@Param("userId") UUID userId);

  /**
   * Zwraca liste ID zaakceptowanych znajomych uzytkownika.
   * Uzywane przez FeedService do budowania friends feed.
   */
  @Query(
      """
      SELECT
        CASE WHEN f.requester.id = :userId THEN f.addressee.id
             ELSE f.requester.id
        END
      FROM Friendship f
      WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
        AND f.status = com.petsapp.friend.FriendshipStatus.ACCEPTED
      """)
  List<UUID> findFriendIds(@Param("userId") UUID userId);

  /**
   * Leaderboard: zaakceptowani znajomi posortowani malejaco po unique_breeds.
   * Uzywane dla GET /friends/leaderboard.
   */
  @Query(
      """
      SELECT
        CASE WHEN f.requester.id = :userId THEN f.addressee
             ELSE f.requester
        END
      FROM Friendship f
      WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
        AND f.status = com.petsapp.friend.FriendshipStatus.ACCEPTED
      ORDER BY
        CASE WHEN f.requester.id = :userId THEN f.addressee.uniqueBreeds
             ELSE f.requester.uniqueBreeds
        END DESC
      """)
  List<com.petsapp.auth.User> findLeaderboard(@Param("userId") UUID userId);

  /**
   * Liczy zaakceptowanych znajomych uzytkownika. Uzywane przez AchievementChecker.
   */
  @Query(
      """
      SELECT COUNT(f) FROM Friendship f
      WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
        AND f.status = com.petsapp.friend.FriendshipStatus.ACCEPTED
      """)
  long countAcceptedFriends(@Param("userId") UUID userId);
}
