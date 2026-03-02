package com.petsapp.feed;

import com.petsapp.catch_.DogCatch;
import com.petsapp.catch_.DogCatchRepository;
import com.petsapp.feed.FeedCacheService.ScoredCatchEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zadanie cykliczne odswiezajace score'y feeda i przebudowujace cache Redis.
 *
 * <p>Logika:
 * <ol>
 *   <li>Pobiera wszystkie publiczne, niezdeaktywowane polowania z ostatnich 7 dni (z zamontowana
 *       rasa — JOIN FETCH aby uniknac N+1 przy obliczaniu score przez FeedRankingService).
 *   <li>Oblicza nowe FeedScore dla kazdego polowania wg formuly:
 *       {@code (likes + comments*2 + rarity*3) / (hours+2)^1.5}
 *   <li>Aktualizuje {@code DogCatch.feedScore} w bazie danych (jeden saveAll).
 *   <li>Atomicznie przebudowuje {@code feed:public} w Redis (DEL + ZADD + EXPIRE przez pipeline).
 *   <li>Filtruje subset ostatnich 24h na trending i przebudowuje {@code feed:trending}.
 * </ol>
 *
 * <p>Horyzont 7 dni w feedzie publicznym to kompromis: starsze catche maja bardzo niski score
 * (mianownik rosnie szybko) i wypadaja naturalnie na dno. Przechowywanie starszych danych w
 * Redis niepotrzebnie zwiekszyloby zuzycie pamieci.
 *
 * <p>Job uzywa {@code @Scheduled(fixedDelay)} zamiast {@code cron} — jesli poprzednie wykonanie
 * trwalo dluzej niz 15 min, nastepne startuje dopiero po zakonczeniu (bez overlap).
 */
@Component
public class FeedRebuildJob {

  private static final Logger log = LoggerFactory.getLogger(FeedRebuildJob.class);

  /** Horyzont czasowy polowania uwzglednianych w feedzie publicznym. */
  private static final long PUBLIC_FEED_HORIZON_DAYS = 7;

  /** Horyzont czasowy polowania uwzglednianych w feedzie trending. */
  private static final long TRENDING_HORIZON_HOURS = 24;

  /** Opoznienie miedzy zakonczeniem poprzedniego a startem kolejnego przebiegu (ms). */
  private static final long REBUILD_INTERVAL_MS = 15 * 60 * 1_000;

  private final DogCatchRepository catchRepository;
  private final FeedRankingService rankingService;
  private final FeedCacheService feedCacheService;

  public FeedRebuildJob(
      DogCatchRepository catchRepository,
      FeedRankingService rankingService,
      FeedCacheService feedCacheService) {
    this.catchRepository = catchRepository;
    this.rankingService = rankingService;
    this.feedCacheService = feedCacheService;
  }

  /**
   * Glowny przebieg przebudowy feeda.
   *
   * <p>Uruchamiany co 15 min (fixedDelay — nie ma overlap w razie opoznienia). Inicjalny delay
   * 30 sekund daje czas na pelne uruchomienie aplikacji i polaczenie z Redis.
   *
   * <p>Adnotacja {@code @Transactional} obejmuje tylko czesc bazodanowa (pobieranie + saveAll).
   * Operacje Redis sa poza transakcja JPA — Redis nie uczestniczy w rollbacku.
   */
  @Scheduled(fixedDelay = REBUILD_INTERVAL_MS, initialDelay = 30_000)
  @Transactional
  public void rebuildFeeds() {
    Instant publicHorizon = Instant.now().minusSeconds(PUBLIC_FEED_HORIZON_DAYS * 86_400);
    Instant trendingHorizon = Instant.now().minusSeconds(TRENDING_HORIZON_HOURS * 3_600);

    log.info("FeedRebuildJob started — loading public catches since {}", publicHorizon);
    long start = System.currentTimeMillis();

    List<DogCatch> publicCatches = catchRepository.findAllPublicForFeedRebuild(publicHorizon);
    log.debug("FeedRebuildJob loaded {} catches for scoring", publicCatches.size());

    List<ScoredCatchEntry> publicEntries = new ArrayList<>(publicCatches.size());
    List<ScoredCatchEntry> trendingEntries = new ArrayList<>();

    for (DogCatch dogCatch : publicCatches) {
      double score = rankingService.calculate(dogCatch);
      dogCatch.updateFeedScore(score);
      publicEntries.add(new ScoredCatchEntry(dogCatch.getId(), score));
      if (!dogCatch.getCaughtAt().isBefore(trendingHorizon)) {
        trendingEntries.add(new ScoredCatchEntry(dogCatch.getId(), score));
      }
    }

    // Commit updated feedScores to DB (batch UPDATE przez JPA)
    catchRepository.saveAll(publicCatches);

    // Przebuduj Redis poza transakcja JPA (wywolania po saveAll w ramach tej samej metody
    // — Spring commituje transakcje dopiero po zakonczeniu metody, ale Redis jest poza JPA)
    feedCacheService.rebuildPublicFeed(publicEntries);
    feedCacheService.rebuildTrending(trendingEntries);

    long elapsed = System.currentTimeMillis() - start;
    log.info(
        "FeedRebuildJob finished in {}ms — public={}, trending={}",
        elapsed,
        publicEntries.size(),
        trendingEntries.size());
  }
}
