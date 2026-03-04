package com.petsapp.notification;

import com.petsapp.auth.User;
import com.petsapp.auth.UserSettings;
import com.petsapp.auth.UserSettingsRepository;
import com.petsapp.common.ApiResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serwis zarzadzania powiadomieniami in-app i push.
 *
 * <p>Logika batch grouping dla lajkow:
 * <ul>
 *   <li>Jesli ostatnie powiadomienie LIKE dla uzytkownika zostalo utworzone mniej niz 1 minute temu
 *       — aktualizujemy tresc zamiast tworzyc nowe (grupowanie wielu lajkow).
 *   <li>Tytulem kumulowanej notyfikacji jest "X osob polubilo Twoje zdjecie"
 * </ul>
 *
 * <p>Metody notify* sa asynchroniczne (@Async) — wywolanie wraca natychmiast, logika dzieje sie w
 * nowym watku z puli 'taskExecutor'. Dzieki temu POST /catches/:id/like nie czeka na push FCM.
 */
@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private static final Duration LIKE_BATCH_WINDOW = Duration.ofSeconds(60);
  private static final int DEFAULT_NOTIFICATION_LIMIT = 30;

  private final NotificationRepository notificationRepository;
  private final DeviceTokenRepository deviceTokenRepository;
  private final UserSettingsRepository userSettingsRepository;
  private final PushService pushService;

  public NotificationService(
      NotificationRepository notificationRepository,
      DeviceTokenRepository deviceTokenRepository,
      UserSettingsRepository userSettingsRepository,
      PushService pushService) {
    this.notificationRepository = notificationRepository;
    this.deviceTokenRepository = deviceTokenRepository;
    this.userSettingsRepository = userSettingsRepository;
    this.pushService = pushService;
  }

  // --- private helpers ---

  /**
   * Sprawdza czy powiadomienia danego typu sa wlaczone dla uzytkownika.
   * Domyslnie (brak ustawien) — wlaczone.
   */
  private boolean isPushEnabled(UUID userId, Function<UserSettings, Boolean> getter) {
    return userSettingsRepository.findById(userId).map(getter).orElse(true);
  }

  /** Buduje prosty jednoklucowy JSON: {"key": "value"}. */
  private static String jsonField(String key, String value) {
    return String.format("{\"%s\":\"%s\"}", key, value);
  }

  /**
   * Tworzy powiadomienie o nowym lajku. Grupuje szybkie lajki (< 1 min) w jedno powiadomienie.
   *
   * @param catchOwner wlasciciel polowania (odbiorca powiadomienia)
   * @param likerUsername nazwa uzytkownika ktory polubil
   * @param catchId ID polowania
   */
  @Async("taskExecutor")
  @Transactional
  public void notifyLike(User catchOwner, String likerUsername, UUID catchId) {
    if (!isPushEnabled(catchOwner.getId(), UserSettings::isPushLikes)) {
      log.debug("Push likes disabled for userId={}", catchOwner.getId());
      return;
    }

    // Batch grouping: sprawdz czy jest nowe powiadomienie LIKE
    Optional<Notification> latestLike =
        notificationRepository.findLatestByUserIdAndType(
            catchOwner.getId(), NotificationTypes.LIKE);

    Instant batchCutoff = Instant.now().minus(LIKE_BATCH_WINDOW);
    if (latestLike.isPresent() && latestLike.get().getCreatedAt().isAfter(batchCutoff)) {
      // Pomijamy — uzytkownik dostanie powiadomienie batch (max raz na minute)
      log.debug(
          "Like batch window active — skipping duplicate notification for userId={}",
          catchOwner.getId());
      pushService.sendToUser(
          catchOwner.getId(),
          "Nowe polubienia",
          likerUsername + " i inni polubili Twoje zdjecie",
          "catchId",
          catchId.toString());
      return;
    }

    Notification notification =
        Notification.builder()
            .user(catchOwner)
            .type(NotificationTypes.LIKE)
            .title("Nowe polubienie")
            .body(likerUsername + " polubil Twoje zdjecie")
            .dataJson(jsonField("catchId", catchId.toString()))
            .build();
    notificationRepository.save(notification);

    pushService.sendToUser(
        catchOwner.getId(),
        notification.getTitle(),
        notification.getBody(),
        "catchId",
        catchId.toString());
  }

  /**
   * Tworzy powiadomienie o nowym komentarzu.
   *
   * @param catchOwner wlasciciel polowania (odbiorca)
   * @param commenterUsername nazwa uzytkownika ktory skomentowal
   * @param catchId ID polowania
   */
  @Async("taskExecutor")
  @Transactional
  public void notifyComment(User catchOwner, String commenterUsername, UUID catchId) {
    if (!isPushEnabled(catchOwner.getId(), UserSettings::isPushComments)) {
      return;
    }

    Notification notification =
        Notification.builder()
            .user(catchOwner)
            .type(NotificationTypes.COMMENT)
            .title("Nowy komentarz")
            .body(commenterUsername + " skomentowal Twoje zdjecie")
            .dataJson(jsonField("catchId", catchId.toString()))
            .build();
    notificationRepository.save(notification);

    pushService.sendToUser(
        catchOwner.getId(),
        notification.getTitle(),
        notification.getBody(),
        "catchId",
        catchId.toString());
  }

  /**
   * Tworzy powiadomienie o nowej prosbie o znajomosc.
   *
   * @param addressee odbiorca (target prosby)
   * @param requesterUsername nazwa uzytkownika wysylajacego prosbe
   * @param friendshipId ID relacji znajomosci
   */
  @Async("taskExecutor")
  @Transactional
  public void notifyFriendRequest(User addressee, String requesterUsername, UUID friendshipId) {
    if (!isPushEnabled(addressee.getId(), UserSettings::isPushFriendRequests)) {
      return;
    }

    Notification notification =
        Notification.builder()
            .user(addressee)
            .type(NotificationTypes.FRIEND_REQUEST)
            .title("Nowa prosba o znajomosc")
            .body(requesterUsername + " chce dodac Cie do znajomych")
            .dataJson(jsonField("friendshipId", friendshipId.toString()))
            .build();
    notificationRepository.save(notification);

    pushService.sendToUser(
        addressee.getId(),
        notification.getTitle(),
        notification.getBody(),
        "friendshipId",
        friendshipId.toString());
  }

  /**
   * Tworzy powiadomienie o zaakceptowaniu prosby o znajomosc.
   *
   * @param requester nadawca oryginalnej prosby (odbiorca powiadomienia)
   * @param accepterUsername nazwa uzytkownika ktory zaakceptowal
   */
  @Async("taskExecutor")
  @Transactional
  public void notifyFriendAccepted(User requester, String accepterUsername) {
    if (!isPushEnabled(requester.getId(), UserSettings::isPushFriendRequests)) {
      return;
    }

    Notification notification =
        Notification.builder()
            .user(requester)
            .type(NotificationTypes.FRIEND_ACCEPTED)
            .title("Prosba zaakceptowana")
            .body(accepterUsername + " zaakceptowal Twoja prosbe o znajomosc")
            .build();
    notificationRepository.save(notification);

    pushService.sendToUser(
        requester.getId(), notification.getTitle(), notification.getBody(), null, null);
  }

  /**
   * Tworzy powiadomienie o odblokowaniu achievementu.
   *
   * @param user uzytkownik ktory odblokowal achievement (odbiorca)
   * @param achievementName polska nazwa achievementu
   * @param achievementCode kod achievementu (do nawigacji w aplikacji)
   */
  @Async("taskExecutor")
  @Transactional
  public void notifyAchievement(User user, String achievementName, String achievementCode) {
    if (!isPushEnabled(user.getId(), UserSettings::isPushAchievements)) {
      return;
    }

    Notification notification =
        Notification.builder()
            .user(user)
            .type(NotificationTypes.ACHIEVEMENT)
            .title("Nowe osiagniecie!")
            .body("Odblokowales: " + achievementName)
            .dataJson(jsonField("achievementCode", achievementCode))
            .build();
    notificationRepository.save(notification);

    pushService.sendToUser(
        user.getId(),
        notification.getTitle(),
        notification.getBody(),
        "achievementCode",
        achievementCode);
  }

  /**
   * Zwraca liste powiadomien biezacego uzytkownika (max 30 najnowszych).
   *
   * @param currentUser zalogowany uzytkownik
   * @return lista DTO powiadomien z liczba nieprzeczytanych
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<NotificationResponse>> getNotifications(User currentUser) {
    List<NotificationResponse> notifications =
        notificationRepository
            .findByUserIdLatest(currentUser.getId(), DEFAULT_NOTIFICATION_LIMIT)
            .stream()
            .map(NotificationResponse::from)
            .toList();
    return ApiResponse.ok(notifications);
  }

  /**
   * Oznacza wszystkie powiadomienia biezacego uzytkownika jako przeczytane.
   *
   * @param currentUser zalogowany uzytkownik
   */
  @Transactional
  public void markAllRead(User currentUser) {
    int updated = notificationRepository.markAllReadByUserId(currentUser.getId());
    log.debug("Marked {} notifications as read for userId={}", updated, currentUser.getId());
  }

  /**
   * Rejestruje token FCM urzadzenia. Jesli token juz istnieje — pomija duplikat.
   *
   * @param currentUser zalogowany uzytkownik
   * @param token token FCM
   * @param platform platforma: ios lub android
   */
  @Transactional
  public void registerDevice(User currentUser, String token, String platform) {
    deviceTokenRepository
        .findByToken(token)
        .ifPresentOrElse(
            existing -> log.debug("Device token already registered: {}", truncate(token)),
            () -> {
              DeviceToken newToken = DeviceToken.of(currentUser, token, platform);
              deviceTokenRepository.save(newToken);
              log.debug(
                  "Device registered: userId={}, platform={}", currentUser.getId(), platform);
            });
  }

  /**
   * Wyrejestrowuje token FCM urzadzenia.
   *
   * @param token token FCM do usuniecia
   */
  @Transactional
  public void unregisterDevice(String token) {
    deviceTokenRepository.deleteByToken(token);
    log.debug("Device unregistered: {}", truncate(token));
  }

  private String truncate(String token) {
    return token != null && token.length() > 12 ? token.substring(0, 12) + "..." : token;
  }
}
