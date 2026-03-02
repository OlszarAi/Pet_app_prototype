package com.petsapp.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Encja reprezentujaca uzytkownika aplikacji.
 *
 * <p>password_hash jest nullable — uzytkownicy OAuth nie maja hasla lokalnego. Soft delete przez
 * deleted_at zamiast fizycznego usuniecia (GDPR + mozliwosc przywrocenia konta).
 */
@Entity
@Table(name = "\"user\"")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "username", nullable = false, unique = true, length = 30)
  private String username;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  /** Null dla uzytkownikow zarejestrowanych przez OAuth. */
  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(name = "bio", length = 150)
  private String bio;

  @Column(name = "total_catches", nullable = false)
  private int totalCatches = 0;

  @Column(name = "unique_breeds", nullable = false)
  private int uniqueBreeds = 0;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name = "is_private", nullable = false)
  private boolean isPrivate = false;

  @Column(name = "oauth_provider", length = 20)
  private String oauthProvider;

  @Column(name = "oauth_id", length = 255)
  private String oauthId;

  /** Null oznacza aktywne konto. Ustawiane przy soft delete. */
  @Column(name = "deleted_at")
  private Instant deletedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected User() {}

  private User(Builder builder) {
    this.username = builder.username;
    this.email = builder.email;
    this.passwordHash = builder.passwordHash;
    this.oauthProvider = builder.oauthProvider;
    this.oauthId = builder.oauthId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public String getBio() {
    return bio;
  }

  public int getTotalCatches() {
    return totalCatches;
  }

  public int getUniqueBreeds() {
    return uniqueBreeds;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public String getOauthProvider() {
    return oauthProvider;
  }

  public String getOauthId() {
    return oauthId;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markEmailVerified() {
    this.emailVerified = true;
  }

  public void softDelete() {
    this.deletedAt = Instant.now();
  }

  public void updatePassword(String newPasswordHash) {
    this.passwordHash = newPasswordHash;
  }

  public void updateProfile(String username, String bio) {
    if (username != null) {
      this.username = username;
    }
    if (bio != null) {
      this.bio = bio;
    }
  }

  public void updateAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  /**
   * Laczy istniejace konto emailowe z kontem OAuth.
   *
   * <p>Uzywane gdy uzytkownik zarejestrowany przez email loguje sie po raz pierwszy przez Google.
   * Pozwala uzywac obu metod logowania na tym samym koncie.
   */
  public void linkOAuth(String provider, String oauthId) {
    this.oauthProvider = provider;
    this.oauthId = oauthId;
  }

  /**
   * Zmienia widocznosc profilu (publiczny / prywatny).
   *
   * <p>Prywatny profil ukrywa Pokedex i catche przed innymi uzytkownikami.
   */
  public void updatePrivacy(boolean isPrivate) {
    this.isPrivate = isPrivate;
  }

  /**
   * Inkrementuje denormalizowany licznik catchy.
   *
   * <p>Wywolywane po pomyslnym zapisaniu nowego DogCatch w CatchService. Denormalizacja pozwala
   * na szybkie wyswietlanie statystyk profilu bez COUNT(*) na dog_catch.
   */
  public void incrementTotalCatches() {
    this.totalCatches++;
  }

  /** Dekrementuje denormalizowany licznik catchy (po soft delete). */
  public void decrementTotalCatches() {
    if (this.totalCatches > 0) {
      this.totalCatches--;
    }
  }

  /**
   * Inkrementuje licznik unikalnych ras.
   *
   * <p>Wywolywane przez CatchService gdy nowy catch jest pierwszym dla danej rasy.
   */
  public void incrementUniqueBreeds() {
    this.uniqueBreeds++;
  }

  public static final class Builder {
    private String username;
    private String email;
    private String passwordHash;
    private String oauthProvider;
    private String oauthId;

    private Builder() {}

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder passwordHash(String passwordHash) {
      this.passwordHash = passwordHash;
      return this;
    }

    public Builder oauthProvider(String oauthProvider) {
      this.oauthProvider = oauthProvider;
      return this;
    }

    public Builder oauthId(String oauthId) {
      this.oauthId = oauthId;
      return this;
    }

    public User build() {
      return new User(this);
    }
  }
}
