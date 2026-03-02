package com.petsapp.catch_;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO dla tworzenia komentarza pod polowaniem.
 */
public record CreateCommentRequest(
    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 500, message = "Comment must be between 1 and 500 characters")
    String content) {}
