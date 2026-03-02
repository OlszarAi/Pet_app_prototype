package com.petsapp.catch_;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repozytorium dla encji {@link Comment}.
 *
 * <p>Paginacja oparta na kursorze — cursor to created_at ostatniego elementu poprzedniej strony.
 * Soft-deleted komentarze sa uwzglednianie w wynikach, ale CatchService ukrywa ich tresc.
 */
public interface CommentRepository extends JpaRepository<Comment, UUID> {

  /**
   * Zwraca pierwsza strone komentarzy dla polowania (brak kursora).
   *
   * @param catchId ID polowania
   * @param limit max liczba wynikow
   * @return lista komentarzy posortowanych malejaco po created_at
   */
  @Query(
      """
      SELECT c FROM Comment c
      WHERE c.dogCatch.id = :catchId
      ORDER BY c.createdAt DESC
      LIMIT :limit
      """)
  List<Comment> findFirstPage(@Param("catchId") UUID catchId, @Param("limit") int limit);

  /**
   * Zwraca kolejna strone komentarzy dla polowania (z kursorem).
   *
   * @param catchId ID polowania
   * @param cursor wartosc created_at ostatniego elementu poprzedniej strony
   * @param limit max liczba wynikow
   * @return lista komentarzy posortowanych malejaco po created_at
   */
  @Query(
      """
      SELECT c FROM Comment c
      WHERE c.dogCatch.id = :catchId
        AND c.createdAt < :cursor
      ORDER BY c.createdAt DESC
      LIMIT :limit
      """)
  List<Comment> findNextPage(
      @Param("catchId") UUID catchId,
      @Param("cursor") Instant cursor,
      @Param("limit") int limit);
}
