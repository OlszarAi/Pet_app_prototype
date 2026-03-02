# Krok 4 — Backend: Users + Settings + Avatar

## Co zostalo zrobione

Krok 4 dodaje pelne zarzadzanie profilem uzytkownika na bazie istniejacego systemu z Krokow 1-3:

1. **Profil** — pobieranie wlasnego profilu i profilu innych uzytkownikow, edycja username/bio, wyszukiwanie.
2. **Avatar** — upload (resize do 400px JPEG przez Thumbnailator, zapis na MinIO/S3), usuniecie.
3. **Zmiana hasla** — wymaga podania aktualnego hasla, po zmianie uniewa\u017cnia wszystkie sesje.
4. **Soft delete konta** — kasuje sesje i oznacza konto jako usuniete (dane zostaja w bazie).
5. **Ustawienia** — pobieranie i aktualizacja flag push, jezyka i trybu ciemnego.
6. **Warstwa storage** — abstrakcja `StorageService` z implementacja `S3StorageService`
   (AWS SDK v2, kompatybilna z MinIO w dev i AWS S3 w prod).

Po Kroku 4 liczba testow: **46 (25 z Kroku 3 + 17 UserIntegration + 4 UserSettingsIntegration)**.

---

## Struktura plikow po Kroku 4

Nowe pliki sa oznaczone `<- NOWY`, zmodyfikowane `<- ZAKTUALIZOWANY`.

```
backend/
└── src/
    ├── main/
    │   ├── java/com/petsapp/
    │   │   ├── storage/                                     <- NOWY pakiet
    │   │   │   ├── StorageProperties.java                   <- NOWY: @ConfigurationProperties("app.storage")
    │   │   │   ├── StorageService.java                      <- NOWY: interfejs upload/delete
    │   │   │   └── S3StorageService.java                    <- NOWY: AWS SDK v2 + MinIO support
    │   │   ├── user/                                        <- NOWY pakiet
    │   │   │   ├── UserController.java                      <- NOWY: 9 endpointow REST
    │   │   │   ├── UserService.java                         <- NOWY: logika biznesowa profilu
    │   │   │   ├── UserProfileResponse.java                 <- NOWY: DTO profilu (record)
    │   │   │   ├── UpdateProfileRequest.java                <- NOWY: DTO edycji profilu (record)
    │   │   │   ├── UpdatePasswordRequest.java               <- NOWY: DTO zmiany hasla (record)
    │   │   │   ├── UserSettingsResponse.java                <- NOWY: DTO ustawien (record)
    │   │   │   ├── UpdateUserSettingsRequest.java           <- NOWY: DTO aktualizacji ustawien (record)
    │   │   │   ├── UserNotFoundException.java               <- NOWY: 404
    │   │   │   ├── PasswordMismatchException.java           <- NOWY: 400
    │   │   │   ├── UnsupportedFileFormatException.java      <- NOWY: 415
    │   │   │   └── AvatarProcessingException.java           <- NOWY: 400 (uszkodzony plik)
    │   │   ├── auth/
    │   │   │   └── UserRepository.java                      <- ZAKTUALIZOWANY: searchActiveByUsernamePrefix
    │   │   ├── common/
    │   │   │   └── GlobalExceptionHandler.java              <- ZAKTUALIZOWANY: 4 nowe handlery
    │   │   └── config/
    │   │       └── SecurityConfig.java                      <- ZAKTUALIZOWANY: /users/search public
    │   └── resources/
    │       └── application-dev.yml                          (bez zmian — app.storage juz bylo)
    └── test/
        ├── java/com/petsapp/
        │   └── user/
        │       ├── UserIntegrationTest.java                 <- NOWY: 17 testow integracyjnych
        │       └── UserSettingsIntegrationTest.java         <- NOWY: 4 testy integracyjne
        └── resources/
            └── application-test.yml                         <- ZAKTUALIZOWANY: sekcja app.storage
```

---

## Endpointy

Wszystkie endpointy sa pod prefiksem `/api/v1` (konfiguracja serwera).

| Metoda     | Sciezka                | Auth      | Opis                                                   |
|------------|------------------------|-----------|--------------------------------------------------------|
| `GET`      | `/users/me`            | wymagany  | Pelny profil zalogowanego uzytkownika (z emailem)      |
| `PATCH`    | `/users/me`            | wymagany  | Edycja username i/lub bio                              |
| `POST`     | `/users/me/avatar`     | wymagany  | Upload avatara (JPEG/PNG, multipart, max 10 MB)        |
| `DELETE`   | `/users/me/avatar`     | wymagany  | Usuniecie avatara                                      |
| `PATCH`    | `/users/me/password`   | wymagany  | Zmiana hasla (wymaga aktualnego hasla)                 |
| `DELETE`   | `/users/me`            | wymagany  | Soft delete konta (usuwa sesje, zachowuje dane w DB)   |
| `GET`      | `/users/{userId}`      | wymagany  | Publiczny profil innego uzytkownika (bez emaila)       |
| `GET`      | `/users/search?q=`     | publiczny | Wyszukiwanie uzytkownikow po fragmencie username       |
| `GET`      | `/users/me/settings`   | wymagany  | Ustawienia powiadomien i interfejsu                    |
| `PATCH`    | `/users/me/settings`   | wymagany  | Aktualizacja ustawien                                  |

