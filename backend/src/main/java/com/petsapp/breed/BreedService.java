package com.petsapp.breed;

import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import com.petsapp.catch_.DogCatchRepository;
import com.petsapp.user.UserNotFoundException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logika biznesowa dla ras i Pokedeksu uzytkownika.
 *
 * <p>Lista i szczegoły ras sa dostepne publicznie — nie wymagaja JWT. Endpoints Pokedeksu wymagaja
 * autentykacji; prywatne profile sa widoczne tylko przez ich wlasciciela.
 */
@Service
@Transactional(readOnly = true)
public class BreedService {

  private static final Logger log = LoggerFactory.getLogger(BreedService.class);

  private final BreedRepository breedRepository;
  private final DogCatchRepository dogCatchRepository;
  private final UserRepository userRepository;

  public BreedService(
      BreedRepository breedRepository,
      DogCatchRepository dogCatchRepository,
      UserRepository userRepository) {
    this.breedRepository = breedRepository;
    this.dogCatchRepository = dogCatchRepository;
    this.userRepository = userRepository;
  }

  /**
   * Zwraca liste aktywnych ras z opcjonalnym filtrowaniem.
   *
   * @param q fragment nazwy (EN lub PL), null = bez filtra
   * @param group grupa AKC/FCI, null = bez filtra
   * @param sizeCategory "small"|"medium"|"large", null = bez filtra
   */
  public List<BreedResponse> getBreeds(String q, String group, String sizeCategory) {
    String normalizedQ = (q != null && q.isBlank()) ? null : q;
    String normalizedGroup = (group != null && group.isBlank()) ? null : group;
    String normalizedSize = (sizeCategory != null && sizeCategory.isBlank()) ? null : sizeCategory;

    return breedRepository.findAllByFilter(normalizedQ, normalizedGroup, normalizedSize).stream()
        .map(BreedResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * Zwraca szczegoły rasy wraz z globalnymi statystykami catchow.
   *
   * @throws BreedNotFoundException jezeli rasa nie istnieje lub nie jest aktywna
   */
  public BreedDetailResponse getBreedById(int id) {
    Breed breed =
        breedRepository
            .findById(id)
            .filter(Breed::isActive)
            .orElseThrow(() -> new BreedNotFoundException(id));

    long totalCatchers = dogCatchRepository.countCatchersByBreedId(id);
    long totalCatches = dogCatchRepository.countTotalCatchesByBreedId(id);

    return BreedDetailResponse.from(breed, totalCatchers, totalCatches);
  }

  /**
   * Zwraca liste ras odkrytych przez uzytkownika (Pokedex).
   *
   * @param targetUserId ID uzytkownika, ktorego Pokedex chcemy zobaczyc
   * @param requesterId ID osoby pytajacej — jesli profil prywatny, musi byc rowny targetUserId
   * @throws UserNotFoundException jezeli uzytkownik nie istnieje
   * @throws PokedexAccessDeniedException jezeli profil jest prywatny i requester nie jest
   *     wlascicielem
   */
  public List<PokedexEntryResponse> getPokedex(UUID targetUserId, UUID requesterId) {
    User targetUser = findActiveUser(targetUserId);
    checkPokedexAccess(targetUser, requesterId);

    List<Object[]> breedCounts = dogCatchRepository.findBreedCatchCountsByUserId(targetUserId);

    if (breedCounts.isEmpty()) {
      return List.of();
    }

    Map<Integer, Long> countByBreedId =
        breedCounts.stream()
            .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));

    List<Integer> breedIds = List.copyOf(countByBreedId.keySet());
    List<Breed> breeds = breedRepository.findAllById(breedIds);

    return breeds.stream()
        .map(breed -> PokedexEntryResponse.from(breed, countByBreedId.get(breed.getId())))
        .sorted((a, b) -> Long.compare(b.catchCount(), a.catchCount()))
        .collect(Collectors.toList());
  }

  /**
   * Zwraca statystyki Pokedeksu uzytkownika: postep, streak, ulubiona rasa.
   *
   * @param targetUserId ID uzytkownika, ktorego statystyki chcemy zobaczyc
   * @param requesterId ID osoby pytajacej — jesli profil prywatny, musi byc rowny targetUserId
   */
  public PokedexStatsResponse getPokedexStats(UUID targetUserId, UUID requesterId) {
    User targetUser = findActiveUser(targetUserId);
    checkPokedexAccess(targetUser, requesterId);

    long uniqueBreeds = dogCatchRepository.countUniqueBreedsByUserId(targetUserId);
    long totalActiveBreeds = breedRepository.countActiveBreeds();
    long totalCatches = dogCatchRepository.countCatchesByUserId(targetUserId);

    double progressPercent =
        totalActiveBreeds > 0
            ? Math.round((uniqueBreeds * 100.0 / totalActiveBreeds) * 10.0) / 10.0
            : 0.0;

    int currentStreak = calculateStreak(targetUserId);

    BreedResponse favoriteBreed = findFavoriteBreed(targetUserId);

    return new PokedexStatsResponse(
        uniqueBreeds,
        totalActiveBreeds,
        progressPercent,
        totalCatches,
        currentStreak,
        favoriteBreed);
  }

  /**
   * Oblicza streak — liczbe kolejnych dni z przynajmniej jednym catchem, liczac wstecz od dnia
   * dzisiejszego lub wczorajszego (jesli dzisiaj brak catcha, streak jest niedawany jesli sie nie
   * zlamalo).
   */
  private int calculateStreak(UUID userId) {
    List<Date> rawDates = dogCatchRepository.findDistinctCatchDatesByUserId(userId);
    if (rawDates.isEmpty()) {
      return 0;
    }

    List<LocalDate> dates =
        rawDates.stream()
            .map(Date::toLocalDate)
            .sorted(java.util.Comparator.reverseOrder())
            .toList();

    LocalDate today = LocalDate.now();
    LocalDate mostRecent = dates.get(0);

    // Jesli ostatni catch jest starszy niz wczoraj, streak jest zerwany
    if (mostRecent.isBefore(today.minusDays(1))) {
      return 0;
    }

    int streak = 1;
    LocalDate expected = mostRecent.minusDays(1);

    for (int i = 1; i < dates.size(); i++) {
      if (dates.get(i).equals(expected)) {
        streak++;
        expected = expected.minusDays(1);
      } else {
        break;
      }
    }

    return streak;
  }

  private Optional<BreedResponse> findFavoriteBreedOptional(UUID userId) {
    Integer favoriteBreedId = dogCatchRepository.findFavoriteBreedIdByUserId(userId);
    if (favoriteBreedId == null) {
      return Optional.empty();
    }
    return breedRepository.findById(favoriteBreedId).map(BreedResponse::from);
  }

  private BreedResponse findFavoriteBreed(UUID userId) {
    return findFavoriteBreedOptional(userId).orElse(null);
  }

  private User findActiveUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(u -> u.getDeletedAt() == null)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
  }

  private void checkPokedexAccess(User targetUser, UUID requesterId) {
    if (targetUser.isPrivate() && !targetUser.getId().equals(requesterId)) {
      throw new PokedexAccessDeniedException(targetUser.getId());
    }
  }
}
