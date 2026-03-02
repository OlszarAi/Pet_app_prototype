package com.petsapp.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petsapp.AbstractIntegrationTest;
import com.petsapp.auth.AuthService;
import com.petsapp.auth.EmailService;
import com.petsapp.auth.RegisterRequest;
import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import com.petsapp.auth.VerifyEmailRequest;
import com.petsapp.common.ApiResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla NotificationService z prawdziwa baza danych.
 *
 * <p>PushService jest mockowany na poziomie AbstractIntegrationTest — weryfikujemy tylko
 * logike zapisu powiadomien do bazy, nie wysylke push.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationServiceTest extends AbstractIntegrationTest {

  @Autowired private NotificationService notificationService;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private DeviceTokenRepository deviceTokenRepository;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;

  @MockBean private EmailService emailService;

  @BeforeEach
  void setupMocks() {
    doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  private User createVerifiedUser() {
    String email = "notif-test-" + UUID.randomUUID() + "@example.com";
    String username = "nt" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    String code = "333444";
    when(emailService.generateVerificationCode()).thenReturn(code);
    when(emailService.verificationCodeExpiry()).thenReturn(Instant.now().plusSeconds(900));

    authService.register(new RegisterRequest(email, username, "Password123!"));
    authService.verifyEmail(new VerifyEmailRequest(email, code));

    return userRepository.findActiveByEmail(email).orElseThrow();
  }

  @Test
  void getNotifications_emptyForNewUser_returnsEmptyList() {
    User user = createVerifiedUser();

    ApiResponse<List<NotificationResponse>> response = notificationService.getNotifications(user);

    assertThat(response.success()).isTrue();
    assertThat(response.data()).isEmpty();
  }

  @Test
  void notifyLike_savesNotificationToDb() throws InterruptedException {
    User catchOwner = createVerifiedUser();
    User liker = createVerifiedUser();
    UUID catchId = UUID.randomUUID();

    // @Async — wywolujemy synchronicznie w testach przez bezposrednie wywolanie metody.
    // Spring wywola metode SYNCHRONICZNIE jezeli @DirtiesContext resetuje kontekst,
    // lub uzyjemy synchronizacji przez Thread.sleep dla async.
    // Prostsze: wywolujemy serwis a potem weryfikujemy stan DB.
    notificationService.notifyLike(catchOwner, liker.getUsername(), catchId);

    // Czekamy na zakonczenie async task
    Thread.sleep(500);

    List<Notification> notifications =
        notificationRepository.findByUserIdLatest(catchOwner.getId(), 10);
    assertThat(notifications).hasSize(1);
    assertThat(notifications.get(0).getType()).isEqualTo(NotificationTypes.LIKE);
    assertThat(notifications.get(0).isRead()).isFalse();
  }

  @Test
  void markAllRead_updatesAllNotificationsAsRead() throws InterruptedException {
    User user = createVerifiedUser();
    UUID catchId = UUID.randomUUID();

    notificationService.notifyLike(user, "someUser", catchId);
    Thread.sleep(500);

    notificationService.markAllRead(user);

    long unread = notificationRepository.countUnread(user.getId());
    assertThat(unread).isZero();
  }

  @Test
  void registerDevice_savesTokenToDb() {
    User user = createVerifiedUser();
    String token = "fcm-token-" + UUID.randomUUID();

    notificationService.registerDevice(user, token, "android");

    assertThat(deviceTokenRepository.findByToken(token)).isPresent();
    assertThat(deviceTokenRepository.findAllByUserId(user.getId())).hasSize(1);
  }

  @Test
  void registerDevice_duplicateToken_doesNotCreateDuplicate() {
    User user = createVerifiedUser();
    String token = "fcm-dedup-" + UUID.randomUUID();

    notificationService.registerDevice(user, token, "ios");
    notificationService.registerDevice(user, token, "ios");

    assertThat(deviceTokenRepository.findAllByUserId(user.getId())).hasSize(1);
  }

  @Test
  void unregisterDevice_removesTokenFromDb() {
    User user = createVerifiedUser();
    String token = "fcm-unreg-" + UUID.randomUUID();

    notificationService.registerDevice(user, token, "android");
    notificationService.unregisterDevice(token);

    assertThat(deviceTokenRepository.findByToken(token)).isEmpty();
  }
}