---

## Opis kazdego elementu

### StorageProperties

```java
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
  private String type;       // "minio" | "s3"
  private String endpoint;   // custom URL dla MinIO / LocalStack, null = AWS S3
  private String bucket;
  private String accessKey;
  private String secretKey;
  private String region = "us-east-1";
}
```

Wartosci wstrzykiwane z `application-dev.yml` przez zmienne srodowiskowe `${MINIO_ROOT_USER}` i
`${MINIO_ROOT_PASSWORD}`. W testach `StorageService` jest mockowany przez `@MockBean` — wartosci
testowe sa zastepczymi placeholderami nigdy nieu\u017cywanymi.

---

### StorageService + S3StorageService

```java
public interface StorageService {
  String upload(String key, InputStream inputStream, String contentType, long contentLength);
  void delete(String key);
}
```

`S3StorageService` tworzy `S3Client` z AWS SDK v2. Gdy `endpoint` jest ustawiony (dev/MinIO),
wlacza `pathStyleAccessEnabled(true)` — wymagane przez MinIO zamiast subdomain-style.
Publiczny URL dla MinIO: `{endpoint}/{bucket}/{key}`, dla AWS S3: `https://{bucket}.s3.{region}.amazonaws.com/{key}`.

---

### UserController

Wstrzykuje zalogowanego uzytkownika przez `@AuthenticationPrincipal User currentUser`.
`JwtFilter` (z Kroku 2) ustawia encje `User` bezposrednio jako principal w `SecurityContext` —
dzieki temu kontroler nie musi ladowac uzytkownika z bazy.

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
    @AuthenticationPrincipal User currentUser) {
  return ResponseEntity.ok(ApiResponse.ok(userService.getMyProfile(currentUser.getId())));
}

@PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<UserProfileResponse>> uploadAvatar(
    @AuthenticationPrincipal User currentUser,
    @RequestPart("file") MultipartFile file) { ... }
```

---

### UserService — upload avatara

1. Sprawdza MIME type (dozwolone: `image/jpeg`, `image/png`).
2. Usuwa stary plik z S3/MinIO (jezeli istnial).
3. Skaluje obraz do maks. 400x400 px z zachowaniem proporcji, konwertuje do JPEG (quality 0.85).
4. Uploaduje przetworzony obraz pod kluczem `avatars/{userId}.jpg`.
5. Aktualizuje `avatar_url` w bazie.

```java
Thumbnails.of(file.getInputStream())
    .size(AVATAR_DIMENSION, AVATAR_DIMENSION)   // 400
    .keepAspectRatio(true)
    .outputFormat("jpg")
    .outputQuality(0.85)
    .toOutputStream(baos);
```

Klucz pliku jest deterministyczny (`avatars/{userId}.jpg`) — kazdy upload nadpisuje poprzedni bez
zostawiania sierot w storage.

---

### UserService — zmiana hasla

```java
public void updatePassword(UUID userId, UpdatePasswordRequest request) {
  User user = findActiveUser(userId);
  if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
    throw new PasswordMismatchException("Current password is incorrect.");
  }
  user.updatePassword(passwordEncoder.encode(request.newPassword()));
  userRepository.save(user);
  // Uniewaznij wszystkie sesje — wymusza ponowne logowanie na wszystkich urzadzeniach
  refreshTokenRepository.deleteAllByUserId(userId);
}
```

---

### UserService — wyszukiwanie uzytkownikow

Native SQL query z `LIKE lower(concat(:prefix, '%'))` — case-insensitive, efficient dzieki indeksowi `idx_user_username` z V1. Wyniki ograniczone do 20.

```java
@Query(
    value = "SELECT * FROM \"user\" WHERE lower(username) LIKE lower(concat(:prefix, '%'))"
        + " AND deleted_at IS NULL ORDER BY username LIMIT :limit",
    nativeQuery = true)
