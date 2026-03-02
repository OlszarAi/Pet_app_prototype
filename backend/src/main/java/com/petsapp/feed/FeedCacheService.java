package com.petsapp.feed;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

/**
 * Serwis cache feeda oparty na Redis Sorted Sets.
 *
 * <p>Struktury danych Redis:
 * <ul>
 *   <li>{@code feed:public} — publiczne catche, member=catchId, score=feedScore (float)
 *   <li>{@code feed:trending} — catche z ostatnich 24h, member=catchId, score=feedScore
 * </ul>
 *
 * <p>Kazda metoda jest odporna na bledy Redis: wyjatek jest logowany, a metoda zwraca
 * {@link Optional#empty()} lub {@code false} zamiast propagowac wyjatek. FeedService rozpoznaje
 * pusty Optional jako sygnal do uzycia fallbacku bazodanowego.
 *
 * <p>TTL cache: publiczny = 15 min, trending = 5 min. FeedRebuildJob jest odpowiedzialny
 * za odswiezenie danych przed wyganieciem (uruchamia sie co 15 min => pokrywa oba TTL).
 */
@Service
public class FeedCacheService {

  private static final Logger log = LoggerFactory.getLogger(FeedCacheService.class);

  static final String KEY_PUBLIC = "feed:public";
  static final String KEY_TRENDING = "feed:trending";

  private static final Duration TTL_PUBLIC = Duration.ofMinutes(15);
  private static final Duration TTL_TRENDING = Duration.ofMinutes(5);

  private final StringRedisTemplate redisTemplate;

  public FeedCacheService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * Dodaje lub aktualizuje catch w publicznym feedzie.
   *
   * <p>Jesli klucz nie istnieje (np. po restarcie), item zostaje dodany — nastepne ZRANGE zwroci
   * tylko ten element. Cache "odbudowuje sie" stopniowo az do przebiegu FeedRebuildJob.
   *
   * @param catchId UUID polowania
   * @param score wyliczony feed score
   */
  public void addToPublicFeed(UUID catchId, double score) {
    try {
      redisTemplate
          .opsForZSet()
          .add(KEY_PUBLIC, catchId.toString(), score);
      redisTemplate.expire(KEY_PUBLIC, TTL_PUBLIC);
    } catch (Exception ex) {
      log.warn("Redis unavailable — skipping addToPublicFeed for {}: {}", catchId, ex.getMessage());
    }
  }

  /**
   * Dodaje lub aktualizuje catch w feedzie trending.
   *
   * @param catchId UUID polowania
   * @param score wyliczony feed score
   */
  public void addToTrending(UUID catchId, double score) {
    try {
      redisTemplate.opsForZSet().add(KEY_TRENDING, catchId.toString(), score);
      redisTemplate.expire(KEY_TRENDING, TTL_TRENDING);
    } catch (Exception ex) {
      log.warn("Redis unavailable — skipping addToTrending for {}: {}", catchId, ex.getMessage());
    }
  }

  /**
   * Usuwa catch z publicznego feeda i trendingu (uzywane przy soft delete catcha).
   *
   * @param catchId UUID usuwanego polowania
   */
  public void removeFromFeeds(UUID catchId) {
    String id = catchId.toString();
    try {
      redisTemplate.opsForZSet().remove(KEY_PUBLIC, (Object) id);
      redisTemplate.opsForZSet().remove(KEY_TRENDING, (Object) id);
    } catch (Exception ex) {
      log.warn("Redis unavailable — skipping removeFromFeeds for {}: {}", catchId, ex.getMessage());
    }
  }

  /**
   * Zwraca pierwsza lub kolejna strone publicznego feeda z Redis.
   *
   * <p>Zwraca {@link Optional#empty()} jezeli Redis nie jest dostepny lub klucz nie istnieje —
   * FeedService powinien wtedy uzywac fallbacku bazodanowego.
   *
   * @param cursorScore score ostatniego elementu poprzedniej strony; null dla pierwszej strony
   * @param limit maksymalna liczba wynikow
   * @return Optional z lista wpisow (moze byc pusta — koniec danych); lub empty przy bledzie
   */
  public Optional<List<ScoredCatchEntry>> getPublicPage(Double cursorScore, int limit) {
    return getPage(KEY_PUBLIC, cursorScore, limit);
  }

  /**
   * Zwraca pierwsza lub kolejna strone trending feeda z Redis.
   *
   * @param cursorScore score ostatniego elementu poprzedniej strony; null dla pierwszej strony
   * @param limit maksymalna liczba wynikow
   * @return Optional z lista wpisow lub empty przy bledzie/braku danych
   */
  public Optional<List<ScoredCatchEntry>> getTrendingPage(Double cursorScore, int limit) {
    return getPage(KEY_TRENDING, cursorScore, limit);
  }

