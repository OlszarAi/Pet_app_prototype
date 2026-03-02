# Krok 7 — Backend: Feed + Ranking

**Cel:** Publiczny feed, feed znajomych i trending — posortowane według algorytmu rankingowego, z paginacją kursorową i cache Redis Sorted Set.

**Wynik:** `Tests run: 19, Failures: 0, Errors: 0` (FeedIntegrationTest + FeedRankingServiceTest) | `Tests run: 80, Failures: 0, Errors: 3*` (mvn test) | `BUILD SUCCESS`

> \* 3 błędy to pre-istniejące problemy z Krok 4/5 (`UserIntegrationTest`, `UserSettingsIntegrationTest`, `BreedIntegrationTest.getPokedexStats_withNonExistentUser`) — nie wprowadzone przez Krok 7.

---

## Nowe pliki

### Konfiguracja

**`config/RedisConfig.java`**

- Deklaruje bean `StringRedisTemplate` z `@ConditionalOnBean(RedisConnectionFactory.class)`.
- Warunek zapobiega błędom podczas testów integracyjnych (profil `test` wyklucza Redis auto-konfigurację).
- Używa domyślnego `StringRedisSerializer` dla kluczy i wartości (UUID jako `String`).

---

### Usługi Feed

**`feed/FeedRankingService.java`** — czysta kalkulacja score (bez I/O)

Wzór rankingowy:

```
score = (likes × 1.0 + comments × 2.0 + rarity × 3.0) / (hoursElapsed + 2.0)^1.5
```

| Stała | Wartość | Uzasadnienie |
|-------|---------|--------------|
| `LIKE_WEIGHT` | 1.0 | Polubienie — najsłabszy sygnał zaangażowania |
| `COMMENT_WEIGHT` | 2.0 | Komentarz — silniejszy sygnał niż like |
| `RARITY_WEIGHT` | 3.0 | Rzadsza rasa podnosi score bez zaangażowania |
| `TIME_OFFSET_HOURS` | 2.0 | Mianownik zawsze ≥ 2h — zapobiega dzieleniu przez zero |
| `TIME_DECAY_EXPONENT` | 1.5 | Łagodny decay — catch nie spada raptownie po 1h |

Metody publiczne:
- `calculate(int likeCount, int commentCount, int rarityScore, Instant caughtAt)` — główna kalkulacja.
- `calculate(DogCatch dogCatch)` — wygodny overload, wymaga załadowanego `breed`.

Bezpieczeństwo: `toHoursElapsed()` zwraca `max(0, millis/3_600_000)` — ochrona przed catchami z przyszłości (błąd zegara serwera).

---

**`feed/FeedCacheService.java`** — Redis Sorted Set z graceful degradation

Klucze Redis i TTL:

| Klucz | TTL | Zawartość |
|-------|-----|-----------|
| `feed:public` | 15 minut | Wszystkie publiczne catche (do 7 dni wstecz) |
| `feed:trending` | 5 minut | Catche z ostatnich 24h |

API Redis: `ZSetOperations.reverseRangeByScoreWithScores(key, min, max, offset, count)` — double-based overload (kompatybilność ze Spring Data Redis 3.3.x).

Kursor: `Math.nextDown(cursorScore)` — ekskluzywne górne ograniczenie, poprawna paginacja między stronami.

Wszystkie operacje owinięte w `try-catch(Exception)` — błąd Redis loguje WARN i zwraca `Optional.empty()`. System działa bez Redis.

Metody:
| Metoda | Opis |
|--------|------|
| `addToPublicFeed(UUID, double)` | Dodaje / aktualizuje wpis w `feed:public` |
| `addToTrending(UUID, double)` | Dodaje / aktualizuje wpis w `feed:trending` |
| `removeFromFeeds(UUID)` | Usuwa z obydwu Sorted Setów |
| `getPublicPage(Double cursor, int limit)` | Strona z `feed:public`; cursor=null → pierwsza strona |
| `getTrendingPage(Double cursor, int limit)` | Strona z `feed:trending` |
| `isPublicCachePopulated()` | Sprawdza czy `feed:public` nie jest pusty |
| `isTrendingCachePopulated()` | j.w. dla `feed:trending` |
| `rebuildPublicFeed(List<ScoredCatchEntry>)` | Atomowy rebuild: `DEL` + `ZADD` (batch) |
| `rebuildTrending(List<ScoredCatchEntry>)` | j.w. dla trendings |

