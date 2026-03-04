package com.petsapp.notification;

import com.petsapp.auth.User;
import com.petsapp.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpointy dla powiadomien i zarzadzania tokenami urzadzen.
 *
 * <p>Wszystkie endpointy wymagaja uwierzytelnienia JWT.
 */
@RestController
@Tag(name = "Notifications", description = "Powiadomienia i tokeny FCM")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/notifications")
  @Operation(summary = "Pobierz liste powiadomien (max 30)")
  public ApiResponse<List<NotificationResponse>> getNotifications(
      @AuthenticationPrincipal User currentUser) {
    return notificationService.getNotifications(currentUser);
  }

  @PostMapping("/notifications/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Oznacz wszystkie powiadomienia jako przeczytane")
  public void markAllRead(@AuthenticationPrincipal User currentUser) {
    notificationService.markAllRead(currentUser);
  }

  @PostMapping("/devices")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Zarejestruj token FCM urzadzenia")
  public ApiResponse<Void> registerDevice(
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody RegisterDeviceRequest request) {
    notificationService.registerDevice(currentUser, request.token(), request.platform());
    return ApiResponse.ok(null);
  }

  @DeleteMapping("/devices/{token}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Wyrejestruj token FCM urzadzenia")
  public void unregisterDevice(
      @AuthenticationPrincipal User currentUser, @PathVariable String token) {
    notificationService.unregisterDevice(token);
  }
}
