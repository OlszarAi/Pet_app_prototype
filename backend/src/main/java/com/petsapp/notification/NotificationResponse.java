package com.petsapp.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO odpowiedzi dla powiadomienia.
 */
public record NotificationResponse(
    UUID id,
    String type,
    String title,
    String body,
    String dataJson,
    boolean isRead,
    Instant createdAt) {

  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getType(),
        notification.getTitle(),
        notification.getBody(),
        notification.getDataJson(),
        notification.isRead(),
        notification.getCreatedAt());
  }
}
