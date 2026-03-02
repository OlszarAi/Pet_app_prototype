package com.petsapp.catch_;

import com.petsapp.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Encja reprezentujaca polubienie polowania.
 *
 * <p>Unikalne ograniczenie (user_id, catch_id) jest zdefiniowane w schemacie V1. Podwojne
 * polubienie przez tego samego uzytkownika rzuca ConstraintViolationException, kktora jest
 * mapowana na HTTP 409 przez GlobalExceptionHandler.
 */
@Entity
@Table(name = "\"like\"")
public class Like {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "catch_id", nullable = false)
  private DogCatch dogCatch;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Like() {}

  private Like(User user, DogCatch dogCatch) {
    this.user = user;
    this.dogCatch = dogCatch;
    this.createdAt = Instant.now();
  }

  public static Like of(User user, DogCatch dogCatch) {
    return new Like(user, dogCatch);
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public DogCatch getDogCatch() {
    return dogCatch;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
