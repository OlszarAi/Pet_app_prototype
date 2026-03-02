package com.petsapp.catch_;

import com.petsapp.auth.ConflictException;
import com.petsapp.auth.RateLimitService;
import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import com.petsapp.breed.Breed;
import com.petsapp.breed.BreedNotFoundException;
import com.petsapp.breed.BreedRepository;
import com.petsapp.common.ApiResponse;
import com.petsapp.feed.FeedCacheService;
import com.petsapp.feed.FeedRankingService;
import com.petsapp.storage.StorageService;
import com.petsapp.user.UnsupportedFileFormatException;
import com.petsapp.user.UserNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Logika biznesowa dla operacji na polowaniach psow.
 *
 * <p>Obsluguje: tworzenie catcha z przetwarzaniem zdjecia, soft delete, polubienia, komentarze
 * oraz zgloszenia. Denormalizacja licznikow (likeCount, commentCount, totalCatches, uniqueBreeds)
 * jest aktualizowana w tej samej transakcji co operacja bazowa.
 */
@Service
public class CatchService {

  private static final Logger log = LoggerFactory.getLogger(CatchService.class);

  private static final Set<String> ALLOWED_PHOTO_MIME_TYPES = Set.of("image/jpeg", "image/png");
  private static final String CATCH_KEY_PREFIX = "catches/";
  private static final int DEFAULT_PAGE_LIMIT = 20;
  private static final int MAX_PAGE_LIMIT = 50;

  private final DogCatchRepository catchRepository;
  private final LikeRepository likeRepository;
  private final CommentRepository commentRepository;
  private final CatchReportRepository reportRepository;
  private final UserRepository userRepository;
  private final BreedRepository breedRepository;
  private final StorageService storageService;
  private final ImageResizer imageResizer;
  private final RateLimitService rateLimitService;
  private final FeedRankingService feedRankingService;
  private final FeedCacheService feedCacheService;

  public CatchService(
      DogCatchRepository catchRepository,
      LikeRepository likeRepository,
      CommentRepository commentRepository,
      CatchReportRepository reportRepository,
      UserRepository userRepository,
      BreedRepository breedRepository,
      StorageService storageService,
      ImageResizer imageResizer,
      RateLimitService rateLimitService,
      FeedRankingService feedRankingService,
      FeedCacheService feedCacheService) {
    this.catchRepository = catchRepository;
    this.likeRepository = likeRepository;
    this.commentRepository = commentRepository;
    this.reportRepository = reportRepository;
    this.userRepository = userRepository;
    this.breedRepository = breedRepository;
    this.storageService = storageService;
    this.imageResizer = imageResizer;
    this.rateLimitService = rateLimitService;
    this.feedRankingService = feedRankingService;
    this.feedCacheService = feedCacheService;
  }

  /**
   * Tworzy nowe polowanie z uploadem zdjecia.
   *
   * <p>Kolejnosc operacji:
   * <ol>
   *   <li>Rate limiting (10 catchy/min per uzytkownik).
   *   <li>Walidacja MIME type (whitelist).
   *   <li>Resize do 800px (pelny) i 200px (miniatura).
   *   <li>Upload obu plikow do S3.
   *   <li>Zapis encji DogCatch.
   *   <li>Aktualizacja denormalizowanych licznikow User (totalCatches, uniqueBreeds).
   * </ol>
   *
   * <p>Upload do S3 jest poza transakcja JPA — zewnetrzne API nie uczestniczy w rollbacku.
   * W przypadku bledu po uploaddzie a przed zapisem DB, pliki pozostana w S3 (dopuszczalne w MVP).
   *
   * @param currentUser zalogowany uzytkownik
   * @param breedId ID rasy psa
   * @param file zdjecie psa
   * @param caption opcjonalny opis
   * @param latitude opcjonalne wspolrzedne GPS
   * @param longitude opcjonalne wspolrzedne GPS
   * @param locationName opcjonalna nazwa miejsca
   * @param isPublic czy catch jest publiczny
   * @return DTO nowego polowania
   */
  public CatchResponse createCatch(
      User currentUser,
      Integer breedId,
      MultipartFile file,
      String caption,
      Double latitude,
      Double longitude,
      String locationName,
      boolean isPublic) {

    rateLimitService.checkUploadRateLimit(currentUser.getId().toString());

    validateMimeType(file);

    Breed breed = breedRepository.findById(breedId)
        .filter(Breed::isActive)
        .orElseThrow(() -> new BreedNotFoundException(breedId));

    String photoKey = buildKey(currentUser.getId(), "full");
    String thumbKey = buildKey(currentUser.getId(), "thumb");

    String photoUrl = uploadResized(file, photoKey, false);
    String thumbUrl = uploadResized(file, thumbKey, true);

    DogCatch dogCatch = DogCatch.builder()
        .user(currentUser)
        .breed(breed)
        .photoUrl(photoUrl)
        .thumbnailUrl(thumbUrl)
        .caption(caption)
        .latitude(latitude)
        .longitude(longitude)
        .locationName(locationName)
        .isPublic(isPublic)
        .build();

    return saveAndUpdateCounters(currentUser, dogCatch, breed, isPublic);
  }