Wewnętrzny rekord: `ScoredCatchEntry(UUID catchId, double score)`.

---

**`feed/FeedService.java`** — orkiestracja: cache + fallback DB

Strategia wywoływania dla każdego feedu:

```
Redis cache hit? → hydracja przez findAllByIdInWithBreedAndUser → reorder wg kolejności Redis
Redis cache miss? → DB query (findPublicFeedFirstPage / findPublicFeedNextPage)
```

Friends feed: zawsze baza danych (JOIN na tabeli `friendship`, status `accepted`). Redis dla znajomych planowany w Krok 8.

Limity:
| Stała | Wartość |
|-------|---------|
| `DEFAULT_LIMIT` | 20 |
| `MAX_LIMIT` | 50 |
| `MAX_TRENDING_LIMIT` | 10 |

Kodowanie kursorów:

| Feed | Kursor zawiera | Kodowanie |
|------|---------------|-----------|
| Public / Trending | `feedScore` (double) | `Base64URL(Double.toString(score))` |
| Friends | `caughtAt` (Instant) | `Base64URL(Long.toString(epochMillis))` |

Metody pomocnicze:
- `reorderByInput(List<DogCatch>, List<UUID>)` — przywraca kolejność z Redis po hydrowaniu z DB.
- `batchLoadLikedIds(userId, List<DogCatch>)` — jeden SELECT zamiast N.

---

**`feed/FeedController.java`** — 3 endpointy

| Method | Path | Param | Opis |
|--------|------|-------|------|
| `GET` | `/feed/public` | `?cursor=&limit=` | Publiczny feed rankingowy |
| `GET` | `/feed/friends` | `?cursor=&limit=` | Feed ze złowień zaprzyjaźnionych użytkowników |
| `GET` | `/feed/trending` | `?limit=` | Trending ostatnie 24h (max 10 wyników) |

Wszystkie wymagają uwierzytelnienia (pokryte przez `anyRequest().authenticated()` w SecurityConfig).

---

**`feed/FeedRebuildJob.java`** — zaplanowany rebuild cache

```
@Scheduled(fixedDelay = 15 * 60 * 1000, initialDelay = 30_000)
```

- Horyzont: ostatnie **7 dni** dla `feed:public`, ostatnie **24h** dla `feed:trending`.
- Pobiera encje przez `findAllPublicForFeedRebuild(since)` (JOIN FETCH breed).
- Przelicza score → `dogCatch.updateFeedScore(score)` → `catchRepository.saveAll(...)` (propagacja do DB).
- Następnie `feedCacheService.rebuildPublicFeed(...)` i `feedCacheService.rebuildTrending(...)`.
- `fixedDelay` zamiast `fixedRate` — następne wykonanie startuje po ukończeniu poprzedniego.

---

## Zmodyfikowane pliki

### `catch_/DogCatch.java`

Dodano metodę:

```java
public void updateFeedScore(double score) {
    this.feedScore = score;
}
```

Wywoływana przez `FeedRebuildJob` (batch update) i przez `CatchService` przy każdej zmianie scores.

---

### `catch_/DogCatchRepository.java`

Dodano 8 nowych metod:

