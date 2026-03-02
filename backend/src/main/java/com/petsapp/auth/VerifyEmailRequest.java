package com.petsapp.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
    @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6, message = "Verification code must be exactly 6 characters")
        String code) {}
