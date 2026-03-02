package com.petsapp.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, UUID> {

  Optional<PasswordReset> findByTokenHash(String tokenHash);

  /**
   * Unieważnia wszystkie poprzednie tokeny resetu hasla dla danego uzytkownika. Wywolywane przed
   * wystawieniem nowego tokena, zeby uniknac zalegajacych wpisow.
   */
  @Modifying
  @Query("DELETE FROM PasswordReset pr WHERE pr.user.id = :userId")
  void deleteAllByUserId(@Param("userId") UUID userId);
}
