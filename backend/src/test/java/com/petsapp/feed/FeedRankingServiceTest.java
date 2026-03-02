package com.petsapp.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

/**
 * Testy jednostkowe formuly rankingowej FeedRankingService.
 *
 * <p>Brak kontekstu Spring — FeedRankingService jest serwisem bez stanu, wiec testy mozna
 * uruchamiac bez @SpringBootTest (szybciej, bez Testcontainers).
 */
class FeedRankingServiceTest {

  private final FeedRankingService service = new FeedRankingService();

  @Test
  void calculate_newCatchNoEngagement_returnsPositiveScore() {
    // Nowy catch z 0 lajkow, rarity=3: score = 9 / pow(2, 1.5) = 9 / 2.828 ≈ 3.18
    double score = service.calculate(0, 0, 3, Instant.now());

    assertThat(score).isGreaterThan(0.0);
    assertThat(score).isCloseTo(3.18, within(0.1));
  }

  @Test
  void calculate_highEngagementRecentCatch_returnsHigherScoreThanLowEngagement() {
    Instant now = Instant.now();

    double highEngagement = service.calculate(50, 20, 5, now);
    double lowEngagement = service.calculate(0, 0, 1, now);

    assertThat(highEngagement).isGreaterThan(lowEngagement);
  }

  @Test
  void calculate_sameEngagement_olderCatchHasLowerScore() {
    Instant recentCatch = Instant.now().minus(1, ChronoUnit.HOURS);
    Instant oldCatch = Instant.now().minus(48, ChronoUnit.HOURS);

    double recentScore = service.calculate(10, 5, 3, recentCatch);
    double oldScore = service.calculate(10, 5, 3, oldCatch);

    assertThat(recentScore).isGreaterThan(oldScore);
  }

  @Test
  void calculate_rareCatch_scoredHigherThanCommonCatchWithSameEngagement() {
    Instant now = Instant.now();

    double rareScore = service.calculate(5, 2, 5, now);    // rarity=5
    double commonScore = service.calculate(5, 2, 1, now);  // rarity=1

    assertThat(rareScore).isGreaterThan(commonScore);
  }

  @Test
  void calculate_commentWeighsMoreThanLike() {
    // 1 komentarz vs 2 lajki — komentarz (waga 2) == 2 lajki (2 * 1.0)
    // Dla rownosci: 0 lajkow 1 komentarz = 2.0, vs 2 lajki 0 komentarzy = 2.0
    Instant now = Instant.now();
    double oneComment = service.calculate(0, 1, 0, now);
    double twoLikes = service.calculate(2, 0, 0, now);

    // Wyniki powinny byc (praktycznie) rowne gdyby rarity=0, ale tu testujemy proporcje
    // 1 komentarz (engagement=2) vs 1 lajk (engagement=1) — komentarz wyzszy
    double commentScore = service.calculate(0, 1, 0, now);
    double likeScore = service.calculate(1, 0, 0, now);

    assertThat(commentScore).isGreaterThan(likeScore);
    // 2 lajki powinny rownowazyc 1 komentarz
    assertThat(oneComment).isCloseTo(twoLikes, within(0.001));
  }

  @Test
  void calculate_futureCaughAt_treatedAsFreshCatch() {
    // Catch z przyszloscia (blad zegara) — hoursAge = 0, score jak dla swiezego catcha
    Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
    double futureScore = service.calculate(10, 5, 3, future);
    double nowScore = service.calculate(10, 5, 3, Instant.now());

    // Oba powinny byc blisko siebie (0h vs ~0h)
    assertThat(futureScore).isCloseTo(nowScore, within(1.0));
    assertThat(futureScore).isGreaterThan(0.0);
  }

  @Test
  void calculate_formula_matchesExpectedValue() {
    // score = (10*1.0 + 3*2.0 + 4*3.0) / pow((1+2), 1.5)
    //       = (10 + 6 + 12) / pow(3, 1.5)
    //       = 28 / 5.196 ≈ 5.39
    Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

    double score = service.calculate(10, 3, 4, oneHourAgo);

    assertThat(score).isCloseTo(5.39, within(0.1));
  }

  @Test
  void calculate_zeroRarity_doesNotBreak() {
    // Dopuszczamy rarity=0 edge case (nie powinien wystapic w produkcji, ale formula musi dzialac)
    double score = service.calculate(5, 5, 0, Instant.now());

    assertThat(score).isGreaterThanOrEqualTo(0.0);
    assertThat(Double.isNaN(score)).isFalse();
    assertThat(Double.isInfinite(score)).isFalse();
  }
}
