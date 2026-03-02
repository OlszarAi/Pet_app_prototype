package com.petsapp.catch_;

import com.petsapp.auth.User;
import com.petsapp.breed.Breed;
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
 * Encja reprezentujaca "zlowienie" psa przez uzytkownika.
 *
 * <p>Encja jest tworzona w Kroku 6 (CatchController/Service). Juz teraz definiujemy pelna strukture
 * aby Pokedex (Krok 5) mogl wykonywac zapytania. Relacja do User i Breed przez lazy fetch — Pokedex
 * i Feed nie laduja pelnych obiektow, tylko ID.
 */
@Entity
@Table(name = "dog_catch")
public class DogCatch {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "breed_id", nullable = false)
  private Breed breed;

  @Column(name = "photo_url", nullable = false, length = 500)
  private String photoUrl;

  @Column(name = "thumbnail_url", nullable = false, length = 500)
  private String thumbnailUrl;

  @Column(name = "caption", length = 300)
  private String caption;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "location_name", length = 255)
  private String locationName;

  @Column(name = "is_public", nullable = false)
  private boolean isPublic;

  @Column(name = "like_count", nullable = false)
  private int likeCount;

  @Column(name = "comment_count", nullable = false)
  private int commentCount;

  @Column(name = "feed_score", nullable = false)
  private double feedScore;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "caught_at", nullable = false, updatable = false)
  private Instant caughtAt;

  protected DogCatch() {}

  private DogCatch(Builder builder) {
    this.user = builder.user;
    this.breed = builder.breed;
    this.photoUrl = builder.photoUrl;
    this.thumbnailUrl = builder.thumbnailUrl;
    this.caption = builder.caption;
    this.latitude = builder.latitude;
    this.longitude = builder.longitude;
    this.locationName = builder.locationName;
    this.isPublic = builder.isPublic;
    this.likeCount = 0;
    this.commentCount = 0;
    this.feedScore = 0.0;
    this.caughtAt = builder.caughtAt != null ? builder.caughtAt : java.time.Instant.now();
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

  public Breed getBreed() {
    return breed;
  }

  public String getPhotoUrl() {
    return photoUrl;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public String getCaption() {
    return caption;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public String getLocationName() {
    return locationName;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public int getLikeCount() {
    return likeCount;
  }

  public int getCommentCount() {
    return commentCount;
  }

  public double getFeedScore() {
    return feedScore;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public Instant getCaughtAt() {
    return caughtAt;
  }

  /** Soft delete — ustawia deleted_at. Catch przestaje byc widoczny w feedzie i profilach. */
  public void softDelete() {
    this.deletedAt = Instant.now();
  }

  /**
   * Inkrementuje denormalizowany licznik polubien.
   *
   * <p>Wywolywane po zapisaniu encji Like — denormalizacja eliminuje kosztowny COUNT(*) przy
   * kazdym renderowaniu karty.<br>
   * Dziala wylacznie wewnatrz transakcji (@Transactional w CatchService).
   */
  public void incrementLikeCount() {
    this.likeCount++;
  }

  /** Dekrementuje denormalizowany licznik polubien. */
  public void decrementLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }

  /** Inkrementuje denormalizowany licznik komentarzy. */
  public void incrementCommentCount() {
    this.commentCount++;
  }

  /** Dekrementuje denormalizowany licznik komentarzy. */
  public void decrementCommentCount() {
    if (this.commentCount > 0) {
      this.commentCount--;
    }
  }

  /**
   * Aktualizuje wyliczony score rankingowy feeda.
   *
   * <p>Wywolywane przez FeedRebuildJob co 15 minut oraz przy tworzeniu catcha. Wartosc sluzy jako
   * primary sort key w feedzie publicznym i Redis Sorted Set (feed:public, feed:trending).
   */
  public void updateFeedScore(double score) {
    this.feedScore = score;
  }

  /** Builder dla DogCatch — uzywany w testach i przez CatchService (Krok 6). */
  public static final class Builder {
    private User user;
    private Breed breed;
    private String photoUrl;
    private String thumbnailUrl;
    private String caption;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private boolean isPublic = true;
    private java.time.Instant caughtAt;

    private Builder() {}

    public Builder user(User user) {
      this.user = user;
      return this;
    }

    public Builder breed(Breed breed) {
      this.breed = breed;
      return this;
    }

    public Builder photoUrl(String photoUrl) {
      this.photoUrl = photoUrl;
      return this;
    }

    public Builder thumbnailUrl(String thumbnailUrl) {
      this.thumbnailUrl = thumbnailUrl;
      return this;
    }

    public Builder caption(String caption) {
      this.caption = caption;
      return this;
    }

    public Builder latitude(Double latitude) {
      this.latitude = latitude;
      return this;
    }

    public Builder longitude(Double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Builder locationName(String locationName) {
      this.locationName = locationName;
      return this;
    }

    public Builder isPublic(boolean isPublic) {
      this.isPublic = isPublic;
      return this;
    }

    public Builder caughtAt(java.time.Instant caughtAt) {
      this.caughtAt = caughtAt;
      return this;
    }

    public DogCatch build() {
      return new DogCatch(this);
    }
  }
}