  /**
   * Zwraca pojedynczy catch po ID.
   *
   * @param catchId ID polowania
   * @param currentUser zalogowany uzytkownik (sprawdzanie czy polubil)
   * @return DTO polowania
   * @throws CatchNotFoundException gdy catch nie istnieje lub jest soft-deleted
   */
  @Transactional(readOnly = true)
  public CatchResponse getCatch(UUID catchId, User currentUser) {
    DogCatch dogCatch = findActiveCatch(catchId);
    boolean liked = likeRepository.existsByUserIdAndDogCatchId(currentUser.getId(), catchId);
    return CatchResponse.from(dogCatch, liked);
  }

  /**
   * Soft-deletes catch i usuwa plik z S3.
   *
   * <p>Tylko wlasciciel moze usunac swoje polowanie.
   *
   * @param catchId ID polowania
   * @param currentUser zalogowany uzytkownik
   * @throws CatchNotFoundException gdy catch nie istnieje
   * @throws CatchAccessDeniedException gdy uzytkownik nie jest wlascicielem
   */
  @Transactional
  public void deleteCatch(UUID catchId, User currentUser) {
    DogCatch dogCatch = findActiveCatch(catchId);
    if (!dogCatch.getUser().getId().equals(currentUser.getId())) {
      throw new CatchAccessDeniedException(catchId);
    }

    dogCatch.softDelete();
    catchRepository.save(dogCatch);

    feedCacheService.removeFromFeeds(catchId);

    User owner = findActiveUserById(currentUser.getId());
    owner.decrementTotalCatches();
    userRepository.save(owner);

    cleanupStorageKeys(dogCatch);

    log.debug("Catch soft-deleted: catchId={}, userId={}", catchId, currentUser.getId());
  }

  /**
   * Dodaje polubienie do catcha.
   *
   * <p>Podwojne polubienie zwraca ConflictException (HTTP 409). Po dodaniu polubienia
   * przeliczamy feedScore i aktualizujemy Redis cache (feed:public) — bez czekania na job.
   *
   * @param catchId ID polowania
   * @param currentUser zalogowany uzytkownik
   * @return zaktualizowany DTO polowania
   */
  @Transactional
  public CatchResponse likeCatch(UUID catchId, User currentUser) {
    DogCatch dogCatch = findActiveCatch(catchId);

    if (likeRepository.existsByUserIdAndDogCatchId(currentUser.getId(), catchId)) {
      throw new ConflictException("Catch is already liked.");
    }

    likeRepository.save(Like.of(currentUser, dogCatch));
    dogCatch.incrementLikeCount();
    updateFeedScoreAndCache(dogCatch);
    catchRepository.save(dogCatch);

    log.debug("Like added: catchId={}, userId={}", catchId, currentUser.getId());
    return CatchResponse.from(dogCatch, true);
  }

