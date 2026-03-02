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
 * Encja reprezentujaca zgloszenie polowania jako nieodpowiednie.
 *
 * <p>Mapuje tabele {@code report} z V1. Pole {@code catchId} jest nullable — tabela obsluguje
 * rowniez zgloszenia profili uzytkownikow (user_id), ale ten endpoint jest implementowany tutaj.
 */
@Entity
@Table(name = "report")
public class CatchReport {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "catch_id")
  private DogCatch dogCatch;

  @Column(name = "reason", nullable = false, length = 50)
  private String reason;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected CatchReport() {}

  private CatchReport(User reporter, DogCatch dogCatch, String reason, String description) {
    this.reporter = reporter;
    this.dogCatch = dogCatch;
    this.reason = reason;
    this.description = description;
    this.status = "pending";
    this.createdAt = Instant.now();
  }

  public static CatchReport of(User reporter, DogCatch dogCatch, String reason, String description) {
    return new CatchReport(reporter, dogCatch, reason, description);
  }

  public UUID getId() {
    return id;
  }

  public User getReporter() {
    return reporter;
  }

  public DogCatch getDogCatch() {
    return dogCatch;
  }

  public String getReason() {
    return reason;
  }

  public String getDescription() {
    return description;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
