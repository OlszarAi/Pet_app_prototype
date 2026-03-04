package com.petsapp.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  /** Szuka aktywnych uzytkownikow (nie soft-deleted) po emailu. */
  @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
  Optional<User> findActiveByEmail(@Param("email") String email);

  /** Szuka aktywnych uzytkownikow po username. */
  @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
  Optional<User> findActiveByUsername(@Param("username") String username);

  @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
  boolean existsActiveByEmail(@Param("email") String email);

  @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
  boolean existsActiveByUsername(@Param("username") String username);

  @Modifying
  @Query("UPDATE User u SET u.deletedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
  void softDeleteById(@Param("id") UUID id);

  /** Szuka aktywnego uzytkownika po dostawcy OAuth i jego unikalnym ID. */
  @Query(
      "SELECT u FROM User u WHERE u.oauthProvider = :provider AND u.oauthId = :oauthId AND u.deletedAt IS NULL")
  Optional<User> findByOauthProviderAndOauthId(
      @Param("provider") String provider, @Param("oauthId") String oauthId);

  /**
   * Wyszukuje aktywnych uzytkownikow ktorych username zaczyna sie od podanego prefiksu
   * (case-insensitive). Wyniki sa posortowane alfabetycznie i ograniczone do podanego limitu.
   */
  @Query(
      value =
          "SELECT * FROM \"user\" WHERE lower(username) LIKE lower(concat(:prefix, '%'))"
              + " AND deleted_at IS NULL ORDER BY username LIMIT :limit",
      nativeQuery = true)
  List<User> searchActiveByUsernamePrefix(
      @Param("prefix") String prefix, @Param("limit") int limit);
}
