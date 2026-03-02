package com.petsapp.catch_;

import com.petsapp.auth.User;
import com.petsapp.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpointy dla operacji na polowaniach psow.
 *
 * <p>Kontroler odpowiada wylacznie za routing, walidacje danych wejsciowych i mapowanie na
 * ApiResponse. Logika biznesowa (rate limiting, przetwarzanie zdjecia, denormalizacja) jest w
 * {@link CatchService}. Aktualnie zalogowany uzytkownik jest wstrzykiwany przez
 * {@code @AuthenticationPrincipal} — JwtFilter ustawia encje {@link User} jako principal.
 */
@RestController
public class CatchController {

  private final CatchService catchService;

  public CatchController(CatchService catchService) {
    this.catchService = catchService;
  }

  /**
   * Tworzy nowe polowanie z uploadem zdjecia.
   *
   * <p>Akceptuje multipart/form-data. Zdjecie jest wymagane, pozostale pola sa opcjonalne (breedId
   * jest obowiazkowy). Rate limit: 10 catchy/min per uzytkownik.
   */
  @PostMapping(value = "/catches", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<CatchResponse>> createCatch(
      @AuthenticationPrincipal User currentUser,
      @RequestPart("file") MultipartFile file,
      @RequestParam @NotNull Integer breedId,
      @RequestParam(required = false) @Size(max = 300) String caption,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude,
      @RequestParam(required = false) @Size(max = 255) String locationName,
      @RequestParam(defaultValue = "true") boolean isPublic) {

    CatchResponse response =
        catchService.createCatch(
            currentUser, breedId, file, caption, latitude, longitude, locationName, isPublic);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  /** Zwraca pojedynczy catch po ID. */
  @GetMapping("/catches/{id}")
  public ResponseEntity<ApiResponse<CatchResponse>> getCatch(
      @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
    CatchResponse response = catchService.getCatch(id, currentUser);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * Soft-deletes catch zalogowanego uzytkownika.
   *
   * <p>Tylko wlasciciel moze usunac swoje polowanie. Fizyczny plik jest usuwany z S3.
   */
  @DeleteMapping("/catches/{id}")
  public ResponseEntity<Void> deleteCatch(
      @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
    catchService.deleteCatch(id, currentUser);
    return ResponseEntity.noContent().build();
  }

  /**
   * Dodaje polubienie do catcha.
   *
   * <p>Jezeli uzytkownik juz polubil — zwraca 409 Conflict.
   */
  @PostMapping("/catches/{id}/likes")
  public ResponseEntity<ApiResponse<CatchResponse>> likeCatch(
      @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
    CatchResponse response = catchService.likeCatch(id, currentUser);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /** Usuwa polubienie z catcha. Idempotentne — jesli polubienie nie istnieje, nie zwraca bledu. */
  @DeleteMapping("/catches/{id}/likes")
  public ResponseEntity<Void> unlikeCatch(
      @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
    catchService.unlikeCatch(id, currentUser);
    return ResponseEntity.noContent().build();
  }

  /**
   * Zwraca paginowana liste komentarzy dla catcha.
   *
   * @param cursor ISO-8601 timestamp ostatniego elementu poprzedniej strony (null = pierwsza
   *     strona)
   * @param limit max liczba wynikow (domyslnie 20, max 50)
   */
  @GetMapping("/catches/{id}/comments")
  public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
      @PathVariable UUID id,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer limit) {
    ApiResponse<List<CommentResponse>> response = catchService.getComments(id, cursor, limit);
    return ResponseEntity.ok(response);
  }

  /** Dodaje komentarz do catcha. */
  @PostMapping("/catches/{id}/comments")
  public ResponseEntity<ApiResponse<CommentResponse>> addComment(
      @PathVariable UUID id,
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody CreateCommentRequest request) {
    CommentResponse response = catchService.addComment(id, currentUser, request.content());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  /**
   * Soft-deletes komentarz.
   *
   * <p>Tylko autor komentarza moze go usunac.
   */
  @DeleteMapping("/comments/{id}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
    catchService.deleteComment(id, currentUser);
    return ResponseEntity.noContent().build();
  }

  /** Zglasza catch jako nieodpowiedni. */
  @PostMapping("/catches/{id}/reports")
  public ResponseEntity<Void> reportCatch(
      @PathVariable UUID id,
      @AuthenticationPrincipal User currentUser,
      @Valid @RequestBody CreateReportRequest request) {
    catchService.reportCatch(id, currentUser, request.reason(), request.description());
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * Zwraca paginowana liste catchy uzytkownika.
   *
   * <p>Dla wlasnego profilu zwraca wszystkie (publiczne + prywatne). Dla cudzego — tylko publiczne.
   *
   * @param userId ID uzytkownika
   * @param cursor ISO-8601 timestamp (null = pierwsza strona)
   * @param limit max liczba wynikow (domyslnie 20, max 50)
   */
  @GetMapping("/users/{userId}/catches")
  public ResponseEntity<ApiResponse<List<CatchResponse>>> getUserCatches(
      @PathVariable UUID userId,
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Integer limit) {
    ApiResponse<List<CatchResponse>> response =
        catchService.getUserCatches(userId, currentUser, cursor, limit);
    return ResponseEntity.ok(response);
  }
}
