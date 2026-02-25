# PetsApp — Plan Implementacji Krok po Kroku

Zasada: **jeden krok = jeden PR = działający, przetestowany kawałek systemu.**
Nie przechodzimy do kolejnego kroku zanim poprzedni nie przejdzie testów i code review.

---

## Krok 0 — Zasady AI i środowisko pracy

Przed napisaniem pierwszej linii kodu.

### Pliki do stworzenia
- [NEW] [AI_RULES.md](file:///home/adam/coding/Pet_app_prototype/AI_RULES.md) ✅ — zasady dla AI, już gotowy
- [NEW] `.gitignore` — Java/Node/env
- [NEW] `docker-compose.yml` — PostgreSQL 16, Redis, MailHog

### Weryfikacja
```bash
# Repozytorium Git z branchami
git checkout -b develop

# Sprawdź docker-compose
docker compose up -d
docker compose ps   # wszystkie serwisy "running"
docker compose down
```

---

## Krok 1 — Backend: Infra (Spring Boot + Flyway + Docker)

**Cel:** Pusta aplikacja Spring Boot która startuje, łączy się z bazą przez Flyway i zwraca health check.

### Zakres
- Inicjalizacja projektu Maven (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `flyway-core`, `postgresql`, `spring-boot-starter-actuator`)
- `application.yml` + `application-dev.yml` (tylko z localhost values)
- `docker-compose.yml` z PostgreSQL 16 + Redis + MailHog (dev only)
- Migracja `V1__init_schema.sql` — pełny schemat z README (wszystkie tabele + indeksy)
- Migracja `V2__seed_breeds.sql` — ~10 przykładowych ras (pełny seed later)
- `GET /health` i `GET /health/ready` — oba zwracają `200 OK`
- `ApiResponse<T>`, `ErrorCode`, `GlobalExceptionHandler` w pakiecie `common/`
- Checkstyle + Spotless w `pom.xml`

### Struktura pakietów (tylko szkielet)
```
com.petsapp/
├── PetsAppApplication.java
├── common/
│   ├── ApiResponse.java
│   ├── ErrorCode.java
│   └── GlobalExceptionHandler.java
├── config/
│   └── (puste, wypełnimy kolejnych krokach)
└── health/
    └── HealthController.java
```

### Weryfikacja
```bash
docker compose up -d
mvn clean verify -pl backend
curl http://localhost:8080/api/v1/health
# Oczekiwane: {"success":true,"data":{"status":"UP"}}
```

> [!IMPORTANT]
> Flyway musi przejść wszystkie migracje bez błędów. Sprawdź `mvn flyway:info`.

---

## Krok 2 — Backend: Auth (email + JWT)

**Cel:** Rejestracja emailem, weryfikacja kodu, login, refresh token, logout.

### Zakres
- Encje JPA: `User`, `RefreshToken`, `EmailVerification`, `UserSettings`
- `SecurityConfig` — Spring Security + JWT filter, wszystkie endpointy `permitAll` poza `/users/me`
- `JwtProvider` — generowanie + walidacja access (15 min) + refresh (7 dni)
- `JwtFilter` — `OncePerRequestFilter`, czyta header `Authorization: Bearer`
- `AuthController` + `AuthService`:
  - `POST /auth/register` → walidacja, hash BCrypt (cost 12), wysyłka kodu (MailHog dev)
  - `POST /auth/verify-email` → aktywacja konta, zwrot tokenów
  - `POST /auth/resend-verification`
  - `POST /auth/login` → zwrot access + refresh token
  - `POST /auth/refresh` → nowy access token
  - `POST /auth/logout` → unieważnienie refresh tokena
- `EmailService` — wysyłka przez Spring Mail (MailHog na dev)
- Rate limiting na `/auth/*` przez Bucket4j + Redis (5 req/min login, 3 req/min register)
- **Testy integracyjne** z Testcontainers dla całego flow rejestracji

### Weryfikacja
```bash
# Uruchom testy
mvn test -pl backend -Dtest="AuthIntegrationTest"

# Manualne przez Swagger UI
open http://localhost:8080/swagger-ui.html
# 1. POST /auth/register
# 2. Sprawdź MailHog: http://localhost:8025
# 3. POST /auth/verify-email z kodem z emaila
# 4. POST /auth/login → skopiuj access token
# 5. Użyj tokena w Authorize (Swagger) → GET /users/me powinien zwrócić 200
```

---

## Krok 3 — Backend: Auth (reset hasła + OAuth Google)

**Cel:** Pełny flow resetu hasła + logowanie przez Google.

### Zakres
- `PasswordReset` encja JPA
- `POST /auth/forgot-password` — token ważny 1h, email z linkiem
- `POST /auth/reset-password` — walidacja tokena, update hasła, unieważnienie wszystkich refresh tokenów usera
- OAuth Google: `POST /auth/google { idToken }` — weryfikacja przez Google API, znajdź/utwórz usera
- `OAuthService` — logika find-or-create, generowanie username jeśli nowy user
- Testy integracyjne dla reset flow (mockuj email)

### Weryfikacja
```bash
mvn test -pl backend -Dtest="PasswordResetIntegrationTest,OAuthServiceTest"
# MailHog: http://localhost:8025 — sprawdź email z linkiem resetu
```

---

## Krok 4 — Backend: Users + Settings + Avatar

**Cel:** Endpointy profilu użytkownika.

### Zakres
- `UserController` + `UserService`:
  - `GET /users/me` — mój profil (statystyki z denormalizacji)
  - `PATCH /users/me` — edycja username/bio (walidacja unikalności)
  - `POST /users/me/avatar` — upload WebP na S3 (lokalnie: LocalStack lub MinIO w docker-compose)
  - `DELETE /users/me/avatar`
  - `PATCH /users/me/password` — wymaga starego hasła
  - `DELETE /users/me` — soft delete, unieważnienie tokenów
  - `GET /users/:id` — profil innego usera (respektuje `is_private`)
  - `GET /users/search?q=` — search po username
- `UserSettings` — `GET` + `PATCH /users/me/settings`
- `S3StorageService` — upload/delete, generowanie presigned URL
  - Dev: MinIO w docker-compose (kompatybilny z S3 API)
  - Prod: AWS S3

### Weryfikacja
```bash
mvn test -pl backend -Dtest="UserIntegrationTest,UserSettingsIntegrationTest"
# Sprawdź manualnie przez Swagger: upload avatara, edycja profilu
```

---

## Krok 5 — Backend: Breeds + Pokédex

**Cel:** Lista ras, seed pełnych danych (~200 ras), pokédex usera.

### Zakres
- `Breed` encja + `BreedRepository` + `BreedController` + `BreedService`
- `GET /breeds` — filtrowanie `?q=`, `?group=`, `?size=`
- `GET /breeds/:id` — szczegóły + globalne statystyki (ile userów złapało)
- `GET /users/:id/pokedex` — odkryte rasy usera
- `GET /users/:id/pokedex/stats` — statystyki: postęp, streak, ulubiona rasa
- Migracja `V2__seed_breeds.sql` — pełna lista ~200 ras (AKC/FCI) z `rarity_score`

### Weryfikacja
```bash
mvn test -pl backend -Dtest="BreedControllerTest"
curl "http://localhost:8080/api/v1/breeds?q=golden"
# Oczekiwane: lista z Golden Retriever
```

---

## Krok 6 — Backend: Catches + Upload zdjęć

**Cel:** Złapanie psa z uploadem zdjęcia na S3.

### Zakres
- `DogCatch` encja + `CatchRepository` + `CatchController` + `CatchService`
- `POST /catches` — multipart upload: walidacja MIME, resize Thumbnailator (800px + 200px thumb), konwersja WebP, upload S3
- `GET /catches/:id`, `DELETE /catches/:id` (soft delete)
- `POST /catches/:id/like` / `DELETE /catches/:id/like` — z denormalizacją `like_count`
- `GET /catches/:id/comments` (paginated cursor) + `POST` + `DELETE /comments/:id`
- `POST /catches/:id/report` — zapis do `report` tabeli
- `GET /users/:id/catches?cursor=` — grid profilu, paginated
- `ImageResizer` — Thumbnailator wrapper, strip EXIF metadata
- Rate limit upload: 10/min

### Weryfikacja
```bash
mvn test -pl backend -Dtest="CatchIntegrationTest"
# Manualne przez Swagger: upload zdjęcia, sprawdź że miniaturka jest w MinIO
```

---

## Krok 7 — Backend: Feed + Ranking

**Cel:** Działający feed z rankingiem i cache Redis.

### Zakres
- `FeedController` + `FeedService` + `FeedRankingService`
- `GET /feed/public?cursor=&limit=20` — ranked by `feed_score`
- `GET /feed/friends?cursor=&limit=20` — chronologiczny, tylko znajomi
- `GET /feed/trending?limit=10` — top 10 z ostatnich 24h
- Redis Sorted Sets: `feed:public`, `feed:friends:{userId}`, `feed:trending`
- `FeedRebuildJob` — `@Scheduled` co 15 min, aktualizuje score'y
- Formuła: `(likes * 1.0 + comments * 2.0 + rarity * 3) / (hours + 2)^1.5`
- Cache TTL: public = 15 min, friends = 5 min, trending = 5 min
- Cursor pagination przez `ZREVRANGEBYSCORE`

### Weryfikacja
```bash
mvn test -pl backend -Dtest="FeedServiceTest,FeedRankingServiceTest"
# Integration: dodaj catch, polajkuj, sprawdź że pojawia się w feed:public
```

---

## Krok 8 — Backend: Friends + Notifications + Achievements

**Cel:** Znajomi, push notyfikacje, gamifikacja.

### Zakres (w kolejności)

#### 8a — Friends
- `Friendship` encja + `FriendController` + `FriendService`
- Request / Accept / Reject / Delete / Block flow
- `GET /friends/leaderboard` — sorted by `unique_breeds` DESC, Redis cache 1h

#### 8b — Notifications
- `Notification` encja + `NotificationController` + `NotificationService`
- `DeviceToken` + `POST /devices`, `DELETE /devices/:token`
- `PushService` — Firebase Admin SDK, `@Async` wysyłka
- Respektuj `user_settings` (wyłączone typy push)
- Batch: grupowanie jeśli >5 lajków w 1 min

#### 8c — Achievements
- `Achievement` seed (`V3__seed_achievements.sql`) — 9 achievementów z README
- `AchievementChecker` — wywołany async po każdym `POST /catches`
- Sprawdzanie warunków: `total_catches`, `unique_breeds`, `rarity_score`, streak
- Push notification przy odblokowaniu

### Weryfikacja
```bash
mvn test -pl backend -Dtest="FriendIntegrationTest,NotificationServiceTest,AchievementCheckerTest"
# Manualne: dodaj znajomego, polajkuj catch → sprawdź że notification pojawia się w GET /notifications
```

---

## Co PO tych krokach?

Po ukończeniu kroków 1–8 masz działający backend MVP. Następne etapy:

| Etap | Co |
|---|---|
| **Krok 9** | Frontend setup: Expo + React Navigation + NativeWind + Zustand + API client |
| **Krok 10** | Frontend: Auth screens (login/register/verify/forgot) |
| **Krok 11** | Frontend: Feed + Catch screen + Pokédex |
| **Krok 12** | Frontend: Profil + Znajomi + Achievements + Push |
| **Krok 13** | DevOps: CI/CD GitHub Actions, Railway staging deploy |
| **Krok 14** | Testy E2E + beta testing |

---

## Weryfikacja ogólna (po każdym kroku)

```bash
# Backend musi zawsze przechodzić
cd backend
mvn clean verify         # kompilacja + wszystkie testy
mvn checkstyle:check     # styl kodu
mvn spotless:check       # formatowanie

# Docker
docker compose up -d
curl http://localhost:8080/api/v1/health
```

> [!NOTE]
> Swagger UI dostępny na `http://localhost:8080/swagger-ui.html` przez cały czas developmentu.