  /**
   * Usuwa polubienie z catcha.
   *
   * @param catchId ID polowania
   * @param currentUser zalogowany uzytkownik
   * @throws CatchNotFoundException gdy catch nie istnieje
   */
  @Transactional
  public void unlikeCatch(UUID catchId, User currentUser) {
    DogCatch dogCatch = findActiveCatch(catchId);

    Optional<Like> like = likeRepository.findByUserIdAndDogCatchId(currentUser.getId(), catchId);
    if (like.isPresent()) {
      likeRepository.delete(like.get());
      dogCatch.decrementLikeCount();
      updateFeedScoreAndCache(dogCatch);
      catchRepository.save(dogCatch);
      log.debug("Like removed: catchId={}, userId={}", catchId, currentUser.getId());
    }
  }

  /**
   * Zwraca paginowana liste komentarzy dla polowania.
   *
   * @param catchId ID polowania
   * @param cursor ISO-8601 timestamp ostatniego elementu poprzedniej strony (null = pierwsza strona)
   * @param limit max liczba wynikow (domyslnie {@value #DEFAULT_PAGE_LIMIT})
   * @return lista komenatrzy z metadanymi paginacji
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<CommentResponse>> getComments(
      UUID catchId, String cursor, Integer limit) {
    findActiveCatch(catchId);

    int pageLimit = resolveLimit(limit);
    int fetchLimit = pageLimit + 1;

    List<Comment> comments;
    if (cursor == null || cursor.isBlank()) {
      comments = commentRepository.findFirstPage(catchId, fetchLimit);
    } else {
      Instant cursorInstant = Instant.parse(cursor);
      comments = commentRepository.findNextPage(catchId, cursorInstant, fetchLimit);
    }

    boolean hasMore = comments.size() > pageLimit;
    List<Comment> pageItems = hasMore ? comments.subList(0, pageLimit) : comments;

    String nextCursor = null;
    if (hasMore) {
      nextCursor = pageItems.get(pageItems.size() - 1).getCreatedAt().toString();
    }

    List<CommentResponse> response = pageItems.stream().map(CommentResponse::from).toList();
    return ApiResponse.ok(response, new ApiResponse.PaginationMeta(nextCursor, hasMore));
  }

  /**
   * Dodaje komentarz do polowania.
   *
   * @param catchId ID polowania
   * @param currentUser zalogowany uzytkownik
   * @param content tresc komentarza
   * @return DTO nowego komentarza
   * @throws CatchNotFoundException gdy catch nie istnieje
   */
  @Transactional
  public CommentResponse addComment(UUID catchId, User currentUser, String content) {
    DogCatch dogCatch = findActiveCatch(catchId);

    Comment comment = Comment.of(currentUser, dogCatch, content);
    commentRepository.save(comment);

    dogCatch.incrementCommentCount();
    updateFeedScoreAndCache(dogCatch);
    catchRepository.save(dogCatch);

    log.debug("Comment added: catchId={}, userId={}", catchId, currentUser.getId());
    return CommentResponse.from(comment);
  }

