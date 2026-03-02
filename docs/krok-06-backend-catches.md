# Krok 6 — Backend: Catches + Upload zdjęć

**Cel:** Złapanie psa z uploadem zdjęcia na S3, polubienia, komentarze, zgłoszenia, paginacja profilu.

**Wynik:** `Tests run: 17, Failures: 0, Errors: 0` (CatchIntegrationTest) | `Tests run: 80, Failures: 0, Errors: 0` (mvn verify) | `BUILD SUCCESS`

---

## Nowe pliki

### Encje JPA

| Plik | Tabela | Opis |
|------|--------|------|
| `catch_/Like.java` | `"like"` | Polubienie catcha. Unikalny constraint `(user_id, catch_id)` w schemacie V1. Fabryka statyczna `Like.of(user, dogCatch)`. |
| `catch_/Comment.java` | `comment` | Komentarz pod catchem. Soft delete przez `deletedAt`. Fabryka `Comment.of(user, dogCatch, content)`. |
| `catch_/CatchReport.java` | `report` | Zgłoszenie catcha jako nieodpowiedniego. Status domyślny `pending`. Fabryka `CatchReport.of(reporter, dogCatch, reason, description)`. |

### Repozytoria

| Plik | Metody |
|------|--------|
| `catch_/LikeRepository.java` | `findByUserIdAndDogCatchId`, `existsByUserIdAndDogCatchId` |
| `catch_/CommentRepository.java` | `findFirstPage` (bez kursora), `findNextPage` (z kursorem `createdAt`) |
| `catch_/CatchReportRepository.java` | zapis-only (extends JpaRepository) |

### Serwis przetwarzania obrazów

**`catch_/ImageResizer.java`**

- Resize do `800px` szerokości — pełna wersja (`resizeFull`).
- Resize do `200px` szerokości — miniatura (`resizeThumbnail`).
- Konwersja do JPEG (quality 0.85) + automatyczny stripping metadanych EXIF przez Thumbnailator.
- Wynik: record `ProcessedImage(InputStream, contentLength, contentType)` — gotowy do `StorageService.upload()`.
- Wewnętrznie operuje na `ByteArrayOutputStream` (max 10MB — bezpieczne dla MVP).

### Wyjątki

| Plik | HTTP | Kiedy |
|------|------|-------|
| `catch_/CatchNotFoundException.java` | 404 | Catch nie istnieje lub jest soft-deleted |
| `catch_/CatchAccessDeniedException.java` | 403 | Próba modyfikacji cudzego catcha / komentarza |
| `catch_/ImageProcessingException.java` | 400 | Uszkodzony lub nieobsługiwany plik obrazu |

### DTO

| Plik | Kierunek | Opis |
|------|----------|------|
| `catch_/CatchResponse.java` | wyjście | Pełny widok catcha. Pole `liked` informuje czy zalogowany user polubił. Fabryka `CatchResponse.from(entity, liked)`. |
| `catch_/CommentResponse.java` | wyjście | Widok komentarza. Treść soft-deleted komentarzy jest `null`. |
| `catch_/UserSummary.java` | wyjście | `(id, username, avatarUrl)` — osadzany w CatchResponse i CommentResponse zamiast pełnej encji User. |
| `catch_/BreedSummary.java` | wyjście | `(id, name, namePl, rarityScore)` — osadzany w CatchResponse. |
| `catch_/CreateCommentRequest.java` | wejście | `content` (1–500 znaków, @NotBlank). |
| `catch_/CreateReportRequest.java` | wejście | `reason` (max 50), `description` (opcjonalny, max 500). |

### Logika biznesowa

**`catch_/CatchService.java`**

Kolejność operacji przy `createCatch`:
1. Rate limiting (10 catchy/min per user — `RateLimitService.checkUploadRateLimit`).
2. Walidacja MIME type (whitelist: `image/jpeg`, `image/png`).
3. Wyszukanie aktywnej rasy (`BreedRepository`).
4. Resize + upload pełnego zdjęcia do S3 (klucz: `catches/{userId}/{uuid}_full.jpg`).
5. Resize + upload miniatury do S3 (klucz: `catches/{userId}/{uuid}_thumb.jpg`).
6. Zapis encji `DogCatch`.
7. Inkrementacja `totalCatches` na User.
8. Inkrementacja `uniqueBreeds` na User — tylko gdy to pierwsze złowienie tej rasy.

Pozostałe metody:

| Metoda | Transakcja | Opis |
|--------|-----------|------|
| `getCatch` | readOnly | Sprawdza `liked` przez `LikeRepository.existsByUserIdAndDogCatchId`. |
| `deleteCatch` | tak | Soft delete + dekrementacja `totalCatches` + `storageService.delete` (błąd S3 logowany jako WARN, nie rzuca wyjątku). |
| `likeCatch` | tak | Zapis `Like` + `incrementLikeCount`. Podwójne polubienie → `ConflictException` (409). |
| `unlikeCatch` | tak | Idempotentne — jeśli `Like` nie istnieje, metoda nie rzuca błędu. |
| `getComments` | readOnly | Cursor pagination po `createdAt`. Fetch `limit+1` rekordów do wykrycia `hasMore`. |
| `addComment` | tak | Zapis `Comment` + `incrementCommentCount`. |
| `deleteComment` | tak | Soft delete + `decrementCommentCount`. Tylko autor (403 dla innych). |
| `reportCatch` | tak | Zapis `CatchReport` ze statusem `pending`. |
| `getUserCatches` | readOnly | Cursor pagination po `caughtAt`. Właściciel widzi wszystkie, obcy filtruje `isPublic = true`. |

