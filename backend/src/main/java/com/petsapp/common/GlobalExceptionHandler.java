package com.petsapp.common;

import com.petsapp.auth.AuthException;
import com.petsapp.auth.ConflictException;
import com.petsapp.auth.RateLimitExceededException;
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
 * Centralny handler wyjatkow dla calego API.
 * Bledy nigdy nie sa zwracane bezposrednio z kontrolerow — trafiaja tutaj.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        /**
         * Bledy walidacji DTO (@Valid na parametrach kontrolera). Zwraca mape pola ->
         * komunikat.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidationException(
                        MethodArgumentNotValidException ex) {

                Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                                .collect(
                                                Collectors.toMap(
                                                                fe -> fe.getField(),
                                                                fe -> fe.getDefaultMessage() != null
                                                                                ? fe.getDefaultMessage()
                                                                                : "invalid",
                                                                (existing, duplicate) -> existing));

                return ResponseEntity.badRequest()
                                .body(
                                                ApiResponse.error(
                                                                ApiResponse.ErrorDetail.of(ErrorCode.VALIDATION_ERROR,
                                                                                "Validation failed", fieldErrors)));
        }

        /** Bledy walidacji na poziomie encji (@Validated na serwisach). */
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
                        ConstraintViolationException ex) {

                Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                                .collect(
                                                Collectors.toMap(
                                                                cv -> cv.getPropertyPath().toString(),
                                                                cv -> cv.getMessage(),
                                                                (existing, duplicate) -> existing));

                return ResponseEntity.badRequest()
                                .body(
                                                ApiResponse.error(
                                                                ApiResponse.ErrorDetail.of(ErrorCode.VALIDATION_ERROR,
                                                                                "Validation failed", fieldErrors)));
        }

        /**
         * Bledne dane uwierzytelniania, nieprawidlowy token lub niezweryfikowany email.
         * 401 Unauthorized.
         */
        @ExceptionHandler(AuthException.class)
        public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error(
                                                ApiResponse.ErrorDetail.of(ErrorCode.UNAUTHORIZED, ex.getMessage())));
        }

        /** Duplikat emaila lub nazwy uzytkownika. 409 Conflict. */
        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ApiResponse<Void>> handleConflictException(ConflictException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error(
                                                ApiResponse.ErrorDetail.of(ErrorCode.CONFLICT, ex.getMessage())));
        }

        /** Przekroczono limit zapytan. 429 Too Many Requests. */
        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(
                        RateLimitExceededException ex) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(ApiResponse.error(
                                                ApiResponse.ErrorDetail.of(ErrorCode.RATE_LIMITED, ex.getMessage())));
        }

        /** Zdjecie przekracza limit 10MB. */
        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiResponse<Void>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                                .body(
                                                ApiResponse.error(
                                                                ApiResponse.ErrorDetail.of(ErrorCode.FILE_TOO_LARGE,
                                                                                "File size exceeds the 10MB limit")));
        }

        /**
         * Fallback dla wszystkich nieprzewidzianych wyjatkow.
         * Loguje pelny stack trace, ale do klienta zwraca tylko ogolny komunikat.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
                log.error("Unhandled exception", ex);
                return ResponseEntity.internalServerError()
                                .body(
                                                ApiResponse.error(
                                                                ApiResponse.ErrorDetail.of(ErrorCode.INTERNAL_ERROR,
                                                                                "An unexpected error occurred")));
        }
}