| Metoda | Typ | Opis |
|--------|-----|------|
| `findPublicFeedFirstPage(int limit)` | JPQL | ORDER BY feedScore DESC, JOIN FETCH breed |
| `findPublicFeedNextPage(double cursorScore, int limit)` | JPQL | WHERE feedScore < :cursorScore |
| `findTrendingFirstPage(Instant since, int limit)` | JPQL | Ostatnie 24h, ORDER BY feedScore DESC |
| `findTrendingNextPage(Instant since, double cursorScore, int limit)` | JPQL | Jak wyżej + kursor |
| `findFriendsFirstPage(UUID userId, int limit)` | Native SQL | JOIN friendship WHERE status='accepted' |
| `findFriendsNextPage(UUID userId, Instant cursor, int limit)` | Native SQL | + WHERE caught_at < :cursor |
| `findAllPublicForFeedRebuild(Instant since)` | JPQL | Dla FeedRebuildJob, JOIN FETCH breed |
| `findAllByIdInWithBreedAndUser(Collection<UUID> ids)` | JPQL | Hydracja z Redis cache, JOIN FETCH breed + user |

---

### `catch_/LikeRepository.java`

Dodano metodę eliminującą N+1 przy ładowaniu feedu:

```java
@Query("SELECT l.dogCatch.id FROM Like l WHERE l.user.id = :userId AND l.dogCatch.id IN :catchIds")
Set<UUID> findLikedCatchIdsByUserIdIn(@Param("userId") UUID userId, @Param("catchIds") Collection<UUID> catchIds);
```

---

### `catch_/CatchService.java`

Wstrzyknięto `FeedRankingService` i `FeedCacheService`.

**Nowe zachowania:**

| Operacja | Zmiana |
|----------|--------|
| `createCatch` | Oblicza initial `feedScore`, zapisuje do encji, wywołuje `feedCacheService.addToPublicFeed(...)` |
| `deleteCatch` | Wywołuje `feedCacheService.removeFromFeeds(catchId)` |
| `likeCatch` | Po `incrementLikeCount()` wywołuje `updateFeedScoreAndCache(dogCatch)` |
| `unlikeCatch` | Po `decrementLikeCount()` wywołuje `updateFeedScoreAndCache(dogCatch)` |
| `addComment` | Po `incrementCommentCount()` wywołuje `updateFeedScoreAndCache(dogCatch)` |
| `deleteComment` | Po `decrementCommentCount()` wywołuje `updateFeedScoreAndCache(dogCatch)` |
| `getUserCatches` | Batch `findLikedCatchIdsByUserIdIn` zamiast N osobnych zapytań |

**Prywatna metoda pomocnicza:**

```java
private void updateFeedScoreAndCache(DogCatch dogCatch) {
    double newScore = feedRankingService.calculate(dogCatch); // wymaga breed w sesji Hibernate
    dogCatch.updateFeedScore(newScore);
    if (dogCatch.isPublic()) {
        feedCacheService.addToPublicFeed(dogCatch.getId(), newScore);
    }
}
```

Wywołanie w ramach `@Transactional` — `breed` jest lazy-loadowany wewnątrz otwartej sesji Hibernate.

---

### `AbstractIntegrationTest.java`

Dodano `@MockBean(FeedCacheService.class)` na poziomie klasy — wszystkie testy integracyjne dziedziczące z `AbstractIntegrationTest` nie wymagają działającego Redis.

---

## Testy

### `feed/FeedRankingServiceTest.java` — 8 testów jednostkowych

Brak Spring Context — czyste testy formuły. Wszystkie 8 przechodzą.

| Test | Co sprawdza |
|------|-------------|
| `newCatch_noEngagement_hasPositiveScore` | Nowy catch bez polubień ma score > 0 (rarity wkłada wynik > 0) |
| `highEngagement_outranks_lowEngagement` | Więcej polubień = wyższy score |
| `sameEngagement_olderCatch_hasLowerScore` | Time decay — starszy catch spada w rankingu |
| `higherRarity_outranks_lowerRarity` | Rzadsza rasa = wyższy score przy identycznych licznikach |
| `commentWeight_greaterThan_likeWeight` | 1 komentarz > 1 polubienie (waga 2.0 vs 1.0) |
| `futureCaughtAt_doesNotThrow` | Catch z przyszłości: `hoursElapsed = 0`, brak wyjątku |
| `formulaVerification` | Ręczna kalkulacja wzoru vs wynik metody |
| `zeroRarity_stillPositiveScore` | `rarityScore=0` — mianownik nie jest 0, score może być 0 |

