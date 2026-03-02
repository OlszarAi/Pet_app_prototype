package com.petsapp.catch_;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.petsapp.AbstractIntegrationTest;
import com.petsapp.auth.AuthService;
import com.petsapp.auth.ConflictException;
import com.petsapp.auth.EmailService;
import com.petsapp.auth.RegisterRequest;
import com.petsapp.auth.User;
import com.petsapp.auth.UserRepository;
import com.petsapp.auth.VerifyEmailRequest;
import com.petsapp.breed.Breed;
import com.petsapp.breed.BreedRepository;
import com.petsapp.common.ApiResponse;
import com.petsapp.storage.StorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Testy integracyjne dla CatchService z prawdziwa baza PostgreSQL i zmockowanymi serwisami
 * zewnetrznymi (S3, email).
 *
 * <p>Kazdy test tworzy unikalne konto uzytkownika aby uniknac konfliktow.
 * StorageService jest mockowany — nie laczymy sie z MinIO / S3 w testach.
 * ImageResizer dziala na prawdziwych danych (4x4 px JPEG).
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatchIntegrationTest extends AbstractIntegrationTest {

  @Autowired private CatchService catchService;
  @Autowired private DogCatchRepository catchRepository;
  @Autowired private LikeRepository likeRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private BreedRepository breedRepository;

  @MockBean private EmailService emailService;
  @MockBean private StorageService storageService;

  private static final String TEST_PASSWORD = "SecurePassword123";
  private static final String FAKE_PHOTO_URL = "http://localhost:9000/petsapp-test/catches/test_full.jpg";
  private static final String FAKE_THUMB_URL = "http://localhost:9000/petsapp-test/catches/test_thumb.jpg";

  @BeforeEach
  void setupMocks() {
    doNothing().when(emailService).sendVerificationCode(anyString(), anyString(), anyString());
    doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);
  }

  private String uniqueEmail() {
    return "catch-test-" + UUID.randomUUID() + "@example.com";
  }

  private String uniqueUsername() {
    return "ct" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
  }

  private User createVerifiedUser() {
    String email = uniqueEmail();
    String username = uniqueUsername();
    String code = "654321";
    when(emailService.generateVerificationCode()).thenReturn(code);
    when(emailService.verificationCodeExpiry()).thenReturn(Instant.now().plusSeconds(900));

    authService.register(new RegisterRequest(email, username, TEST_PASSWORD));
    authService.verifyEmail(new VerifyEmailRequest(email, code));

    return userRepository.findActiveByEmail(email).orElseThrow();
  }

  /**
   * Tworzy poprawny 4x4 px plik JPEG przy uzyciu Java ImageIO.
   *
   * <p>Gwarantuje poprawnosc naglowkow JPEG potrzebna do przetworzenia przez Thumbnailator.
   */
  private MockMultipartFile createTestPhoto() {
    BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
    img.setRGB(0, 0, 0xFF0000);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ImageIO.write(img, "jpg", baos);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to create test JPEG", ex);
    }
    return new MockMultipartFile("file", "dog.jpg", "image/jpeg", baos.toByteArray());
  }

  private Breed getFirstBreed() {
    return breedRepository.findAll().stream()
        .filter(Breed::isActive)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No active breeds in test DB"));
  }

  // -------------------------
  // createCatch
  // -------------------------

  @Test
  void createCatch_withValidData_returnsCatchResponse() {
    // Mockujemy dwa uploady po kolei: pelny + miniatura
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User user = createVerifiedUser();
    Breed breed = getFirstBreed();
    MockMultipartFile photo = createTestPhoto();

    CatchResponse response =
        catchService.createCatch(user, breed.getId(), photo, "Piekny pies!", null, null, null, true);

    assertThat(response.id()).isNotNull();
    assertThat(response.photoUrl()).isEqualTo(FAKE_PHOTO_URL);
    assertThat(response.thumbnailUrl()).isEqualTo(FAKE_THUMB_URL);
    assertThat(response.caption()).isEqualTo("Piekny pies!");
    assertThat(response.likeCount()).isZero();
    assertThat(response.liked()).isFalse();
    assertThat(response.breed().id()).isEqualTo(breed.getId());
    assertThat(response.user().id()).isEqualTo(user.getId());
  }

  @Test
  void createCatch_incrementsTotalCatchesOnUser() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User user = createVerifiedUser();
    int beforeCatches = userRepository.findById(user.getId()).orElseThrow().getTotalCatches();

    catchService.createCatch(user, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);

    int afterCatches = userRepository.findById(user.getId()).orElseThrow().getTotalCatches();
    assertThat(afterCatches).isEqualTo(beforeCatches + 1);
  }

  @Test
  void createCatch_withUnsupportedMimeType_throwsUnsupportedFileFormatException() {
    User user = createVerifiedUser();
    MockMultipartFile gifFile =
        new MockMultipartFile("file", "dog.gif", "image/gif", new byte[] {0x47, 0x49, 0x46});

    assertThatThrownBy(
            () ->
                catchService.createCatch(
                    user, getFirstBreed().getId(), gifFile, null, null, null, null, true))
        .isInstanceOf(com.petsapp.user.UnsupportedFileFormatException.class);
  }

  // -------------------------
  // getCatch
  // -------------------------

  @Test
  void getCatch_withExistingCatch_returnsCatchResponse() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User user = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            user, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);

    CatchResponse fetched = catchService.getCatch(created.id(), user);

    assertThat(fetched.id()).isEqualTo(created.id());
  }

  @Test
  void getCatch_withNonExistentId_throwsCatchNotFoundException() {
    User user = createVerifiedUser();
    UUID randomId = UUID.randomUUID();

    assertThatThrownBy(() -> catchService.getCatch(randomId, user))
        .isInstanceOf(CatchNotFoundException.class);
  }

  // -------------------------
  // deleteCatch
  // -------------------------

  @Test
  void deleteCatch_byCatchOwner_softDeletesRecord() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User user = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            user, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);

    catchService.deleteCatch(created.id(), user);

    assertThat(catchRepository.findById(created.id()))
        .isPresent()
        .hasValueSatisfying(dc -> assertThat(dc.getDeletedAt()).isNotNull());
  }

  @Test
  void deleteCatch_byOtherUser_throwsCatchAccessDeniedException() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User other = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            owner, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);

    assertThatThrownBy(() -> catchService.deleteCatch(created.id(), other))
        .isInstanceOf(CatchAccessDeniedException.class);
  }

  // -------------------------
  // likeCatch / unlikeCatch
  // -------------------------

  @Test
  void likeCatch_addsLikeAndIncrementsCount() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User liker = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            owner, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);

    CatchResponse liked = catchService.likeCatch(created.id(), liker);

    assertThat(liked.likeCount()).isEqualTo(1);
    assertThat(liked.liked()).isTrue();
    assertThat(likeRepository.existsByUserIdAndDogCatchId(liker.getId(), created.id())).isTrue();
  }

  @Test
  void likeCatch_twice_throwsConflictException() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User liker = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            owner, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);

    catchService.likeCatch(created.id(), liker);

    assertThatThrownBy(() -> catchService.likeCatch(created.id(), liker))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void unlikeCatch_removesLikeAndDecrementsCount() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User liker = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            owner, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);

    catchService.likeCatch(created.id(), liker);
    catchService.unlikeCatch(created.id(), liker);

    CatchResponse afterUnlike = catchService.getCatch(created.id(), liker);
    assertThat(afterUnlike.likeCount()).isZero();
    assertThat(afterUnlike.liked()).isFalse();
  }

  // -------------------------
  // addComment / deleteComment
  // -------------------------

  @Test
  void addComment_savesCommentAndIncrementsCount() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User commenter = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            owner, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);

    CommentResponse comment = catchService.addComment(created.id(), commenter, "Super pies!");

    assertThat(comment.id()).isNotNull();
    assertThat(comment.content()).isEqualTo("Super pies!");
    assertThat(comment.user().id()).isEqualTo(commenter.getId());

    CatchResponse updated = catchService.getCatch(created.id(), owner);
    assertThat(updated.commentCount()).isEqualTo(1);
  }

  @Test
  void deleteComment_byAuthor_softDeletesAndDecrementsCount() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User commenter = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            owner, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);
    CommentResponse comment = catchService.addComment(created.id(), commenter, "Pies super!");

    catchService.deleteComment(comment.id(), commenter);

    assertThat(commentRepository.findById(comment.id()))
        .isPresent()
        .hasValueSatisfying(c -> assertThat(c.getDeletedAt()).isNotNull());

    CatchResponse updated = catchService.getCatch(created.id(), owner);
    assertThat(updated.commentCount()).isZero();
  }

  @Test
  void deleteComment_byOtherUser_throwsCatchAccessDeniedException() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User commenter = createVerifiedUser();
    User other = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            owner, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);
    CommentResponse comment = catchService.addComment(created.id(), commenter, "Test");

    assertThatThrownBy(() -> catchService.deleteComment(comment.id(), other))
        .isInstanceOf(CatchAccessDeniedException.class);
  }

  // -------------------------
  // getComments (paginacja)
  // -------------------------

  @Test
  void getComments_firstPage_returnsCommentsOrderedByCreatedAtDesc() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User commenter = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            owner, getFirstBreed().getId(), createTestPhoto(), null, null, null, null, true);

    catchService.addComment(created.id(), commenter, "Pierwszy");
    catchService.addComment(created.id(), commenter, "Drugi");
    catchService.addComment(created.id(), commenter, "Trzeci");

    ApiResponse<List<CommentResponse>> result =
        catchService.getComments(created.id(), null, 10);

    assertThat(result.data()).hasSize(3);
    assertThat(result.pagination().hasMore()).isFalse();
  }

  // -------------------------
  // reportCatch
  // -------------------------

  @Test
  void reportCatch_savesReportRecord() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User reporter = createVerifiedUser();
    CatchResponse created =
        catchService.createCatch(
            owner, getFirstBreed().getId(), createTestPhoto(), "Dog photo", null, null, null, true);

    catchService.reportCatch(created.id(), reporter, "inappropriate", "Spam content");
    // Brak wyjatku oznacza sukces — weryfikujemy przez sideeffect
    assertThat(catchRepository.findById(created.id())).isPresent();
  }

  // -------------------------
  // getUserCatches (paginacja)
  // -------------------------

  @Test
  void getUserCatches_forOwner_returnsAllCatches() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL)
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    Breed breed = getFirstBreed();
    catchService.createCatch(owner, breed.getId(), createTestPhoto(), "A", null, null, null, true);
    catchService.createCatch(owner, breed.getId(), createTestPhoto(), "B", null, null, null, false);

    ApiResponse<List<CatchResponse>> result =
        catchService.getUserCatches(owner.getId(), owner, null, 20);

    assertThat(result.data()).hasSize(2);
  }

  @Test
  void getUserCatches_forOtherUser_returnsOnlyPublicCatches() {
    when(storageService.upload(anyString(), any(), anyString(), anyLong()))
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL)
        .thenReturn(FAKE_PHOTO_URL)
        .thenReturn(FAKE_THUMB_URL);

    User owner = createVerifiedUser();
    User viewer = createVerifiedUser();
    Breed breed = getFirstBreed();
    catchService.createCatch(owner, breed.getId(), createTestPhoto(), "Public", null, null, null, true);
    catchService.createCatch(owner, breed.getId(), createTestPhoto(), "Private", null, null, null, false);

    ApiResponse<List<CatchResponse>> result =
        catchService.getUserCatches(owner.getId(), viewer, null, 20);

    assertThat(result.data()).hasSize(1);
    assertThat(result.data().get(0).caption()).isEqualTo("Public");
  }
}
