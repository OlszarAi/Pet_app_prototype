package com.petsapp.friend;

import com.petsapp.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Encja reprezentujaca relacje znajomosci miedzy uzytkownikami.
 *
 * <p>Kierunkowosc: requester wysyla prosbe, addressee ja akceptuje/odrzuca.
 * Blokowanie (BLOCKED) jest jednostronne — requester blokuje addressee.
 * Constraint DB gwarantuje unikalnosc pary (requester_id, addressee_id).
 */
@Entity
@Table(name = "friendship")
public class Friendship {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requester_id", nullable = false)
  private User requester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "addressee_id", nullable = false)
  private User addressee;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private FriendshipStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Friendship() {}

  private Friendship(User requester, User addressee, FriendshipStatus status) {
    this.requester = requester;
    this.addressee = addressee;
    this.status = status;
  }

  public static Friendship pending(User requester, User addressee) {
    return new Friendship(requester, addressee, FriendshipStatus.PENDING);
  }

  public static Friendship blocked(User requester, User addressee) {
    return new Friendship(requester, addressee, FriendshipStatus.BLOCKED);
  }

  public void accept() {
    this.status = FriendshipStatus.ACCEPTED;
  }

  public UUID getId() {
    return id;
  }

  public User getRequester() {
    return requester;
  }

  public User getAddressee() {
    return addressee;
  }

  public FriendshipStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
