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
 * Jednorazowy token do resetowania hasla.
 *
 * <p>Zapisujemy hash tokena (SHA-256), nie raw wartosc — tak samo jak refresh_token. Token jest
 * wazny przez 1 godzine. Po uzyciu jest oznaczany used_at, co sprawia ze nie moze byc uzyty po raz
 * drugi.
 */
@Entity
@Table(name = "password_reset")
public class PasswordReset {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 255)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PasswordReset() {}

  private PasswordReset(Builder builder) {
    this.user = builder.user;
    this.tokenHash = builder.tokenHash;
    this.expiresAt = builder.expiresAt;
  }

  public static Builder builder() {
    return new Builder();
  }

  public User getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  /** Sprawdza czy token wygasl (biezacy czas jest po expiresAt). */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  /** Sprawdza czy token byl juz uzyty. */
  public boolean isUsed() {
    return usedAt != null;
  }

  /** Oznacza token jako uzyty — uniemozliwia ponowne uzycie. */
  public void markUsed() {
    this.usedAt = Instant.now();
  }

  public static final class Builder {
    private User user;
    private String tokenHash;
    private Instant expiresAt;

    private Builder() {}

    public Builder user(User user) {
      this.user = user;
      return this;
    }

    public Builder tokenHash(String tokenHash) {
      this.tokenHash = tokenHash;
      return this;
    }

    public Builder expiresAt(Instant expiresAt) {
      this.expiresAt = expiresAt;
      return this;
    }

    public PasswordReset build() {
      return new PasswordReset(this);
    }
  }
}
