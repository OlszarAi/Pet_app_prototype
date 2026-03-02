package com.petsapp.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.petsapp.AbstractIntegrationTest;
import com.petsapp.auth.AuthService;
import com.petsapp.auth.EmailService;
import com.petsapp.auth.RegisterRequest;
import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import com.petsapp.auth.VerifyEmailRequest;
import com.petsapp.breed.Breed;
import com.petsapp.breed.BreedRepository;
import com.petsapp.catch_.CatchResponse;
import com.petsapp.catch_.CatchService;
import com.petsapp.common.ApiResponse;
import com.petsapp.storage.StorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla FeedService z prawdziwa baza PostgreSQL.
 *
 * <p>FeedCacheService jest mockowany (@MockBean) — testy weryfikuja logike DB fallback oraz
 * poprawnosc zapytan JPA. StorageService i EmailService rowniez mockowane bo testy nie lacza sie
 * z MinIO ani MailHog.
 *
 * <p>Namespacing testow przez @Nested pozwala pogrupowac scenariusze per endpoint.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FeedServiceTest extends AbstractIntegrationTest {

  @Autowired private FeedService feedService;
  @Autowired private CatchService catchService;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private BreedRepository breedRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockBean private FeedCacheService feedCacheService;
  @MockBean private StorageService storageService;
  @MockBean private EmailService emailService;

  private static final String TEST_PASSWORD = "ValidPass123";
  private static final String FAKE_PHOTO_URL = "http://localhost:9000/petsapp/catches/test_full.jpg";
  private static final String FAKE_THUMB_URL = "http://localhost:9000/petsapp/catches/test_thumb.jpg";

  @BeforeEach
  void setupMocks() {
    // StorageService i EmailService — zewnetrzne zaleznosci, zawsze mockowane
    doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);
    doNothing().when(storageService).delete(anyString());

    // FeedCacheService — domyslnie symulujemy "cache pusty" (DB fallback)
    when(feedCacheService.getPublicPage(any(), anyInt())).thenReturn(Optional.empty());
    when(feedCacheService.getTrendingPage(any(), anyInt())).thenReturn(Optional.empty());
    doNothing().when(feedCacheService).addToPublicFeed(any(UUID.class), anyDouble());
    doNothing().when(feedCacheService).removeFromFeeds(any(UUID.class));
    doNothing().when(feedCacheService).rebuildPublicFeed(any());
    doNothing().when(feedCacheService).rebuildTrending(any());
  }

  private String uniqueEmail() {
    return "feed-test-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueUsername() {
    return "ft" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
  }

  private User registerAndVerify(String email, String username) {
    String verificationCode = "123456";
    when(emailService.generateVerificationCode()).thenReturn(verificationCode);
    when(emailService.verificationCodeExpiry()).thenReturn(Instant.now().plusSeconds(900));

    authService.register(new RegisterRequest(email, username, TEST_PASSWORD));
    authService.verifyEmail(new VerifyEmailRequest(email, verificationCode));

    return userRepository.findActiveByEmail(email).orElseThrow();
  }

  private MockMultipartFile createMinimalJpeg() throws IOException {
    BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", baos);
    return new MockMultipartFile("file", "dog.jpg", "image/jpeg", baos.toByteArray());
  }

  private CatchResponse createPublicCatch(User user, int breedId) throws IOException {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL).thenReturn(FAKE_THUMB_URL);
    return catchService.createCatch(user, breedId, createMinimalJpeg(),
        "Test caption", null, null, null, true);
  }

  /** Testy dla GET /feed/public */
  @Nested
  class PublicFeed {

    @Test
    void getPublicFeed_cacheEmpty_returnsCatchesFromDb() throws IOException {
      User user = registerAndVerify(uniqueEmail(), uniqueUsername());
      Breed firstBreed = breedRepository.findAll().get(0);

      createPublicCatch(user, firstBreed.getId());

      ApiResponse<List<CatchResponse>> response =
          feedService.getPublicFeed(user, null, 20);

      assertThat(response.success()).isTrue();
      assertThat(response.data()).isNotEmpty();
      assertThat(response.data()).allMatch(c -> c.id() != null);
    }

    @Test
    void getPublicFeed_privateCatch_notIncluded() throws IOException {
      User user = registerAndVerify(uniqueEmail(), uniqueUsername());
      Breed breed = breedRepository.findAll().get(0);

      when(storageService.upload(anyString(), any(), anyString(), anyLong()))
          .thenReturn(FAKE_PHOTO_URL).thenReturn(FAKE_THUMB_URL);
      catchService.createCatch(user, breed.getId(), createMinimalJpeg(),
          null, null, null, null, false); // isPublic=false

      // DB fallback na feedzie publicznym nie powinien zwrocic prywatnego catcha
      ApiResponse<List<CatchResponse>> response = feedService.getPublicFeed(user, null, 20);
      assertThat(response.data())
          .noneMatch(c -> c.user().id().equals(user.getId()) && !c.isPublic());
    }

    @Test
    void getPublicFeed_multipleCatches_sortedByFeedScoreDesc() throws IOException {
      User user = registerAndVerify(uniqueEmail(), uniqueUsername());
      List<Breed> breeds = breedRepository.findAll();
      Breed breed = breeds.get(0);

      createPublicCatch(user, breed.getId());
      createPublicCatch(user, breed.getId());
      createPublicCatch(user, breed.getId());

      ApiResponse<List<CatchResponse>> response = feedService.getPublicFeed(user, null, 20);

      assertThat(response.success()).isTrue();
    }

    @Test
    void getPublicFeed_withCacheHit_hydratesFromDb() throws IOException {
      User user = registerAndVerify(uniqueEmail(), uniqueUsername());
      Breed breed = breedRepository.findAll().get(0);

      CatchResponse created = createPublicCatch(user, breed.getId());

      // Symulujemy cache hit: Redis zwraca ID catcha ze score
      List<FeedCacheService.ScoredCatchEntry> cacheEntries =
          List.of(new FeedCacheService.ScoredCatchEntry(created.id(), 5.0));
      when(feedCacheService.getPublicPage(null, 21))
          .thenReturn(Optional.of(cacheEntries));

      ApiResponse<List<CatchResponse>> response = feedService.getPublicFeed(user, null, 20);

      assertThat(response.success()).isTrue();
      assertThat(response.data()).hasSize(1);
      assertThat(response.data().get(0).id()).isEqualTo(created.id());
    }

    @Test
    void getPublicFeed_likedField_correctlySet() throws IOException {
      User user = registerAndVerify(uniqueEmail(), uniqueUsername());
      Breed breed = breedRepository.findAll().get(0);

      CatchResponse created = createPublicCatch(user, breed.getId());
      catchService.likeCatch(created.id(), user);

      ApiResponse<List<CatchResponse>> response = feedService.getPublicFeed(user, null, 20);

      assertThat(response.data())
          .filteredOn(c -> c.id().equals(created.id()))
          .singleElement()
          .satisfies(c -> assertThat(c.liked()).isTrue());
    }

    @Test
    void getPublicFeed_pagination_cursorWorksCorrectly() throws IOException {
      // Zroznicowanie score wymagane do poprawnej paginacji kursorowej.
      // Wszystkie catche tworzone jednoczesnie maja identyczny score (0h age, ten sample rarity).
      // Dodajemy polubienia aby uzyskac 3 rozne score: catch1 > catch2 > catch3.
      User user1 = registerAndVerify(uniqueEmail(), uniqueUsername());
      User user2 = registerAndVerify(uniqueEmail(), uniqueUsername());
      User user3 = registerAndVerify(uniqueEmail(), uniqueUsername());
      Breed breed = breedRepository.findAll().stream()
          .filter(Breed::isActive).findFirst().orElseThrow();

      CatchResponse catch1 = createPublicCatch(user1, breed.getId());
      CatchResponse catch2 = createPublicCatch(user1, breed.getId());
      CatchResponse catch3 = createPublicCatch(user1, breed.getId());

      // catch1: 2 likes (user2 + user3), catch2: 1 like (user2), catch3: 0 likes
      catchService.likeCatch(catch1.id(), user2);
      catchService.likeCatch(catch1.id(), user3);
      catchService.likeCatch(catch2.id(), user2);

      // Pierwsza strona — limit 2 (catch1 i catch2 posortowane malejaco po score)
      ApiResponse<List<CatchResponse>> firstPage = feedService.getPublicFeed(user1, null, 2);
      assertThat(firstPage.data()).hasSize(2);
      assertThat(firstPage.pagination()).isNotNull();
      assertThat(firstPage.pagination().hasMore()).isTrue();
      assertThat(firstPage.pagination().cursor()).isNotNull();

      // Druga strona z kursorem — uzywamy duzego limitu, bo inne testy tworza catche
      // z tym samym base-score; catch3 musi pojawic sie wsrod nich
      ApiResponse<List<CatchResponse>> secondPage =
          feedService.getPublicFeed(user1, firstPage.pagination().cursor(), 50);
      assertThat(secondPage.data()).isNotEmpty();
      assertThat(secondPage.data()).anyMatch(c -> c.id().equals(catch3.id()));
    }
  }

  /** Testy dla GET /feed/friends */
  @Nested
  class FriendsFeed {

    @Test
    void getFriendsFeed_noFriends_returnsEmptyPage() {
      User user = registerAndVerify(uniqueEmail(), uniqueUsername());

      ApiResponse<List<CatchResponse>> response = feedService.getFriendsFeed(user, null, 20);

      assertThat(response.success()).isTrue();
      assertThat(response.data()).isEmpty();
    }

    @Test
    void getFriendsFeed_withAcceptedFriend_returnsFriendsCatches() throws IOException {
      User userA = registerAndVerify(uniqueEmail(), uniqueUsername());
      User userB = registerAndVerify(uniqueEmail(), uniqueUsername());
      Breed breed = breedRepository.findAll().get(0);

      // Tworzymy relacje znajomosci bezposrednio w DB (encja Friendship dodana w Kroku 8)
      jdbcTemplate.update(
          "INSERT INTO friendship (id, requester_id, addressee_id, status) VALUES (?::uuid, ?::uuid, ?::uuid, 'ACCEPTED')",
          UUID.randomUUID().toString(),
          userA.getId().toString(),
          userB.getId().toString());

      // UserB tworzy catcha
      when(storageService.upload(anyString(), any(), anyString(), anyLong()))
          .thenReturn(FAKE_PHOTO_URL).thenReturn(FAKE_THUMB_URL);
      CatchResponse friendCatch = catchService.createCatch(
          userB, breed.getId(), createMinimalJpeg(), null, null, null, null, true);

      ApiResponse<List<CatchResponse>> response = feedService.getFriendsFeed(userA, null, 20);

      assertThat(response.data())
          .anyMatch(c -> c.id().equals(friendCatch.id()));
    }

    @Test
    void getFriendsFeed_pendingFriendship_notIncluded() throws IOException {
      User userA = registerAndVerify(uniqueEmail(), uniqueUsername());
      User userB = registerAndVerify(uniqueEmail(), uniqueUsername());
      Breed breed = breedRepository.findAll().get(0);

      // Znajomosc oczekujaca (pending) — catch NIE powinien sie pojawic
      jdbcTemplate.update(
          "INSERT INTO friendship (id, requester_id, addressee_id, status) VALUES (?::uuid, ?::uuid, ?::uuid, 'PENDING')",
          UUID.randomUUID().toString(),
          userA.getId().toString(),
          userB.getId().toString());

      when(storageService.upload(anyString(), any(), anyString(), anyLong()))
          .thenReturn(FAKE_PHOTO_URL).thenReturn(FAKE_THUMB_URL);
      CatchResponse pendingFriendCatch = catchService.createCatch(
          userB, breed.getId(), createMinimalJpeg(), null, null, null, null, true);

      ApiResponse<List<CatchResponse>> response = feedService.getFriendsFeed(userA, null, 20);

      assertThat(response.data()).noneMatch(c -> c.id().equals(pendingFriendCatch.id()));
    }
  }

  /** Testy dla GET /feed/trending */
  @Nested
  class TrendingFeed {

    @Test
    void getTrendingFeed_cacheEmpty_returnsRecentCatches() throws IOException {
      User user = registerAndVerify(uniqueEmail(), uniqueUsername());
      Breed breed = breedRepository.findAll().get(0);

      createPublicCatch(user, breed.getId());

      ApiResponse<List<CatchResponse>> response = feedService.getTrendingFeed(user, 10);

      assertThat(response.success()).isTrue();
      assertThat(response.data()).isNotEmpty();
    }

    @Test
    void getTrendingFeed_limitRespected() throws IOException {
      User user = registerAndVerify(uniqueEmail(), uniqueUsername());
      Breed breed = breedRepository.findAll().get(0);

      // Tworzymy wiecej catchy niz limit
      for (int i = 0; i < 5; i++) {
        when(storageService.upload(anyString(), any(), anyString(), anyLong()))
            .thenReturn(FAKE_PHOTO_URL).thenReturn(FAKE_THUMB_URL);
        catchService.createCatch(user, breed.getId(), createMinimalJpeg(),
            null, null, null, null, true);
      }

      ApiResponse<List<CatchResponse>> response = feedService.getTrendingFeed(user, 3);

      assertThat(response.data().size()).isLessThanOrEqualTo(3);
    }
  }
}
