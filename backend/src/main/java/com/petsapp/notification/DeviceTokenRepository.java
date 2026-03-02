package com.petsapp.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium dla encji {@link DeviceToken}.
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

  /** Zwraca wszystkie tokeny danego uzytkownika — uzywane przy wysylaniu push. */
  List<DeviceToken> findAllByUserId(UUID userId);

  /** Szuka tokenu po wartosci — do sprawdzenia duplikatu przy rejestracji. */
  Optional<DeviceToken> findByToken(String token);

  /** Usuwa token po wartosci (wyrejestrowanie urzadzenia). */
  void deleteByToken(String token);

  /** Usuwa wszystkie tokeny uzytkownika (logout, soft delete konta). */
  void deleteAllByUserId(UUID userId);
}