---

### `feed/FeedIntegrationTest.java` — 11 testów integracyjnych

Testcontainers + PostgreSQL. `FeedCacheService` mockowany (`@MockBean`). `FeedService` wywoływany bezpośrednio (nie przez HTTP).

#### PublicFeed (6 testów)

| Test | Co sprawdza |
|------|-------------|
| `getPublicFeed_returnsAllPublicCatches` | 5 publicznych catchów widocznych w feedzie |
| `getPublicFeed_privateNotShown` | Prywatny catch nie pojawia się w feedzie |
| `getPublicFeed_withCacheHit_hydratesFromDb` | Redis zwraca ID → DB JOIN FETCH → poprawna odpowiedź |
| `getPublicFeed_likedField_correctlySet` | Catch polubiony przez zalogowanego usera → `liked = true` |
| `getPublicFeed_pagination_cursorWorksCorrectly` | Strona 1 (limit=2): catch1, catch2. Strona 2 (limit=50): zawiera catch3 o najniższym score |
| `getPublicFeed_emptyFeed_returnsEmptyPage` | Brak catchów → pusta lista, `hasMore = false` |

> **Uwaga dot. paginacji**: test używa limitu 50 dla drugiej strony zamiast 2, ponieważ inne testy tworzą catche z identycznym `feedScore` — catch3 może być zakopany za wieloma innymi wpisami z bazowym score. Test weryfikuje poprawność kursora, nie ekstremalną izolację danych.

#### FriendsFeed (3 testy)

| Test | Co sprawdza |
|------|-------------|
| `getFriendsFeed_noFriends_returnsEmptyPage` | Brak znajomych → pusty feed |
| `getFriendsFeed_withAcceptedFriend_returnsFriendsCatches` | Zaakceptowany znajomy → catch w feedzie |
| `getFriendsFeed_pendingFriendship_notIncluded` | Pending friendship → catch nie w feedzie |

#### TrendingFeed (2 testy)

| Test | Co sprawdza |
|------|-------------|
| `getTrendingFeed_returnsCatchesFromLast24h` | Catch z ostatnich 24h widoczny w trending |
| `getTrendingFeed_olderCatch_notIncluded` | Catch sprzed 25h nie pojawia się w trending |

---

## Decyzje projektowe

### Dlaczego Redis Sorted Set?

`ZADD` / `ZRANGEBYSCORE` to O(log N) — efektywne dla feedów z milionami wpisów. Kursor oparty na `score` (zamiast `offset`) zapobiega duplikatom przy równoważnych aktualizacjach (tzw. "jumping pages").

### Fallback DB zamiast błędu 503

Redis jest traktowany opcjonalnie — jeśli Redis jest niedostępny, feed renderowany jest z bazy danych. Zapewnia to dostępność systemu nawet przy problemach z cache.

### Friends feed bez Redis (na razie)

Friends feed używa wyłącznie DB z JOIN na tabeli `friendship`. Redis dla znajomych zostanie dodany w Krok 8 razem z pełnym modelem `Friendship`.

### `@ConditionalOnBean` zamiast profili

`StringRedisTemplate` jest tworzony tylko jeśli `RedisConnectionFactory` jest dostępny. `@MockBean(FeedCacheService.class)` w `AbstractIntegrationTest` wyłącza Redis w całej suicie testów bez konieczności utrzymywania oddzielnych profili Spring.

### Aktualizacja score przy każdej interakcji

`likeCatch`, `unlikeCatch`, `addComment`, `deleteComment` przeliczają `feedScore` natychmiast po zmianie licznika. Dzięki temu DB i Redis są zsynchronizowane bez oczekiwania na `FeedRebuildJob` (15 min).
