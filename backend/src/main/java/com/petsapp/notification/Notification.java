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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Encja reprezentujaca powiadomienie in-app dla uzytkownika.
 *
 * <p>data_json przechowuje dodatkowy payload specyficzny dla typu notyfikacji:
 * catchId, friendshipId, achievementCode itp. Pole jest opcjonalne.
 */
@Entity
@Table(name = "notification")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "type", nullable = false, length = 50)
  private String type;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "body", length = 500)
  private String body;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data_json", columnDefinition = "jsonb")
  private String dataJson;

  @Column(name = "is_read", nullable = false)
  private boolean isRead = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Notification() {}

  private Notification(Builder builder) {
    this.user = builder.user;
    this.type = builder.type;
    this.title = builder.title;
    this.body = builder.body;
    this.dataJson = builder.dataJson;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void markRead() {
    this.isRead = true;
  }

  public UUID getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getDataJson() {
    return dataJson;
  }

  public boolean isRead() {
    return isRead;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public static final class Builder {
    private User user;
    private String type;
    private String title;
    private String body;
    private String dataJson;

    private Builder() {}

    public Builder user(User user) {
      this.user = user;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder body(String body) {
      this.body = body;
      return this;
    }

    public Builder dataJson(String dataJson) {
      this.dataJson = dataJson;
      return this;
    }

    public Notification build() {
      return new Notification(this);
    }
  }
}
