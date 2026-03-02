package com.petsapp.achievement;

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
import org.hibernate.annotations.CreationTimestamp;

/**
 * Encja reprezentujaca odblokowanie achievementu przez uzytkownika.
 *
 * <p>Unikalny constraint (user_id, achievement_id) zapobiega podwojnemu odblokowaniu.
 */
@Entity
@Table(name = "achievement_unlock")
public class AchievementUnlock {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "achievement_id", nullable = false)
  private Achievement achievement;

  @CreationTimestamp
  @Column(name = "unlocked_at", nullable = false, updatable = false)
  private Instant unlockedAt;

  protected AchievementUnlock() {}

  private AchievementUnlock(User user, Achievement achievement) {
    this.user = user;
    this.achievement = achievement;
  }

  public static AchievementUnlock of(User user, Achievement achievement) {
    return new AchievementUnlock(user, achievement);
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public Achievement getAchievement() {
    return achievement;
  }

  public Instant getUnlockedAt() {
    return unlockedAt;
  }
}
