package com.petsapp.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Ustawienia uzytkownika — relacja 1:1 z User.
 *
 * <p>Rekord tworzony automatycznie przy rejestracji z wartosciami domyslnymi. UUID jest
 * wspoldzielone z encja User (@MapsId).
 */
@Entity
@Table(name = "user_settings")
public class UserSettings {

  @Id
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "push_likes", nullable = false)
  private boolean pushLikes = true;

  @Column(name = "push_comments", nullable = false)
  private boolean pushComments = true;

  @Column(name = "push_friend_requests", nullable = false)
  private boolean pushFriendRequests = true;

  @Column(name = "push_achievements", nullable = false)
  private boolean pushAchievements = true;

  @Column(name = "language", nullable = false, length = 5)
  private String language = "pl";

  @Column(name = "dark_mode", nullable = false)
  private boolean darkMode = false;

  protected UserSettings() {}

  public UserSettings(User user) {
    this.user = user;
  }

  public UUID getUserId() {
    return userId;
  }

  public User getUser() {
    return user;
  }

  public boolean isPushLikes() {
    return pushLikes;
  }

  public boolean isPushComments() {
    return pushComments;
  }

  public boolean isPushFriendRequests() {
    return pushFriendRequests;
  }

  public boolean isPushAchievements() {
    return pushAchievements;
  }

  public String getLanguage() {
    return language;
  }

  public boolean isDarkMode() {
    return darkMode;
  }

  public void update(
      boolean pushLikes,
      boolean pushComments,
      boolean pushFriendRequests,
      boolean pushAchievements,
      String language,
      boolean darkMode) {
    this.pushLikes = pushLikes;
    this.pushComments = pushComments;
    this.pushFriendRequests = pushFriendRequests;
    this.pushAchievements = pushAchievements;
    this.language = language;
    this.darkMode = darkMode;
  }
}
