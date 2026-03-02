# Krok 8: Backend — Znajomi, Powiadomienia, Achievementy

## Cel

Implementacja trzech powiązanych modułów uzupełniających backend aplikacji:

- **8a. Znajomi (Friends)** — system znajomości, blokowania i tabeli liderów
- **8b. Powiadomienia (Notifications)** — in-app notifications + push FCM przez Firebase
- **8c. Achievementy (Achievements)** — seeded lista osiągnięć + mechanizm odblokowywania

Wszystkie moduły są ze sobą zintegrowane: np. zaakceptowanie znajomego wysyła powiadomienie,
a zdobycie achievementu też generuje push.

---

## Zmiany w schemacie bazy danych

### V5 — Migracja `friendship_status` (nowa, naprawcza)

Plik: `V5__fix_friendship_status_column.sql`

Zmiana zakodowania kolumny `friendship.status` z PostgreSQL natywnego typu `ENUM` na `VARCHAR(20)`.

**Powód:** Hibernate 6 z `@Enumerated(EnumType.STRING)` operuje na wartościach uppercase
(`PENDING`, `ACCEPTED`, `BLOCKED`), natomiast V1 zdefiniował enum z lowercase (`pending`, `accepted`, `blocked`).
Hibernate 6 generuje niekompatybilne rzutowanie typów (`::FriendshipStatus` zamiast `::friendship_status`).

**Po migracji:**
- wartości uppercase: `PENDING`, `ACCEPTED`, `BLOCKED`
- CHECK constraint zapewnia integralność danych
- natywny typ enum `friendship_status` jest usunięty

### V4 — Seed achievementów

Plik: `V4__seed_achievements.sql`

9 predefiniowanych osiągnięć wstawianych przy starcie:

| Kod | Opis | Warunek |
|-----|------|---------|
| `FIRST_CATCH` | Pierwsze złowienie | `total_catches >= 1` |
| `TEN_CATCHES` | 10 złowień | `total_catches >= 10` |
| `FIFTY_CATCHES` | 50 złowień | `total_catches >= 50` |
| `HUNDRED_CATCHES` | 100 złowień | `total_catches >= 100` |
| `EXPLORER` | Odkrywca (5 ras) | `unique_breeds >= 5` |
| `BREED_COLLECTOR` | Kolekcjoner (20 ras) | `unique_breeds >= 20` |
| `RARE_HUNTER` | Łowca rzadkich | `rare_catch >= 4` (rarity score) |
| `SOCIAL` | Towarzyski (5 znajomych) | `friends_count >= 5` |
| `WEEK_STREAK` | Tygodniowy streak | `streak >= 7` |

---

## 8a. Moduł Znajomi

### Encja `Friendship`

```
friendship
├── id             UUID (PK)
├── requester_id   UUID (FK → users)
├── addressee_id   UUID (FK → users)
├── status         VARCHAR(20) CHECK IN ('PENDING','ACCEPTED','BLOCKED')
└── created_at     TIMESTAMP
```

Relacja jest kierunkowa: `requester` wysyła prośbę, `addressee` akceptuje/odrzuca.
Blokowanie jest jednostronne — `requester` blokuje `addressee`.

### API Endpoints

Wszystkie endpointy wymagają autoryzacji JWT (`Authorization: Bearer <token>`).

#### POST /friends/requests
Wysłanie prośby o znajomość.

**Body:**
```json
{ "targetUserId": "uuid" }
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "friendshipId": "uuid",
    "userId": "uuid",
    "username": "bob",
    "status": "pending",
    "createdAt": "2025-01-01T12:00:00Z"
  }
}
```

**Błędy:**
- `409 Conflict` — prośba do siebie lub relacja już istnieje

#### GET /friends/requests/pending
Lista oczekujących próśb (adresowanych do mnie).

#### PATCH /friends/requests/{id}/accept
Akceptacja prośby (tylko addressee może akceptować).

#### DELETE /friends/requests/{id}
Odrzucenie/anulowanie prośby o znajomość.

#### GET /friends
Lista zaakceptowanych znajomych.

#### DELETE /friends/{targetUserId}
Usunięcie znajomego (unfriend). Działa tylko dla relacji `ACCEPTED`.

#### POST /friends/block/{targetUserId}
Zablokowanie użytkownika. Jeśli istnieje relacja (PENDING/ACCEPTED) — jest zastąpiona BLOCKED.

#### GET /friends/leaderboard
Tabela liderów — znajomi + aktualny użytkownik, posortowani po `unique_breeds` malejąco.

**Response:**
```json
{
  "success": true,
  "data": [
    { "rank": 1, "userId": "uuid", "username": "alice", "uniqueBreeds": 42, "totalCatches": 100 },
    { "rank": 2, "userId": "uuid", "username": "bob",   "uniqueBreeds": 15, "totalCatches": 30 }
  ]
}
```

---

## 8b. Moduł Powiadomień

