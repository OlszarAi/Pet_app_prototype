package com.petsapp.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repozytorium dla encji {@link Notification}.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  /**
   * Zwraca stronicowana liste powiadomien uzytkownika (najnowsze pierwsze).
   */
  @Query(
      """
      SELECT n FROM Notification n
      WHERE n.user.id = :userId
      ORDER BY n.createdAt DESC
      LIMIT :limit
      """)
  List<Notification> findByUserIdLatest(@Param("userId") UUID userId, @Param("limit") int limit);

  /**
   * Liczy nieprzeczytane powiadomienia uzytkownika.
   */
  @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
  long countUnread(@Param("userId") UUID userId);

  /**
   * Oznacza wszystkie powiadomienia uzytkownika jako przeczytane.
   */
  @Modifying
  @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
  int markAllReadByUserId(@Param("userId") UUID userId);

  /**
   * Zwraca najnozsza date powiadomienia danego typu dla uzytkownika.
   * Uzywane przez logike batch-grouping dla lajkow.
   */
  @Query(
      """
      SELECT n FROM Notification n
      WHERE n.user.id = :userId AND n.type = :type
      ORDER BY n.createdAt DESC
      LIMIT 1
      """)
  java.util.Optional<Notification> findLatestByUserIdAndType(
      @Param("userId") UUID userId, @Param("type") String type);
}