List<User> searchActiveByUsernamePrefix(@Param("prefix") String prefix, @Param("limit") int limit);
```

---

### UserProfileResponse — dwa warianty

```java
public record UserProfileResponse(UUID id, String username, String bio, String avatarUrl,
    int totalCatches, int uniqueBreeds, boolean isPrivate, String email, Instant createdAt) {

  // Wlasny profil — zawiera email
  public static UserProfileResponse fromOwner(User user) { ... }

  // Profil publiczny — email jest null (pomijany w JSON przez @JsonInclude(NON_NULL))
  public static UserProfileResponse fromPublic(User user) { ... }
}
```

---

### GlobalExceptionHandler — nowe handlery

| Wyjatek                         | HTTP status                   | ErrorCode            |
|---------------------------------|-------------------------------|----------------------|
| `UserNotFoundException`         | 404 Not Found                 | `NOT_FOUND`          |
| `UnsupportedFileFormatException`| 415 Unsupported Media Type    | `UNSUPPORTED_FORMAT` |
| `AvatarProcessingException`     | 400 Bad Request               | `VALIDATION_ERROR`   |
| `PasswordMismatchException`     | 400 Bad Request               | `VALIDATION_ERROR`   |

---

## Testy

### UserIntegrationTest (17 testow)

| Metoda testowa                                                               | Co sprawdza                                        |
|------------------------------------------------------------------------------|----------------------------------------------------|
| `getMyProfile_returnsProfileWithEmail`                                       | Profil wlasny zawiera email                        |
| `getMyProfile_withDeletedUser_throwsUserNotFoundException`                   | Soft-deleted user zwraca 404                       |
| `updateProfile_withNewUsername_updatesSuccessfully`                          | Zmiana username dziala                             |
| `updateProfile_withBioOnly_updatesBio`                                       | Tylko bio — username nie zmienia sie               |
| `updateProfile_withDuplicateUsername_throwsConflictException`                | Duplikat username -> 409                           |
| `uploadAvatar_withValidJpeg_storesAndUpdatesAvatarUrl`                       | Upload JPEG wywoluje StorageService i ustawia URL  |
| `uploadAvatar_withUnsupportedMimeType_throwsUnsupportedFileFormatException`  | GIF -> 415, storage nie jest wywolyway             |
| `deleteAvatar_whenAvatarExists_deletesFromStorageAndClearsUrl`               | Usuniecie avatara czysci URL i wywoluje delete     |
| `deleteAvatar_whenNoAvatar_doesNotCallStorage`                               | Brak avatara -> storage nie jest dotykany          |
| `updatePassword_withCorrectCurrentPassword_changesPasswordAndInvalidatesSessions` | Zmiana hasla + usuniecie sesji              |
| `updatePassword_withWrongCurrentPassword_throwsPasswordMismatchException`    | Bledne haslo -> 400                                |
| `deleteAccount_softDeletesUserAndInvalidatesTokens`                         | Soft delete zachowuje rekord, ustawia deleted_at   |
| `getUserProfile_returnsPublicProfileWithoutEmail`                            | Obcy profil nie zawiera emaila                     |
| `getUserProfile_withNonExistentId_throwsUserNotFoundException`               | Nieistniej\u0105cy UUID -> 404                             |
| `searchUsers_withMatchingPrefix_returnsResults`                              | Prefix search zwraca pasuj\u0105cych uzytkownikow          |
| `searchUsers_withNonMatchingQuery_returnsEmptyList`                          | Brak wynikow -> pusta lista                        |
| `searchUsers_withBlankQuery_returnsEmptyList`                                | Pusty/null query -> pusta lista bez bledu          |

### UserSettingsIntegrationTest (4 testy)

| Metoda testowa                                          | Co sprawdza                               |
|---------------------------------------------------------|-------------------------------------------|
| `getMySettings_returnsDefaultSettings`                  | Domyslne flagi (true/true/true/true/pl/false) |
| `updateSettings_withAllFieldsChanged_updatesSuccessfully` | Wszystkie pola sa zapisywane            |
| `updateSettings_withPartialChange_persistsCorrectly`    | Kolejne zapisy nie nadpisuja niepotrzebnie|
| `updateSettings_persistsAcrossRequests`                 | Zapis jest trwaly miedzy wywolaniami      |

---

## Weryfikacja

```bash
# Pelna weryfikacja
cd backend
mvn clean verify

# Tylko testy Kroku 4
mvn test -Dtest="UserIntegrationTest,UserSettingsIntegrationTest"

# Wynik oczekiwany:
# Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

---

## Decyzje architektoniczne

**Klucz avatara deterministyczny (`avatars/{userId}.jpg`)** — zamiast UUID per upload. Upraszcza
logike (nie trzeba przechowywac klucza osobno), a S3/MinIO nadpisuje obiekt atomowo.
Wada: brak wersjonowania historii avatara — akceptowalne w MVP.

**Soft delete konta bez anonimizacji danych** — `deleted_at` jest ustawiany, ale dane (email, username)
pozostaja w bazie. Pozwala na przywrocenie konta i spe\u0142nia GDPR jezeli endpoint GDPR-delete bedzie
zaimplementowany pózniej (czytelnie zaplanowane w backlogu).

**`StorageService` jako interfejs** — umo\u017cliwia podmiane implementacji bez zmian w `UserService`.
W testach `@MockBean StorageService` eliminuje potrzebe uruchamiania MinIO w Testcontainers.

**Brak `@Transactional` na `uploadAvatar`** — operacja S3 nie uczestniczy w transakcji JPA. Awaria
miedzy deletem starym plikiem a uploadem nowego moze zostawic brak avatara — akceptowalne w MVP,
gdyz nastepny upload naprawi sytuacje bez utraty danych uzytkownika.
