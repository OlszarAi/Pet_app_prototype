package com.petsapp.feed;

import com.petsapp.catch_.DogCatch;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Oblicza ranking score polowania do sortowania feeda.
 *
 * <p>Formula rankingowa:
 *
 * <pre>
 *   score = (likes * 1.0 + comments * 2.0 + rarity * 3.0) / (hours_age + 2) ^ 1.5
 * </pre>
 *
 * <p>Uzasadnienie wspolczynnikow:
 * <ul>
 *   <li>Komentarze (x2) sa cenniejsze niz lajki — wyrazaja wyzsze zaangazowanie.
 *   <li>Rzadkosc rasy (x3) promuje rzadkie polowania nawet bez zaangazowania.
 *   <li>Wykladnik czasu 1.5 sprawia ze score spada szybciej niz liniowo — zapobiega dominacji
 *       starych viralowych postow. Staly offset +2h daje nowym catchom czas na zebranie lajkow.
 * </ul>
 *
 * <p>Klasa jest serwisem bez stanu — wszystkie metody sa deterministyczne i testowalne jednostkowo.
 */
@Service
public class FeedRankingService {

  private static final double LIKE_WEIGHT = 1.0;
  private static final double COMMENT_WEIGHT = 2.0;
  private static final double RARITY_WEIGHT = 3.0;
  private static final double TIME_OFFSET_HOURS = 2.0;
  private static final double TIME_DECAY_EXPONENT = 1.5;

  /**
   * Oblicza feed score na podstawie licznikow zaangazowania, rzadkosci rasy i wieku posta.
   *
   * @param likeCount liczba polubien
   * @param commentCount liczba komentarzy
   * @param rarityScore rzadkosc rasy (1-5, gdzie 5 = najrzadsza)
   * @param caughtAt czas zlowienia psa
   * @return score >= 0; wyzszy = wyzej w feedzie
   */
  public double calculate(int likeCount, int commentCount, int rarityScore, Instant caughtAt) {
    double hoursAge = toHoursElapsed(caughtAt);
    double engagement = likeCount * LIKE_WEIGHT + commentCount * COMMENT_WEIGHT
        + rarityScore * RARITY_WEIGHT;
    return engagement / Math.pow(hoursAge + TIME_OFFSET_HOURS, TIME_DECAY_EXPONENT);
  }

  /**
   * Oblicza feed score bezposrednio z encji DogCatch.
   *
   * <p>Metoda oczekuje zaladowanej relacji {@code breed} (nie lazy proxy) bo potrzebuje
   * {@code rarityScore}. FeedRebuildJob uzywa JOIN FETCH, wiec jest to bezpieczne.
   *
   * @param dogCatch encja polowania z zaladowana rasa
   * @return score >= 0
   */
  public double calculate(DogCatch dogCatch) {
    return calculate(
        dogCatch.getLikeCount(),
        dogCatch.getCommentCount(),
        dogCatch.getBreed().getRarityScore(),
        dogCatch.getCaughtAt());
  }

  /**
   * Oblicza liczbe godzin od podanego momentu do teraz.
   *
   * <p>Zwraca co najmniej 0 — zabezpieczenie przed catchami z przyszlosca (blad zegara).
   */
  private double toHoursElapsed(Instant from) {
    long millis = Duration.between(from, Instant.now()).toMillis();
    return Math.max(0.0, millis / 3_600_000.0);
  }
}