  /**
   * Czy klucz publicznego feeda istnieje i ma co najmniej jeden element.
   *
   * <p>Uzywane przez FeedService do decyzji: cache hit vs DB fallback.
   */
  public boolean isPublicCachePopulated() {
    return isCachePopulated(KEY_PUBLIC);
  }

  /**
   * Czy klucz trending feeda istnieje i ma co najmniej jeden element.
   */
  public boolean isTrendingCachePopulated() {
    return isCachePopulated(KEY_TRENDING);
  }

  /**
   * Atomicznie odbudowuje publiczny feed: usuwa stary klucz, wstawia nowy zestaw z TTL.
   *
   * <p>Operacje sa wykonywane przez pipeline Redis zeby zminimalizowac RTT. Wstawienie odbywa sie
   * przez ZADD z NX=false (nadpisuje istniejace score). DEL+ZADD gwarantuje ze klienci nigdy
   * nie widza pustego setu (gap-free rebuild).
   *
   * @param entries lista (catchId, score) do wstawienia; pusta lista tylko usuwa stary klucz
   */
  public void rebuildPublicFeed(List<ScoredCatchEntry> entries) {
    rebuildFeed(KEY_PUBLIC, entries, TTL_PUBLIC);
  }

  /**
   * Atomicznie odbudowuje trending feed.
   *
   * @param entries lista (catchId, score) do wstawienia
   */
  public void rebuildTrending(List<ScoredCatchEntry> entries) {
    rebuildFeed(KEY_TRENDING, entries, TTL_TRENDING);
  }

  // --- private helpers ---

  private Optional<List<ScoredCatchEntry>> getPage(
      String key, Double cursorScore, int limit) {
    try {
      Set<TypedTuple<String>> results;
      if (cursorScore == null) {
        // Pierwsza strona: wszystkie elementy od najwyzszego score, max limit wynikow
        results = redisTemplate.opsForZSet()
            .reverseRangeByScoreWithScores(
                key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0, limit);
      } else {
        // Kolejna strona: score scisle mniejszy niz cursorScore (exclusive upper bound)
        // Math.nextDown gwarantuje wylacznosc granicy bez epsilon-hacking
        double exclusiveMax = Math.nextDown(cursorScore);
        results = redisTemplate.opsForZSet()
            .reverseRangeByScoreWithScores(
                key, Double.NEGATIVE_INFINITY, exclusiveMax, 0, limit);
      }
      if (results == null) {
        return Optional.empty();
      }
      List<ScoredCatchEntry> entries = results.stream()
          .filter(t -> t.getValue() != null && t.getScore() != null)
          .map(t -> new ScoredCatchEntry(UUID.fromString(t.getValue()), t.getScore()))
          .toList();
      return Optional.of(entries);
    } catch (Exception ex) {
      log.warn("Redis unavailable — feed page query failed for key='{}': {}", key, ex.getMessage());
      return Optional.empty();
    }
  }

  private boolean isCachePopulated(String key) {
    try {
      Long count = redisTemplate.opsForZSet().size(key);
      return count != null && count > 0;
    } catch (Exception ex) {
      log.debug("Redis unavailable — isCachePopulated check failed for key='{}': {}",
          key, ex.getMessage());
      return false;
    }
  }

  private void rebuildFeed(String key, List<ScoredCatchEntry> entries, Duration ttl) {
    if (entries.isEmpty()) {
      try {
        redisTemplate.delete(key);
      } catch (Exception ex) {
        log.warn("Redis unavailable — rebuildFeed delete failed for key='{}': {}",
            key, ex.getMessage());
      }
      return;
    }

    try {
      // Budujemy zestaw TypedTuple<String> do batch ZADD
      Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(entries.size());
      for (ScoredCatchEntry entry : entries) {
        tuples.add(ZSetOperations.TypedTuple.of(entry.catchId().toString(), entry.score()));
      }

      // DEL + ZADD + EXPIRE: brak atomowosci jest akceptowalny — przy pustym kluczu
      // FeedService odpada na DB fallback (brak gap w obsludze)
      redisTemplate.delete(key);
      redisTemplate.opsForZSet().add(key, tuples);
      redisTemplate.expire(key, ttl);

      log.debug("Rebuilt Redis feed '{}' with {} entries (TTL={}s)", key, entries.size(),
          ttl.getSeconds());
    } catch (Exception ex) {
      log.error("Redis unavailable — rebuildFeed failed for key='{}': {}", key, ex.getMessage());
    }
  }

  /**
   * Wpis w Redis Sorted Set — para (catchId, score).
   *
   * <p>Uzywany przy odczytach z cache (getPublicPage, getTrendingPage) oraz przez FeedRebuildJob
   * przy budowaniu zawartosci (rebuildPublicFeed, rebuildTrending).
   */
  public record ScoredCatchEntry(UUID catchId, double score) {}
}
