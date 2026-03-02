package com.petsapp.auth;

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
import org.hibernate.annotations.CreationTimestamp;

/**
 * Kod weryfikacji emaila wysylany przy rejestracji i na zadanie ponownego wyslania.
 *
 * <p>Kazdy uzytkownik moze miec wiele rekordow (np. po ponownym wyslaniu), ale tylko jeden
 * nieuzywany (used_at IS NULL) jest wazny. Stare kody sa logicznie nieaktywne przez expires_at.
 */
@Entity
@Table(name = "email_verification")
public class EmailVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  /** 6-znakowy numeryczny kod wysylany na email. */
  @Column(name = "code", nullable = false, length = 6)
  private String code;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** Null = nieuzywany. Ustawiane przy pomyslnej weryfikacji. */
  @Column(name = "used_at")
  private Instant usedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected EmailVerification() {}

  private EmailVerification(Builder builder) {
    this.user = builder.user;
    this.code = builder.code;
    this.expiresAt = builder.expiresAt;
  }

  public static Builder builder() {
    return new Builder();
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getCode() {
    return code;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  public boolean isUsed() {
    return usedAt != null;
  }

  public void markUsed() {
    this.usedAt = Instant.now();
  }

  public static final class Builder {
    private User user;
    private String code;
    private Instant expiresAt;

    private Builder() {}

    public Builder user(User user) {
      this.user = user;
      return this;
    }

    public Builder code(String code) {
      this.code = code;
      return this;
    }

    public Builder expiresAt(Instant expiresAt) {
      this.expiresAt = expiresAt;
      return this;
    }

    public EmailVerification build() {
      return new EmailVerification(this);
    }
  }
}
