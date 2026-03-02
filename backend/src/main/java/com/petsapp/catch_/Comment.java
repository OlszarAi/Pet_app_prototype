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
 * Encja reprezentujaca komentarz pod polowaniem.
 *
 * <p>Soft delete — usuniete komentarze maja deletedAt != null, tresc wciaz jest w bazie (zachowanie
 * watku), ale CatchService ukrywa ja przed klientem.
 */
@Entity
@Table(name = "comment")
public class Comment {

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

  @Column(name = "content", nullable = false, length = 500)
  private String content;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Comment() {}

  private Comment(User user, DogCatch dogCatch, String content) {
    this.user = user;
    this.dogCatch = dogCatch;
    this.content = content;
    this.createdAt = Instant.now();
  }

  public static Comment of(User user, DogCatch dogCatch, String content) {
    return new Comment(user, dogCatch, content);
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

  public String getContent() {
    return content;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void softDelete() {
    this.deletedAt = Instant.now();
  }
}
