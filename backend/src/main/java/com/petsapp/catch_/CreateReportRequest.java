package com.petsapp.catch_;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO dla zglaszania polowania jako nieodpowiednie.
 */
public record CreateReportRequest(
    @NotBlank(message = "Reason is required")
    @Size(max = 50, message = "Reason must not exceed 50 characters")
    String reason,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description) {}