### Encje

**`notification`**
```
id          UUID
user_id     UUID (FK → users)
type        VARCHAR(50)   — patrz NotificationTypes
title       VARCHAR(100)
body        TEXT
data_json   JSONB
is_read     BOOLEAN DEFAULT false
created_at  TIMESTAMP
```

**`device_token`** — tokeny FCM dla push notifications
```
id          UUID
user_id     UUID (FK → users)
token       VARCHAR(500) UNIQUE
platform    VARCHAR(10) CHECK IN ('ios', 'android')
created_at  TIMESTAMP
```

### Typy powiadomień (`NotificationTypes`)

| Stała | Wartość | Kiedy wysyłane |
|-------|---------|----------------|
| `LIKE` | `like` | Ktoś polubił catch |
| `BATCH_LIKES` | `batch_likes` | Kilka polubień w ciągu 60s |
| `COMMENT` | `comment` | Ktoś skomentował catch |
| `FRIEND_REQUEST` | `friend_request` | Prośba o znajomość |
| `FRIEND_ACCEPTED` | `friend_accepted` | Znajomy zaakceptował |
| `ACHIEVEMENT` | `achievement` | Odblokowanie achievementu |

### Batching polubień

Powiadomienia o polubieniach są grupowane w 60-sekundowym oknie.
Jeśli w ciągu 60s catch dostał N polubień (N>1), wysyłane jest jedno powiadomienie `batch_likes`
zamiast N powiadomień `like`. Okno jest wykrywane przez sprawdzenie ostatniego powiadomienia danego
typu dla danego catcha w bazie danych.

### Integracja Firebase FCM

Firebase Admin SDK jest opcjonalny — działa tylko jeśli `firebase.enabled=true`
i podano ścieżkę do pliku credentials.

W środowisku deweloperskim i testowym Firebase jest wyłączony — push jest pominięty bez błędu.

**Konfiguracja `application.yml`:**
```yaml
firebase:
  enabled: false
  credentials-file: /path/to/service-account.json
```

**Aby włączyć push:**
1. Wygeneruj plik `service-account.json` w Firebase Console → Project Settings → Service Accounts
2. Ustaw `firebase.enabled=true` i podaj ścieżkę
3. Aplikacja mobilna musi zarejestrować token FCM przez `POST /devices`

### API Endpoints

#### GET /notifications
Lista ostatnich 50 powiadomień zalogowanego użytkownika. Zwraca `unreadCount`.

#### POST /notifications/read-all
Oznaczenie wszystkich powiadomień jako przeczytane.

#### POST /devices
Rejestracja tokenu FCM.

**Body:**
```json
{ "token": "fcm-token-xyz", "platform": "android" }
```

Duplikaty tokenów są ignorowane (idempotentne).

#### DELETE /devices/{token}
Wyrejestrowanie tokenu FCM.

### Preferencje push (UserSettings)

Użytkownik może wyłączyć poszczególne typy powiadomień w ustawieniach:
- `pushLikes` — polubienia
- `pushComments` — komentarze
- `pushFriendRequests` — prośby o znajomość
- `pushAchievements` — achievementy

---

## 8c. Moduł Achievementów

### Encje

**`achievement`** (seeded, read-only)
```
id              BIGINT (GENERATED ALWAYS AS IDENTITY)
code            VARCHAR(50) UNIQUE
name_pl         VARCHAR(100)
description_pl  TEXT
condition_type  VARCHAR(30)
condition_value INT
icon_name       VARCHAR(50)
```

**`achievement_unlock`**
```
id              UUID (PK)
user_id         UUID (FK → users)
achievement_id  BIGINT (FK → achievement)
unlocked_at     TIMESTAMP
```

### Mechanizm sprawdzania

`AchievementChecker.checkAfterCatch(User user, int caughtBreedRarity)` jest wywoływany
**asynchronicznie** po każdym nowym złowieniu przez `CatchService`.

Dla każdego achievementu który nie jest jeszcze odblokowany, ewaluowany jest warunek:

| `condition_type` | Źródło danych |
|-----------------|-|
| `total_catches` | `user.totalCatches` (denormalizowany licznik) |
| `unique_breeds` | `user.uniqueBreeds` (denormalizowany licznik) |
| `rare_catch` | parametr `caughtBreedRarity` (rarity score 1-5) |
| `friends_count` | `FriendshipRepository.countAcceptedFriends(userId)` |
| `streak` | liczba kolejnych dni z catch z `DogCatchRepository` |

### API Endpoints

#### GET /achievements
Lista wszystkich dostępnych achievementów (publiczna, bez JWT).
Zwraca `unlocked: false` dla wszystkich.

#### GET /users/me/achievements
Achievementy zalogowanego użytkownika z flagą `unlocked: true/false`.

