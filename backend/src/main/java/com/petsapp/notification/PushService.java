package com.petsapp.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Serwis wysylki push notifications przez Firebase Cloud Messaging (FCM).
 *
 * <p>Metody sa oznaczone {@code @Async} — wywolanie nie blokuje watku HTTP.
 * Jesli Firebase nie jest skonfigurowany (firebase.enabled=false), wysylka jest pomijana z logiem
 * DEBUG. Ulatwia to developement i testy bez potrzeby credentials FCM.
 *
 * <p>W przypadku gdy FCM zwroci blad UNREGISTERED, token jest automatycznie usuwany z bazy.
 */
@Service
public class PushService {

  private static final Logger log = LoggerFactory.getLogger(PushService.class);

  private final DeviceTokenRepository deviceTokenRepository;

  public PushService(DeviceTokenRepository deviceTokenRepository) {
    this.deviceTokenRepository = deviceTokenRepository;
  }

  /**
   * Wysyla push notification do wszystkich tokenow uzytkownika.
   *
   * <p>Wywolanie jest asynchroniczne — metoda wraca natychmiast, wysylka dzieje sie w tle.
   * Bledy FCM dla konkretnych tokenow sa logowane i token jest usuwany jesli jest UNREGISTERED.
   *
   * @param userId ID uzytkownika do powiadomienia
   * @param title tytul powiadomienia
   * @param body tresc powiadomienia
   * @param dataKey opcjonalny klucz danych (np. "catchId")
   * @param dataValue opcjonalna wartosc danych
   */
  @Async("taskExecutor")
  public void sendToUser(
      java.util.UUID userId,
      String title,
      String body,
      String dataKey,
      String dataValue) {

    if (!isFirebaseInitialized()) {
      log.debug("Firebase not initialized — skipping push to userId={}", userId);
      return;
    }

    List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
    if (tokens.isEmpty()) {
      log.debug("No device tokens for userId={}", userId);
      return;
    }

    tokens.forEach(dt -> sendToToken(dt, title, body, dataKey, dataValue));
  }

  private void sendToToken(
      DeviceToken deviceToken,
      String title,
      String body,
      String dataKey,
      String dataValue) {

    try {
      Message.Builder messageBuilder =
          Message.builder()
              .setToken(deviceToken.getToken())
              .setNotification(
                  Notification.builder().setTitle(title).setBody(body).build());

      if (dataKey != null && dataValue != null) {
        messageBuilder.putData(dataKey, dataValue);
      }

      FirebaseMessaging.getInstance().send(messageBuilder.build());
      log.debug("Push sent: token={}", truncate(deviceToken.getToken()));

    } catch (FirebaseMessagingException ex) {
      String errorCode = ex.getMessagingErrorCode() != null
          ? ex.getMessagingErrorCode().name()
          : "UNKNOWN";
      log.warn("Push failed for token={}: {}", truncate(deviceToken.getToken()), errorCode);

      if ("UNREGISTERED".equals(errorCode)) {
        deviceTokenRepository.deleteByToken(deviceToken.getToken());
        log.info("Removed unregistered token: {}", truncate(deviceToken.getToken()));
      }
    }
  }

  private boolean isFirebaseInitialized() {
    try {
      return !FirebaseApp.getApps().isEmpty();
    } catch (Exception ex) {
      return false;
    }
  }

  private String truncate(String token) {
    return token != null && token.length() > 12
        ? token.substring(0, 12) + "..."
        : token;
  }
}
