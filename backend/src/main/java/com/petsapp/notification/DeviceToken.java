package com.petsapp.notification;

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
 * Encja reprezentujaca token FCM urzadzenia mobilnego uzytkownika.
 *
 * <p>Uzytkownik moze miec wiele tokenow (wiele urzadzen). Token jest unikalny globalnie.
 * Usuwany automatycznie gdy FCM zwroci blad UNREGISTERED.
 */
@Entity
@Table(name = "device_token")
public class DeviceToken {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token", nullable = false, unique = true, length = 512)
  private String token;

  @Column(name = "platform", nullable = false, length = 10)
  private String platform;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected DeviceToken() {}

  private DeviceToken(User user, String token, String platform) {
    this.user = user;
    this.token = token;
    this.platform = platform;
  }

  public static DeviceToken of(User user, String token, String platform) {
    return new DeviceToken(user, token, platform);
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getToken() {
    return token;
  }

  public String getPlatform() {
    return platform;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