### Kontroler

**`catch_/CatchController.java`** — 10 endpointów:

| Method | Path | Auth | Opis |
|--------|------|------|------|
| `POST` | `/catches` | wymagana | Upload zdjęcia + tworzenie catcha (multipart/form-data) |
| `GET` | `/catches/{id}` | wymagana | Szczegóły catcha |
| `DELETE` | `/catches/{id}` | wymagana | Soft delete (tylko właściciel) |
| `POST` | `/catches/{id}/likes` | wymagana | Polubienie |
| `DELETE` | `/catches/{id}/likes` | wymagana | Usunięcie polubienia (idempotentne) |
| `GET` | `/catches/{id}/comments` | wymagana | Lista komentarzy (cursor pagination) |
| `POST` | `/catches/{id}/comments` | wymagana | Dodanie komentarza |
| `DELETE` | `/comments/{id}` | wymagana | Usunięcie komentarza (tylko autor) |
| `POST` | `/catches/{id}/reports` | wymagana | Zgłoszenie catcha |
| `GET` | `/users/{userId}/catches` | wymagana | Catche użytkownika (cursor pagination) |

---

## Zmodyfikowane pliki

### `catch_/DogCatch.java`

Dodano metody mutujące stan encji (wywoływane wewnątrz transakcji CatchService):
- `softDelete()` — ustawia `deletedAt = now()`.
- `incrementLikeCount()` / `decrementLikeCount()` — guard przed zejściem poniżej 0.
- `incrementCommentCount()` / `decrementCommentCount()` — jak wyżej.

### `catch_/DogCatchRepository.java`

Dodano cursor pagination dla profilu użytkownika:
- `findFirstPageByUserId(userId, limit)` — pierwsza strona (bez kursora).
- `findNextPageByUserId(userId, cursor, limit)` — kolejna strona (`caughtAt < :cursor`).

### `auth/User.java`

Dodano metody denormalizacji statystyk:
- `incrementTotalCatches()` / `decrementTotalCatches()` (guard > 0).
- `incrementUniqueBreeds()`.

### `auth/RateLimitService.java`

Dodano `checkUploadRateLimit(userId)` — 10 req/min per user (klucz: `upload:{userId}`).

### `common/GlobalExceptionHandler.java`

Dodano handlery:
- `CatchNotFoundException` → 404.
- `CatchAccessDeniedException` → 403.
- `ImageProcessingException` → 400 (loguje WARN).

---

## Testy integracyjne

**`catch_/CatchIntegrationTest.java`** — 17 testów, Testcontainers + PostgreSQL, `StorageService` mockowany.

| Test | Co sprawdza |
|------|-------------|
| `createCatch_withValidData_returnsCatchResponse` | Sukces — URL zdjęcia, miniatura, breed, user w odpowiedzi |
| `createCatch_incrementsTotalCatchesOnUser` | Denormalizacja `totalCatches` na User po zapisie |
| `createCatch_withUnsupportedMimeType_throwsUnsupportedFileFormatException` | GIF → 415 |
| `getCatch_withExistingCatch_returnsCatchResponse` | Odpytanie po ID |
| `getCatch_withNonExistentId_throwsCatchNotFoundException` | UUID nie istnieje → 404 |
| `deleteCatch_byCatchOwner_softDeletesRecord` | `deletedAt != null` po usunięciu |
| `deleteCatch_byOtherUser_throwsCatchAccessDeniedException` | Cudzy catch → 403 |
| `likeCatch_addsLikeAndIncrementsCount` | `likeCount = 1`, `liked = true` |
| `likeCatch_twice_throwsConflictException` | Podwójne polubienie → 409 |
| `unlikeCatch_removesLikeAndDecrementsCount` | `likeCount = 0`, `liked = false` po unlike |
| `addComment_savesCommentAndIncrementsCount` | Komentarz zapisany, `commentCount = 1` |
| `deleteComment_byAuthor_softDeletesAndDecrementsCount` | `deletedAt != null`, `commentCount = 0` |
| `deleteComment_byOtherUser_throwsCatchAccessDeniedException` | Cudzy komentarz → 403 |
| `getComments_firstPage_returnsCommentsOrderedByCreatedAtDesc` | 3 komentarze, `hasMore = false` |
| `reportCatch_savesReportRecord` | Zapis bez wyjątku |
| `getUserCatches_forOwner_returnsAllCatches` | Właściciel widzi publiczne + prywatne (2 catche) |
| `getUserCatches_forOtherUser_returnsOnlyPublicCatches` | Obcy widzi tylko publiczny (1 catch) |
