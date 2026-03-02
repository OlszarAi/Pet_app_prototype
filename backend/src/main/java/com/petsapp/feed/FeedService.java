package com.petsapp.feed;

import com.petsapp.auth.User;
import com.petsapp.catch_.CatchResponse;
import com.petsapp.catch_.DogCatch;
import com.petsapp.catch_.DogCatchRepository;
import com.petsapp.catch_.LikeRepository;
import com.petsapp.common.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Glowny serwis feeda — laczy cache Redis z fallbackiem bazodanowym.
 *
 * <p>Strategia:
 * <ul>
 *   <li>Publiczny feed ({@code feed:public}) i trending ({@code feed:trending}) — pobierane przez
 *       Redis Sorted Set. Jesli Redis jest niedostepny lub klucz jest pusty, serwis odpada do
 *       zapytan bezposrednio na bazie danych (DB fallback).
 *   <li>Feed znajomych — zawsze z bazy danych. Redis friends feed (feed:friends:{userId}) zostanie
 *       zaimplementowany w Kroku 8 gdy pojawi sie encja Friendship.
 * </ul>
 *
 * <p>Kursor paginacji:
 * <ul>
 *   <li>Publiczny / trending: Base64(score jako string double), np. Base64("3.14159")
 *   <li>Znajomych: Base64(caughtAt epoch millis jako string), np. Base64("1704067200000")
 * </ul>
 */
@Service
public class FeedService {

  private static final Logger log = LoggerFactory.getLogger(FeedService.class);

  private static final int DEFAULT_LIMIT = 20;
  private static final int MAX_LIMIT = 50;
  private static final int MAX_TRENDING_LIMIT = 10;

  private final DogCatchRepository catchRepository;
  private final LikeRepository likeRepository;
  private final FeedCacheService feedCacheService;

  public FeedService(
      DogCatchRepository catchRepository,
      LikeRepository likeRepository,
      FeedCacheService feedCacheService) {
    this.catchRepository = catchRepository;
    this.likeRepository = likeRepository;
    this.feedCacheService = feedCacheService;
  }