  /**
   * Soft-deletes komentarz.
   *
   * <p>Tylko autor komentarza moze go usunac.
   *
   * @param commentId ID komentarza
   * @param currentUser zalogowany uzytkownik
   * @throws CatchNotFoundException gdy komentarz nie istnieje
   * @throws CatchAccessDeniedException gdy uzytkownik nie jest autorem
   */
  @Transactional
  public void deleteComment(UUID commentId, User currentUser) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CatchNotFoundException(commentId));

    if (!comment.getUser().getId().equals(currentUser.getId())) {
      throw new CatchAccessDeniedException(commentId);
    }

    comment.softDelete();
    commentRepository.save(comment);

    DogCatch dogCatch = comment.getDogCatch();
    dogCatch.decrementCommentCount();
    updateFeedScoreAndCache(dogCatch);
    catchRepository.save(dogCatch);

    log.debug("Comment deleted: commentId={}, userId={}", commentId, currentUser.getId());
  }

  /**
   * Zglasza catch jako nieodpowiedni.
   *
   * @param catchId ID polowania
   * @param currentUser zglaszajacy uzytkownik
   * @param reason krotki powod zgloszenia
   * @param description opcjonalny opis
   * @throws CatchNotFoundException gdy catch nie istnieje
   */
  @Transactional
  public void reportCatch(UUID catchId, User currentUser, String reason, String description) {
    DogCatch dogCatch = findActiveCatch(catchId);
    reportRepository.save(CatchReport.of(currentUser, dogCatch, reason, description));
    log.debug("Catch reported: catchId={}, reporterId={}", catchId, currentUser.getId());
  }

  /**
   * Zwraca paginowana liste catchy uzytkownika.
   *
   * <p>Dla wlasnego profilu zwracamy wszystkie catche (publiczne i prywatne). Dla cudzego profilu
   * zwracamy tylko publiczne. Prywatne profile sa obsługiwane przez UserService (Krok 4) — tutaj
   * zakladamy ze wywolujacy juz zweryfikowal dostep do profilu.
   *
   * @param targetUserId ID uzytkownika ktorego catche pobieramy
   * @param currentUser zalogowany uzytkownik (do sprawdzania likes i wlasnosci)
   * @param cursor ISO-8601 timestamp (null = pierwsza strona)
   * @param limit max liczba wynikow
   * @return paginated catche z metadanymi
   */
  @Transactional(readOnly = true)
  public ApiResponse<List<CatchResponse>> getUserCatches(
      UUID targetUserId, User currentUser, String cursor, Integer limit) {

    boolean isOwner = targetUserId.equals(currentUser.getId());
    int pageLimit = resolveLimit(limit);
    int fetchLimit = pageLimit + 1;

    List<DogCatch> catches;
    if (cursor == null || cursor.isBlank()) {
      catches = catchRepository.findFirstPageByUserId(targetUserId, fetchLimit);
    } else {
      Instant cursorInstant = Instant.parse(cursor);
      catches = catchRepository.findNextPageByUserId(targetUserId, cursorInstant, fetchLimit);
    }

    boolean hasMore = catches.size() > pageLimit;
    List<DogCatch> pageItems = hasMore ? catches.subList(0, pageLimit) : catches;

    // Filtrowanie prywatnych catchy dla cudzych profili
    List<DogCatch> visibleItems = isOwner
        ? pageItems
        : pageItems.stream().filter(DogCatch::isPublic).toList();

    String nextCursor = null;
    if (hasMore && !pageItems.isEmpty()) {
      nextCursor = pageItems.get(pageItems.size() - 1).getCaughtAt().toString();
    }

    Set<UUID> likedIds = visibleItems.isEmpty()
        ? Set.of()
        : likeRepository.findLikedCatchIdsByUserIdIn(
            currentUser.getId(),
            visibleItems.stream().map(DogCatch::getId).toList());

    List<CatchResponse> response = visibleItems.stream()
        .map(dc -> CatchResponse.from(dc, likedIds.contains(dc.getId())))
        .toList();

    return ApiResponse.ok(response, new ApiResponse.PaginationMeta(nextCursor, hasMore));
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private DogCatch findActiveCatch(UUID catchId) {
    return catchRepository.findById(catchId)
        .filter(dc -> dc.getDeletedAt() == null)
        .orElseThrow(() -> new CatchNotFoundException(catchId));
  }

  private User findActiveUserById(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(u -> u.getDeletedAt() == null)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
  }

  private void validateMimeType(MultipartFile file) {
    String mimeType = file.getContentType();
    if (mimeType == null || !ALLOWED_PHOTO_MIME_TYPES.contains(mimeType)) {
      throw new UnsupportedFileFormatException(
          "Unsupported image format. Allowed: JPEG, PNG.");
    }
  }

  private String buildKey(UUID userId, String suffix) {
    return CATCH_KEY_PREFIX + userId + "/" + UUID.randomUUID() + "_" + suffix + ".jpg";
  }

  private String uploadResized(MultipartFile file, String key, boolean thumbnail) {
    try {
      ImageResizer.ProcessedImage processed = thumbnail
          ? imageResizer.resizeThumbnail(file.getInputStream())
          : imageResizer.resizeFull(file.getInputStream());

      return storageService.upload(
          key, processed.inputStream(), processed.contentType(), processed.contentLength());
    } catch (IOException e) {
      throw new ImageProcessingException("Failed to read uploaded file", e);
    }
  }

  /**
   * Przelicza feed score na podstawie aktualnych licznikow i aktualizuje encje + Redis.
   *
   * <p>Wywoływana po kazdej operacji która zmienia score: like, unlike, dodanie/usuniecie komentarza.
   * Wymaga zaladowanego {@code breed} (eager lub w ramach aktywnej sesji Hibernate).
   */
  private void updateFeedScoreAndCache(DogCatch dogCatch) {
    double newScore = feedRankingService.calculate(dogCatch);
    dogCatch.updateFeedScore(newScore);
    if (dogCatch.isPublic()) {
      feedCacheService.addToPublicFeed(dogCatch.getId(), newScore);
    }
  }

  private CatchResponse saveAndUpdateCounters(
      User currentUser, DogCatch dogCatch, Breed breed, boolean isPublic) {
    // Wylicz i zapisz poczatkowy feed score przed pierwszym save — Redis i DB dostaja ten sam score
    double initialScore = feedRankingService.calculate(
        dogCatch.getLikeCount(),
        dogCatch.getCommentCount(),
        breed.getRarityScore(),
        dogCatch.getCaughtAt() != null ? dogCatch.getCaughtAt() : java.time.Instant.now());
    dogCatch.updateFeedScore(initialScore);

    DogCatch saved = catchRepository.save(dogCatch);

    // Dodaj do Redis public feed natychmiast po zapisie — bez czekania na FeedRebuildJob
    if (isPublic) {
      feedCacheService.addToPublicFeed(saved.getId(), initialScore);
    }

    User owner = findActiveUserById(currentUser.getId());
    owner.incrementTotalCatches();

    if (isFirstCatchOfBreed(owner.getId(), breed.getId())) {
      owner.incrementUniqueBreeds();
    }

    userRepository.save(owner);

    log.debug("Catch created: catchId={}, userId={}, breedId={}, feedScore={}",
        saved.getId(), currentUser.getId(), breed.getId(), initialScore);

    return CatchResponse.from(saved, false);
  }

  /**
   * Sprawdza czy nowo zapisany catch jest pierwszym polowaniem danej rasy dla uzytkownika.
   *
   * <p>Zapytanie wykonuje sie po zapisie catcha — count == 1 oznacza wlasnie dodany rekord.
   */
  private boolean isFirstCatchOfBreed(UUID userId, int breedId) {
    return catchRepository.findBreedCatchCountsByUserId(userId).stream()
        .filter(row -> ((Number) row[0]).intValue() == breedId)
        .findFirst()
        .map(row -> ((Number) row[1]).longValue() == 1)
        .orElse(true);
  }

  private void cleanupStorageKeys(DogCatch dogCatch) {
    String photoUrl = dogCatch.getPhotoUrl();
    String thumbUrl = dogCatch.getThumbnailUrl();

    tryDelete(extractKeyFromUrl(photoUrl));
    tryDelete(extractKeyFromUrl(thumbUrl));
  }

  private void tryDelete(String key) {
    if (key == null || key.isBlank()) {
      return;
    }
    try {
      storageService.delete(key);
    } catch (Exception e) {
      log.warn("Failed to delete storage key={}: {}", key, e.getMessage());
    }
  }

  /**
   * Wyciaga klucz S3 z publicznego URL (wszystko po /catches/).
   *
   * <p>Format URL: {@code {endpoint}/{bucket}/catches/{userId}/{uuid}_full.jpg}
   */
  private String extractKeyFromUrl(String url) {
    if (url == null) {
      return null;
    }
    int idx = url.indexOf(CATCH_KEY_PREFIX);
    return idx >= 0 ? url.substring(idx) : null;
  }

  private int resolveLimit(Integer requested) {
    if (requested == null || requested <= 0) {
      return DEFAULT_PAGE_LIMIT;
    }
    return Math.min(requested, MAX_PAGE_LIMIT);
  }
}
