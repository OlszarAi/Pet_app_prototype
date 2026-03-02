package com.petsapp.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO wejscia do rejestracji tokenu FCM urzadzenia.
 */
public record RegisterDeviceRequest(
    @NotBlank String token,
    @NotBlank @Pattern(regexp = "^(ios|android)$", message = "platform must be 'ios' or 'android'")
        String platform) {}