  /**
   * Zwraca strone publicznego feeda posortowanego malejaco po feed score.
   *
   * <p>Probuje odczytac ID catchy z Redis (feed:public). Jesli cache jest niedostepny lub pusty,
   * wypada na zapytanie bezposrednio do bazy danych, uzywajac feed_score jako klucza sortowania.
   *
   * @param currentUser zalogowany uzytkownik (potrzebny do oznaczenia pola {@code liked})
   * @param cursor zakodowany kursor z poprzedniej strony lub null dla pierwszej strony
   * @param limit liczba wynikow na stronie (max 50)
   * @return ApiResponse z lista CatchResponse i metadanymi paginacji
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<CatchResponse>> getPublicFeed(
      User currentUser, String cursor, int limit) {
    int safeLimit = clampLimit(limit, MAX_LIMIT);
    Double cursorScore = decodeScoreCursor(cursor);

    List<DogCatch> catches;
    double lastScore;

    Optional<List<FeedCacheService.ScoredCatchEntry>> cacheResult =
        feedCacheService.getPublicPage(cursorScore, safeLimit + 1);

    if (cacheResult.isPresent()) {
      List<FeedCacheService.ScoredCatchEntry> entries = cacheResult.get();
      if (entries.isEmpty()) {
        return buildEmptyPage();
      }
      List<UUID> ids = entries.stream().map(FeedCacheService.ScoredCatchEntry::catchId).toList();
      catches = reorderByInput(catchRepository.findAllByIdInWithBreedAndUser(ids), ids);
      lastScore = entries.get(entries.size() - 1).score();
    } else {
      // DB fallback
      log.debug("Redis unavailable — using DB fallback for public feed (cursor={})", cursorScore);
      if (cursorScore == null) {
        catches = catchRepository.findPublicFeedFirstPage(safeLimit + 1);
      } else {
        catches = catchRepository.findPublicFeedNextPage(cursorScore, safeLimit + 1);
      }
      lastScore = catches.isEmpty() ? 0.0 : catches.get(catches.size() - 1).getFeedScore();
    }

    return buildPage(catches, safeLimit, encodeScoreCursor(lastScore), currentUser);
  }

  /**
   * Zwraca strone feeda znajomych w kolejnosci chronologicznej (najnowsze pierwsze).
   *
   * <p>Pobierany bezposrednio z bazy danych przez JOIN z tabela friendship. Redis friends feed
   * (feed:friends:{userId}) zostanie dodany w Kroku 8 jako optymalizacja.
   *
   * @param currentUser zalogowany uzytkownik (wlasciciel feeda)
   * @param cursor zakodowany kursor (epoch millis ostatniego caughtAt) lub null
   * @param limit liczba wynikow na stronie (max 50)
   * @return ApiResponse z lista CatchResponse i metadanymi paginacji
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<CatchResponse>> getFriendsFeed(
      User currentUser, String cursor, int limit) {
    int safeLimit = clampLimit(limit, MAX_LIMIT);
    Instant cursorInstant = decodeInstantCursor(cursor);

    List<DogCatch> catches;
    if (cursorInstant == null) {
      catches = catchRepository.findFriendsFirstPage(currentUser.getId(), safeLimit + 1);
    } else {
      catches = catchRepository.findFriendsNextPage(
          currentUser.getId(), cursorInstant, safeLimit + 1);
    }

    Instant lastCaughtAt = catches.isEmpty()
        ? Instant.now()
        : catches.get(catches.size() - 1).getCaughtAt();
    return buildPage(catches, safeLimit, encodeInstantCursor(lastCaughtAt), currentUser);
  }

  /**
   * Zwraca liste trending catchy — najpopularniejsze z ostatnich 24h.
   *
   * <p>Probuje odczytac z Redis (feed:trending). Jesli cache jest niedostepny lub pusty, odpada
   * na zapytanie DB z filtrem caught_at >= now - 24h, posortowanym po feed_score DESC.
   *
   * @param currentUser zalogowany uzytkownik
   * @param limit liczba wynikow (max 10)
   * @return ApiResponse z lista CatchResponse (bez paginacji — pelna lista trending)
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<CatchResponse>> getTrendingFeed(User currentUser, int limit) {
    int safeLimit = Math.min(limit, MAX_TRENDING_LIMIT);
    Instant since = Instant.now().minusSeconds(86_400); // 24h

    List<DogCatch> catches;
    Optional<List<FeedCacheService.ScoredCatchEntry>> cacheResult =
        feedCacheService.getTrendingPage(null, safeLimit);

    if (cacheResult.isPresent() && !cacheResult.get().isEmpty()) {
      List<UUID> ids = cacheResult.get().stream()
          .map(FeedCacheService.ScoredCatchEntry::catchId).toList();
      catches = reorderByInput(catchRepository.findAllByIdInWithBreedAndUser(ids), ids);
    } else {
      log.debug("Redis unavailable — using DB fallback for trending feed");
      catches = catchRepository.findTrendingFirstPage(since, safeLimit);
    }

    Set<UUID> likedIds = batchLoadLikedIds(currentUser.getId(), catches);
    List<CatchResponse> responses = catches.stream()
        .map(dc -> CatchResponse.from(dc, likedIds.contains(dc.getId())))
        .toList();

    return ApiResponse.ok(responses);
  }

  // --- private helpers ---

  private ApiResponse<List<CatchResponse>> buildPage(
      List<DogCatch> rawCatches, int limit, String lastCursor, User currentUser) {
    boolean hasMore = rawCatches.size() > limit;
    List<DogCatch> catches = hasMore ? rawCatches.subList(0, limit) : rawCatches;

    if (catches.isEmpty()) {
      return ApiResponse.ok(Collections.emptyList(), new ApiResponse.PaginationMeta(null, false));
    }

    Set<UUID> likedIds = batchLoadLikedIds(currentUser.getId(), catches);
    List<CatchResponse> responses = catches.stream()
        .map(dc -> CatchResponse.from(dc, likedIds.contains(dc.getId())))
        .toList();

    String nextCursor = hasMore ? lastCursor : null;
    return ApiResponse.ok(responses, new ApiResponse.PaginationMeta(nextCursor, hasMore));
  }

  private ApiResponse<List<CatchResponse>> buildEmptyPage() {
    return ApiResponse.ok(
        Collections.emptyList(), new ApiResponse.PaginationMeta(null, false));
  }

  private Set<UUID> batchLoadLikedIds(UUID userId, List<DogCatch> catches) {
    if (catches.isEmpty()) {
      return Set.of();
    }
    List<UUID> catchIds = catches.stream().map(DogCatch::getId).toList();
    return likeRepository.findLikedCatchIdsByUserIdIn(userId, catchIds);
  }

  /**
   * Przywraca kolejnosc catches zgodna z lista ids (wynikow z Redis Sorted Set).
   *
   * <p>findAllByIdInWithBreedAndUser nie gwarantuje kolejnosci wg IN clause — tu sortujemy wg
   * pozycji UUID w oryginalnej liscie z Redis.
   */
  private List<DogCatch> reorderByInput(List<DogCatch> unordered, List<UUID> orderedIds) {
    Map<UUID, DogCatch> byId = unordered.stream()
        .collect(Collectors.toMap(DogCatch::getId, Function.identity()));
    return orderedIds.stream()
        .filter(byId::containsKey)
        .map(byId::get)
        .toList();
  }

  private int clampLimit(int requested, int max) {
    return (requested <= 0 || requested > max) ? DEFAULT_LIMIT : requested;
  }

  // --- cursor encoding ---

  /**
   * Koduje score jako kursor: Base64(Double.toString(score)).
   */
  static String encodeScoreCursor(double score) {
    return Base64.getUrlEncoder()
        .encodeToString(Double.toString(score).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Dekoduje kursor na Double score. Zwraca null dla pierwszej strony lub bledu parsowania.
   */
  static Double decodeScoreCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String decoded = new String(
          Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      return Double.parseDouble(decoded);
    } catch (Exception ex) {
      log.debug("Invalid score cursor '{}' — returning null (first page)", cursor);
      return null;
    }
  }

  /**
   * Koduje Instant na kursor: Base64(epochMillis jako string).
   */
  static String encodeInstantCursor(Instant instant) {
    return Base64.getUrlEncoder()
        .encodeToString(
            Long.toString(instant.toEpochMilli()).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Dekoduje kursor na Instant. Zwraca null dla pierwszej strony lub bledu parsowania.
   */
  static Instant decodeInstantCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String decoded = new String(
          Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      return Instant.ofEpochMilli(Long.parseLong(decoded));
    } catch (Exception ex) {
      log.debug("Invalid instant cursor '{}' — returning null (first page)", cursor);
      return null;
    }
  }
}