#### GET /users/{userId}/achievements
Achievementy dowolnego użytkownika (publiczne — widać co odblokował).

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "code": "FIRST_CATCH",
      "namePl": "Pierwsze złowienie!",
      "descriptionPl": "Złap pierwszego psa",
      "conditionType": "total_catches",
      "conditionValue": 1,
      "iconName": "paw",
      "unlocked": true,
      "unlockedAt": "2025-01-01T10:00:00Z"
    }
  ]
}
```

---

## Asynchroniczność

Moduł powiadomień i achievementów używa puli wątków `taskExecutor`:

```
AsyncConfig:
  core pool size:    5
  max pool size:    20
  queue capacity:  100
  thread name:     petsapp-async-{N}
```

Metody oznaczone `@Async("taskExecutor")`:
- `PushService.sendToUser()` — wysyłka FCM
- `NotificationService.notifyLike()`, `notifyComment()`, itd.
- `AchievementChecker.checkAfterCatch()`

Dzięki temu `POST /catches/{id}/like` nie blokuje na operacjach push/achievement.

---

## Testy

### Dodane klasy testowe

| Klasa | Testy | Pokrycie |
|-------|-------|----------|
| `FriendIntegrationTest` | 13 | sendRequest, acceptRequest, rejectRequest, removeFriend, blockUser, getFriends, getPendingRequests, getLeaderboard |
| `NotificationServiceTest` | 6 | getNotifications, notifyLike (async), markAllRead, registerDevice (dedup), unregisterDevice |
| `AchievementCheckerTest` | 7 | FIRST_CATCH unlock, RARE_HUNTER unlock, dedup, no unlock when condition not met, TEN_CATCHES, rarity=5, seed count |

### Modyfikacje istniejących testów

- `AbstractIntegrationTest` — dodano `@MockBean(StringRedisTemplate.class)` (wymagane przez `FriendService`)
- `FeedServiceTest` — poprawiono INSERT friendship z lowercase na uppercase wartości (po migracji V5)
- `DogCatchRepository` native SQL queries — `'accepted'` → `'ACCEPTED'`

### Uruchomienie tylko testów Step 8

```bash
cd backend
mvn test -Dtest="FriendIntegrationTest,NotificationServiceTest,AchievementCheckerTest"
```

### Uruchomienie pełnego suite

```bash
mvn test
# Expected: 125 tests, 0 failures
```

---

## Integracja z istniejącymi modułami

### CatchService (Krok 6)

Po każdym nowym catch (`createCatch`):
1. Inkrementuje liczniki `user.totalCatches` i `user.uniqueBreeds`
2. Wywołuje asynchronicznie `achievementChecker.checkAfterCatch(user, breed.getRarityScore())`

Po `toggleLike`:
- Jeśli like (nie unlike): `notificationService.notifyLike(catchOwner, liker.username, catchId)`
- Pomijane jeśli liker == catchOwner (self-like)

Po `addComment`:
- `notificationService.notifyComment(catchOwner, commenter.username, catchId)`
- Pomijane jeśli commenter == catchOwner

### FriendService (Krok 8a)

Po `sendRequest`:
- `notificationService.notifyFriendRequest(addressee, requesterUsername, friendshipId)`

Po `acceptRequest`:
- `notificationService.notifyFriendAccepted(requester, acceptorUsername)`

### SecurityConfig

`/achievements` dodano do `PUBLIC_ENDPOINTS` — lista achievementów jest publiczna bez JWT.

---

## Architektura — decyzje projektowe

### 1. PostgreSQL ENUM → VARCHAR

Oryginalny schemat (V1) używał PostgreSQL natywnego typu ENUM z lowercase wartościami.
Hibernate 6 wymaga uppercase z `@Enumerated(EnumType.STRING)`. Zamiast utrzymywać
niestandardową konfigurację typów Hibernate, migracja V5 konwertuje kolumnę na `VARCHAR(20)`.

**Zalety:**
- Naturalny mapping z Java enum
- Brak zależności od hibernatowego `@JdbcTypeCode`
- CHECK constraint zachowuje integralność danych

### 2. Batching powiadomień o polubieniach

Zamiast wysyłać push przy każdym like (co jest spam dla popularnych catchy),
powiadomienia są grupowane w 60-sekundowym oknie. Implementacja bez zewnętrznego
schedulera — okno jest wykrywane przez sprawdzenie ostatniego powiadomienia w DB.

### 3. Firebase opcjonalny

`PushService.sendToUser()` sprawdza `FirebaseApp.getApps().isEmpty()` przed wysyłką.
Jeśli Firebase nie jest zainicjalizowany (dev/test), push jest pominięty bez wyjątku.
Tokeny UNREGISTERED są automatycznie usuwane z `device_token`.

### 4. Achievementy read-only + seeded

Lista achievementów jest stała i seedowana przez Flyway (V4). Dodawanie nowych
achievementów wymaga nowej migracji Flyway + kodu w `AchievementChecker`.
Nie ma endpointu admin do zarządzania achievementami — upraszcza model danych.
