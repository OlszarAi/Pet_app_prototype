package com.petsapp.common;

import com.petsapp.auth.AuthException;
import com.petsapp.auth.ConflictException;
import com.petsapp.auth.RateLimitExceededException;
import com.petsapp.breed.BreedNotFoundException;
import com.petsapp.breed.PokedexAccessDeniedException;
import com.petsapp.catch_.CatchAccessDeniedException;
import com.petsapp.catch_.CatchNotFoundException;
import com.petsapp.catch_.ImageProcessingException;
import com.petsapp.friend.FriendshipNotFoundException;
import com.petsapp.user.AvatarProcessingException;
import com.petsapp.user.PasswordMismatchException;
import com.petsapp.user.UnsupportedFileFormatException;
import com.petsapp.user.UserNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Centralny handler wyjatkow dla calego API. Bledy nigdy nie sa zwracane bezposrednio z kontrolerow
 * — trafiaja tutaj.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Bledy walidacji DTO (@Valid na parametrach kontrolera). Zwraca mape pola -> komunikat. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException ex) {

    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    fe -> fe.getField(),
                    fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                    (existing, duplicate) -> existing));

    return ResponseEntity.badRequest()
        .body(
            ApiResponse.error(
                ApiResponse.ErrorDetail.of(
                    ErrorCode.VALIDATION_ERROR, "Validation failed", fieldErrors)));
  }

  /** Bledy walidacji na poziomie encji (@Validated na serwisach). */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException ex) {

    Map<String, String> fieldErrors =
        ex.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    cv -> cv.getPropertyPath().toString(),
                    cv -> cv.getMessage(),
                    (existing, duplicate) -> existing));

    return ResponseEntity.badRequest()
        .body(
            ApiResponse.error(
                ApiResponse.ErrorDetail.of(
                    ErrorCode.VALIDATION_ERROR, "Validation failed", fieldErrors)));
  }

  /**
   * Bledne dane uwierzytelniania, nieprawidlowy token lub niezweryfikowany email. 401 Unauthorized.
   */
  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            ApiResponse.error(ApiResponse.ErrorDetail.of(ErrorCode.UNAUTHORIZED, ex.getMessage())));
  }

  /** Duplikat emaila lub nazwy uzytkownika. 409 Conflict. */
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiResponse<Void>> handleConflictException(ConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(ApiResponse.ErrorDetail.of(ErrorCode.CONFLICT, ex.getMessage())));
  }

  /** Przekroczono limit zapytan. 429 Too Many Requests. */
  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(RateLimitExceededException ex) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(
            ApiResponse.error(ApiResponse.ErrorDetail.of(ErrorCode.RATE_LIMITED, ex.getMessage())));
  }

  /** Zdjecie przekracza limit 10MB. */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(
            ApiResponse.error(
                ApiResponse.ErrorDetail.of(
                    ErrorCode.FILE_TOO_LARGE, "File size exceeds the 10MB limit")));
  }

  /** Catch nie zostal znaleziony lub zostal soft-deleted. 404 Not Found. */
  @ExceptionHandler(CatchNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleCatchNotFoundException(CatchNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ApiResponse.ErrorDetail.of(ErrorCode.NOT_FOUND, ex.getMessage())));
  }

  /** Proba modyfikacji cudzego catcha lub komentarza. 403 Forbidden. */
  @ExceptionHandler(CatchAccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleCatchAccessDenied(
      CatchAccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ApiResponse.error(ApiResponse.ErrorDetail.of(ErrorCode.FORBIDDEN, ex.getMessage())));
  }

  /** Blad przetwarzania zdjecia catcha. 400 Bad Request. */
  @ExceptionHandler(ImageProcessingException.class)
  public ResponseEntity<ApiResponse<Void>> handleImageProcessingException(
      ImageProcessingException ex) {
    log.warn("Image processing failed", ex);
    return ResponseEntity.badRequest()
        .body(
            ApiResponse.error(
                ApiResponse.ErrorDetail.of(
                    ErrorCode.VALIDATION_ERROR, "Could not process the uploaded image.")));
  }

  /** Rasa nie zostala znaleziona lub nie jest aktywna. 404 Not Found. */
  @ExceptionHandler(BreedNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleBreedNotFoundException(BreedNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ApiResponse.ErrorDetail.of(ErrorCode.NOT_FOUND, ex.getMessage())));
  }

  /** Brak dostepu do Pokedeksu prywatnego profilu. 403 Forbidden. */
  @ExceptionHandler(PokedexAccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handlePokedexAccessDenied(
      PokedexAccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(ApiResponse.ErrorDetail.of(ErrorCode.FORBIDDEN, ex.getMessage())));
  }

  /** Uzytkownik nie zostal znaleziony lub zostal soft-deleted. 404 Not Found. */
  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ApiResponse.ErrorDetail.of(ErrorCode.NOT_FOUND, ex.getMessage())));
  }

  /** Relacja znajomosci nie istnieje lub nie nalezy do biezacego uzytkownika. 404 Not Found. */
  @ExceptionHandler(FriendshipNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleFriendshipNotFoundException(
      FriendshipNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ApiResponse.ErrorDetail.of(ErrorCode.NOT_FOUND, ex.getMessage())));
  }

  /**
   * Niedozwolony format pliku (np. nieprawidlowy MIME type avatara). 415 Unsupported Media Type.
   */
  @ExceptionHandler(UnsupportedFileFormatException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnsupportedFileFormat(
      UnsupportedFileFormatException ex) {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(
            ApiResponse.error(
                ApiResponse.ErrorDetail.of(ErrorCode.UNSUPPORTED_FORMAT, ex.getMessage())));
  }

  /** Blad przetwarzania obrazu avatara (uszkodzony plik). 400 Bad Request. */
  @ExceptionHandler(AvatarProcessingException.class)
  public ResponseEntity<ApiResponse<Void>> handleAvatarProcessingException(
      AvatarProcessingException ex) {
    log.warn("Avatar processing failed", ex);
    return ResponseEntity.badRequest()
        .body(
            ApiResponse.error(
                ApiResponse.ErrorDetail.of(
                    ErrorCode.VALIDATION_ERROR, "Could not process the uploaded image.")));
  }

  /** Nieprawidlowe aktualne haslo przy zmianie hasla. 400 Bad Request. */
  @ExceptionHandler(PasswordMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handlePasswordMismatch(PasswordMismatchException ex) {
    return ResponseEntity.badRequest()
        .body(
            ApiResponse.error(
                ApiResponse.ErrorDetail.of(ErrorCode.VALIDATION_ERROR, ex.getMessage())));
  }

  /**
   * Fallback dla wszystkich nieprzewidzianych wyjatkow. Loguje pelny stack trace, ale do klienta
   * zwraca tylko ogolny komunikat.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.internalServerError()
        .body(
            ApiResponse.error(
                ApiResponse.ErrorDetail.of(
                    ErrorCode.INTERNAL_ERROR, "An unexpected error occurred")));
  }
}
