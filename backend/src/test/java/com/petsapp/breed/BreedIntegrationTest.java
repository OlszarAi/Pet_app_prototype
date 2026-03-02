package com.petsapp.breed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.petsapp.AbstractIntegrationTest;
import com.petsapp.auth.AuthService;
import com.petsapp.auth.EmailService;
import com.petsapp.auth.RegisterRequest;
import com.petsapp.auth.UserRepository;
import com.petsapp.auth.VerifyEmailRequest;
import com.petsapp.catch_.DogCatch;
import com.petsapp.catch_.DogCatchRepository;
import com.petsapp.user.UserNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla BreedService z prawdziwa baza PostgreSQL (Testcontainers).
 *
 * <p>Testy dzialaja na danych seedowanych przez V2 i V3 migracje Flyway — nie tworzymy ras w
 * testach. DogCatch sa tworzone przez repository aby sprawdzic logike Pokedeksu bez pelnego
 * CatchService (Krok 6).
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BreedIntegrationTest extends AbstractIntegrationTest {

  @Autowired private BreedService breedService;
  @Autowired private BreedRepository breedRepository;
  @Autowired private DogCatchRepository dogCatchRepository;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;

  @MockBean private EmailService emailService;

  private static final String TEST_PASSWORD = "SecurePassword123";

  @BeforeEach
  void setupMocks() {
    doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
  }

  // -----------------------------------------------
  // getBreeds — lista i filtrowanie
  // -----------------------------------------------

  @Test
  void getBreeds_withNoFilter_returnsAllActiveBreeds() {
    List<BreedResponse> breeds = breedService.getBreeds(null, null, null);

    // V2 ma 10 ras, V3 dodaje wiecej — razem powinno byc ponad 10
    assertThat(breeds).hasSizeGreaterThan(10);
    assertThat(breeds).allMatch(b -> b.name() != null);
    assertThat(breeds).allMatch(b -> b.rarityScore() >= 1 && b.rarityScore() <= 5);
  }

  @Test
  void getBreeds_withNameQuery_returnsMatchingBreeds() {
    List<BreedResponse> breeds = breedService.getBreeds("golden", null, null);

    assertThat(breeds).hasSize(1);
    assertThat(breeds.get(0).name()).isEqualTo("Golden Retriever");
  }

  @Test
  void getBreeds_withNameQueryPolish_returnsMatchingBreeds() {
    // V2 ma Owczarek Niemecki (name_pl)
    List<BreedResponse> breeds = breedService.getBreeds("owczarek", null, null);

    assertThat(breeds).hasSizeGreaterThanOrEqualTo(1);
    assertThat(breeds)
        .anyMatch(
            b ->
                b.namePl().toLowerCase().contains("owczarek")
                    || b.name().toLowerCase().contains("shepherd"));
  }

  @Test
  void getBreeds_withGroupFilter_returnsOnlyMatchingGroup() {
    List<BreedResponse> breeds = breedService.getBreeds(null, "Herding", null);

    assertThat(breeds).isNotEmpty();
    assertThat(breeds).allMatch(b -> "Herding".equals(b.group()));
  }

  @Test
  void getBreeds_withSizeFilter_returnsOnlyMatchingSize() {
    List<BreedResponse> breeds = breedService.getBreeds(null, null, "small");

    assertThat(breeds).isNotEmpty();
    assertThat(breeds).allMatch(b -> "small".equals(b.sizeCategory()));
  }

  @Test
  void getBreeds_withBlankQuery_treatedAsNoFilter() {
    List<BreedResponse> breedsBlank = breedService.getBreeds("  ", null, null);
    List<BreedResponse> breedsNull = breedService.getBreeds(null, null, null);

    assertThat(breedsBlank).hasSameSizeAs(breedsNull);
  }

  @Test
  void getBreeds_withNonMatchingQuery_returnsEmptyList() {
    List<BreedResponse> breeds = breedService.getBreeds("zzz_no_such_breed_xyz", null, null);

    assertThat(breeds).isEmpty();
  }

  // -----------------------------------------------
  // getBreedById — szczegoly rasy
  // -----------------------------------------------

  @Test
  void getBreedById_withExistingBreed_returnsDetailWithStats() {
    Breed goldenRetriever = breedRepository.findAllByFilter("golden retriever", null, null).get(0);

    BreedDetailResponse detail = breedService.getBreedById(goldenRetriever.getId());

    assertThat(detail.id()).isEqualTo(goldenRetriever.getId());
    assertThat(detail.name()).isEqualTo("Golden Retriever");
    assertThat(detail.totalCatches()).isZero();
    assertThat(detail.totalCatchers()).isZero();
  }

  @Test
  void getBreedById_withNonExistentId_throwsBreedNotFoundException() {
    int nonExistentId = 999_999;

    assertThatThrownBy(() -> breedService.getBreedById(nonExistentId))
        .isInstanceOf(BreedNotFoundException.class)
        .hasMessageContaining(String.valueOf(nonExistentId));
  }

  // -----------------------------------------------
  // getPokedex — pokedex uzytkownika
  // -----------------------------------------------

  @Test
  void getPokedex_withNoCatches_returnsEmptyList() {
    UUID userId = createVerifiedUser();

    List<PokedexEntryResponse> pokedex = breedService.getPokedex(userId, userId);

    assertThat(pokedex).isEmpty();
  }

  @Test
  void getPokedex_withCatches_returnsDiscoveredBreeds() {
    UUID userId = createVerifiedUser();
    Breed breed = breedRepository.findAllByFilter("beagle", null, null).get(0);

    addCatch(userId, breed);
    addCatch(userId, breed);

    List<PokedexEntryResponse> pokedex = breedService.getPokedex(userId, userId);

    assertThat(pokedex).hasSize(1);
    assertThat(pokedex.get(0).breedId()).isEqualTo(breed.getId());
    assertThat(pokedex.get(0).catchCount()).isEqualTo(2);
  }

  @Test
  void getPokedex_withMultipleBreeds_sortedByCatchCountDesc() {
    UUID userId = createVerifiedUser();
    Breed breed1 = breedRepository.findAllByFilter("beagle", null, null).get(0);
    Breed breed2 = breedRepository.findAllByFilter("boxer", null, null).get(0);

    addCatch(userId, breed1);
    addCatch(userId, breed1);
    addCatch(userId, breed1);
    addCatch(userId, breed2);

    List<PokedexEntryResponse> pokedex = breedService.getPokedex(userId, userId);

    assertThat(pokedex).hasSize(2);
    assertThat(pokedex.get(0).catchCount()).isGreaterThanOrEqualTo(pokedex.get(1).catchCount());
    assertThat(pokedex.get(0).breedId()).isEqualTo(breed1.getId());
  }

  @Test
  void getPokedex_forPrivateProfile_withDifferentRequester_throwsPokedexAccessDeniedException() {
    UUID ownerId = createVerifiedUser();
    UUID requesterId = createVerifiedUser();

    // Ustaw profil jako prywatny bezposrednio przez repository
    userRepository
        .findById(ownerId)
        .ifPresent(
            user -> {
              user.updatePrivacy(true);
              userRepository.save(user);
            });

    assertThatThrownBy(() -> breedService.getPokedex(ownerId, requesterId))
        .isInstanceOf(PokedexAccessDeniedException.class);
  }

  @Test
  void getPokedex_forPrivateProfile_asOwner_succeeds() {
    UUID userId = createVerifiedUser();
    userRepository
        .findById(userId)
        .ifPresent(
            user -> {
              user.updatePrivacy(true);
              userRepository.save(user);
            });

    List<PokedexEntryResponse> pokedex = breedService.getPokedex(userId, userId);

    assertThat(pokedex).isEmpty();
  }

  // -----------------------------------------------
  // getPokedexStats — statystyki
  // -----------------------------------------------

  @Test
  void getPokedexStats_withNoCatches_returnsZeroedStats() {
    UUID userId = createVerifiedUser();

    PokedexStatsResponse stats = breedService.getPokedexStats(userId, userId);

    assertThat(stats.uniqueBreeds()).isZero();
    assertThat(stats.totalCatches()).isZero();
    assertThat(stats.progressPercent()).isZero();
    assertThat(stats.currentStreak()).isZero();
    assertThat(stats.favoriteBreed()).isNull();
    assertThat(stats.totalActiveBreeds()).isGreaterThan(10);
  }

  @Test
  void getPokedexStats_withCatches_returnsCorrectStats() {
    UUID userId = createVerifiedUser();
    Breed breed = breedRepository.findAllByFilter("pug", null, null).get(0);

    addCatch(userId, breed);
    addCatch(userId, breed);

    PokedexStatsResponse stats = breedService.getPokedexStats(userId, userId);

    assertThat(stats.uniqueBreeds()).isEqualTo(1);
    assertThat(stats.totalCatches()).isEqualTo(2);
    assertThat(stats.progressPercent()).isGreaterThan(0.0);
    assertThat(stats.favoriteBreed()).isNotNull();
    assertThat(stats.favoriteBreed().name()).isEqualTo("Pug");
  }

  @Test
  void getPokedexStats_withNonExistentUser_throwsUserNotFoundException() {
    UUID nonExistentId = UUID.randomUUID();

    assertThatThrownBy(() -> breedService.getPokedexStats(nonExistentId, nonExistentId))
        .isInstanceOf(UserNotFoundException.class);
  }

  // -----------------------------------------------
  // Metody pomocnicze
  // -----------------------------------------------

  /**
   * Rejestruje i weryfikuje uzytkownika. Zwraca jego ID.
   *
   * <p>Email i username sa losowe aby testy nie kolidowaly ze soba.
   */
  private UUID createVerifiedUser() {
    String email = "breed-test-" + UUID.randomUUID() + "@example.com";
    String username = "breedtst" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String code = "123456";

    when(emailService.generateVerificationCode()).thenReturn(code);
    when(emailService.verificationCodeExpiry()).thenReturn(Instant.now().plusSeconds(900));

    authService.register(new RegisterRequest(email, username, TEST_PASSWORD));
    authService.verifyEmail(new VerifyEmailRequest(email, code));

    return userRepository.findActiveByEmail(email).orElseThrow().getId();
  }

  /** Zapisuje DogCatch dla danego uzytkownika i rasy. */
  private void addCatch(UUID userId, Breed breed) {
    com.petsapp.auth.User user = userRepository.findById(userId).orElseThrow();
    DogCatch dogCatch =
        DogCatch.builder()
            .user(user)
            .breed(breed)
            .photoUrl("http://localhost:9000/test/photo.jpg")
            .thumbnailUrl("http://localhost:9000/test/thumb.jpg")
            .isPublic(true)
            .caughtAt(Instant.now().minus(1, ChronoUnit.HOURS))
            .build();

    dogCatchRepository.save(dogCatch);
  }
}
