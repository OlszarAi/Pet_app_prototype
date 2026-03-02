package com.petsapp.feed;

import com.petsapp.auth.User;
import com.petsapp.catch_.CatchResponse;
import com.petsapp.common.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpointy feeda psow.
 *
 * <p>Wszystkie endpointy wymagaja uwierzytelnienia — pole {@code liked} w CatchResponse wymaga
 * informacji o zalogowanym uzytkowniku. Trzy typy feeda:
 * <ul>
 *   <li>{@code /feed/public} — globalny feed rankingowy (Redis feed:public)
 *   <li>{@code /feed/friends} — feed znajomych, chronologiczny (DB JOIN friendship)
 *   <li>{@code /feed/trending} — top {limit} z ostatnich 24h (Redis feed:trending)
 * </ul>
 */
@RestController
@RequestMapping("/feed")
public class FeedController {

  private final FeedService feedService;

  public FeedController(FeedService feedService) {
    this.feedService = feedService;
  }

  /**
   * Zwraca strone publicznego feeda posortowanego malejaco po feed score.
   *
   * <p>Paginacja kursorem: wartosc {@code cursor} z pola {@code pagination.cursor} poprzedniej
   * odpowiedzi. Dla pierwszej strony nie przekazuj parametru cursor.
   *
   * @param cursor zakodowany kursor lub null dla pierwszej strony
   * @param limit ilosc wynikow na stronie (domyslnie 20, max 50)
   */
  @GetMapping("/public")
  public ResponseEntity<ApiResponse<List<CatchResponse>>> getPublicFeed(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    ApiResponse<List<CatchResponse>> response =
        feedService.getPublicFeed(currentUser, cursor, limit);
    return ResponseEntity.ok(response);
  }

  /**
   * Zwraca strone feeda znajomych w kolejnosci chronologicznej (najnowsze pierwsze).
   *
   * <p>Zwraca tylko catche uzytkownikow z zaakceptowana relacja znajomosci. Jesli uzytkownik nie
   * ma zadnych znajomych, zwraca pusta liste (nie blad).
   *
   * @param cursor zakodowany kursor (epoch millis) lub null dla pierwszej strony
   * @param limit ilosc wynikow na stronie (domyslnie 20, max 50)
   */
  @GetMapping("/friends")
  public ResponseEntity<ApiResponse<List<CatchResponse>>> getFriendsFeed(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int limit) {
    ApiResponse<List<CatchResponse>> response =
        feedService.getFriendsFeed(currentUser, cursor, limit);
    return ResponseEntity.ok(response);
  }

  /**
   * Zwraca top N catchy z ostatnich 24h, posortowanych malejaco po feed score.
   *
   * <p>Trending nie ma paginacji — zwraca pelna liste (max 10). Wyniki sa cachowane w Redis
   * (feed:trending) z TTL 5 min.
   *
   * @param limit maksymalna liczba wynikow (domyslnie 10, max 10)
   */
  @GetMapping("/trending")
  public ResponseEntity<ApiResponse<List<CatchResponse>>> getTrendingFeed(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(defaultValue = "10") int limit) {
    ApiResponse<List<CatchResponse>> response =
        feedService.getTrendingFeed(currentUser, limit);
    return ResponseEntity.ok(response);
  }
}
