# Krok 5 — Backend: Breeds + Pokedex

## Co zostalo zrobione

Krok 5 dodaje katalog ras psow oraz Pokedex — indywidualna kolekcje odkrytych ras uzytkownika:

1. **Lista ras** — filtrowanie po fragmencie nazwy (EN/PL), grupie AKC/FCI i rozmiarze. Publiczne (bez JWT).
2. **Szczegoly rasy** — pelne dane + globalne statystyki (ilu unikalnych uzytkownikow zlowilo te rase, ile razy lacznie).
3. **Pokedex** — lista ras odkrytych przez uzytkownika posortowana malejaco po liczbie zlowien.
4. **Statystyki Pokedeksu** — postep (unikalne rasy / aktywne rasy), streak dzienny, ulubiona rasa, totale.
5. **Encja `DogCatch`** — pelna definicja (Krok 6 doda tylko Controller + Service), z Builderem.
6. **Seed V3** — 65 dodatkowych ras z podzialem na grupy AKC/FCI (rarity 1-5), lacznie z V2 ~75 ras.
7. **Kontrola dostepu do prywatnego profilu** — Pokedex prywatnego konta widzi tylko jego wlasciciel.

Po Kroku 5 liczba testow: **63 (46 z Kroku 4 + 17 BreedIntegration)**.

---

## Struktura plikow po Kroku 5

Nowe pliki sa oznaczone `<- NOWY`, zmodyfikowane `<- ZAKTUALIZOWANY`.

```
backend/
└── src/
    ├── main/
    │   ├── java/com/petsapp/
    │   │   ├── breed/                                       <- NOWY pakiet
    │   │   │   ├── Breed.java                               <- NOWY: encja JPA
    │   │   │   ├── BreedRepository.java                     <- NOWY: JPQL filter query
    │   │   │   ├── BreedService.java                        <- NOWY: logika ras i Pokedeksu
    │   │   │   ├── BreedController.java                     <- NOWY: GET /breeds, GET /breeds/:id
    │   │   │   ├── PokedexController.java                   <- NOWY: GET /users/:id/pokedex[/stats]
    │   │   │   ├── BreedResponse.java                       <- NOWY: DTO listy ras (record)
    │   │   │   ├── BreedDetailResponse.java                 <- NOWY: DTO szczegolow z globalstats (record)
    │   │   │   ├── PokedexEntryResponse.java                <- NOWY: wpis Pokedeksu z catchCount (record)
    │   │   │   ├── PokedexStatsResponse.java                <- NOWY: statystyki Pokedeksu (record)
    │   │   │   ├── BreedNotFoundException.java              <- NOWY: 404
    │   │   │   └── PokedexAccessDeniedException.java        <- NOWY: 403 dla prywatnych profili
    │   │   ├── catch_/                                      <- NOWY pakiet (pelny Krok 6)
    │   │   │   ├── DogCatch.java                            <- NOWY: encja JPA z Builderem
    │   │   │   └── DogCatchRepository.java                  <- NOWY: query dla Pokedeksu + globalstats
    │   │   ├── auth/
    │   │   │   └── User.java                                <- ZAKTUALIZOWANY: metoda updatePrivacy(boolean)
    │   │   ├── common/
    │   │   │   └── GlobalExceptionHandler.java              <- ZAKTUALIZOWANY: 2 nowe handlery
    │   │   └── config/
    │   │       └── SecurityConfig.java                      <- ZAKTUALIZOWANY: /breeds/** publiczne
    │   └── resources/
    │       └── db/migration/
    │           └── V3__seed_breeds_extended.sql             <- NOWY: 65 ras (wszystkie grupy AKC/FCI)
    └── test/
        └── java/com/petsapp/
            └── breed/
                └── BreedIntegrationTest.java                <- NOWY: 17 testow integracyjnych
```

---

## Endpointy

Wszystkie endpointy sa pod prefiksem `/api/v1` (konfiguracja serwera).

| Metoda  | Sciezka                         | Auth      | Opis                                                     |
|---------|---------------------------------|-----------|----------------------------------------------------------|
| `GET`   | `/breeds`                       | publiczny | Lista aktywnych ras z filtrowaniem `?q=&group=&size=`    |
| `GET`   | `/breeds/{id}`                  | publiczny | Szczegoly rasy + globalne statystyki zlowien             |
| `GET`   | `/users/{userId}/pokedex`       | wymagany  | Odkryte rasy uzytkownika (malejaco po liczbie zlowien)   |
| `GET`   | `/users/{userId}/pokedex/stats` | wymagany  | Statystyki: postep, streak, ulubiona rasa, totale        |

### Parametry filtrowania `GET /breeds`

| Parametr | Typ    | Opis                                                     |
|----------|--------|----------------------------------------------------------|
| `q`      | String | Fragment nazwy angielskiej lub polskiej (case-insensitive)|
| `group`  | String | Grupa AKC/FCI: `Herding`, `Hound`, `Sporting`, ...       |
| `size`   | String | Rozmiar: `small`, `medium`, `large`                      |

---

## Opis kazdego elementu

### Breed (encja JPA)

Mapuje tabele `breed` z V1. Rasy sa zarzadzane wylacznie przez Flyway seed — nie ma endpointu
tworzenia/edycji ras. Encja jest tylko do odczytu z perspektywy API.

```java
@Entity
@Table(name = "breed")
public class Breed {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private String name;          // nazwa angielska
  private String namePl;        // nazwa polska
  @Column(name = "\"group\"")
  private String group;         // AKC/FCI group
  private String sizeCategory;  // "small" | "medium" | "large"
  private String description;
  private String silhouetteUrl;
  private short rarityScore;    // 1-5
  private boolean isActive;
}
```

Pole `group` wymaga cudzyslow w mapowaniu — `group` jest slowen kluczowym SQL.

---

### BreedRepository

JPQL query z dynamicznym filtrowaniem bez Specification API. Parametry `null` sa ignorowane
dzieki `CASE WHEN` w warunku `WHERE`:

```java
@Query("""
  SELECT b FROM Breed b
  WHERE b.isActive = true
    AND (:q IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(b.namePl) LIKE LOWER(CONCAT('%', :q, '%')))
    AND (:group IS NULL OR b.group = :group)
    AND (:sizeCategory IS NULL OR b.sizeCategory = :sizeCategory)
  ORDER BY b.rarityScore ASC, b.name ASC
  """)
List<Breed> findAllByFilter(String q, String group, String sizeCategory);
```

Sortowanie `rarityScore ASC, name ASC` — najpierw pospolite rasy (1), na koncu unikaty (5).

---

### DogCatch (encja JPA)

Pelna definicja encji, mimo ze `CatchController` i `CatchService` sa tematem Kroku 6. Definiujemy
encje w Kroku 5 aby `DogCatchRepository` mogl wykonywac zapytania dla Pokedeksu bez circular
dependency miedzy krokow.

Builder jest potrzebny zarowno w testach integracyjnych, jak i przez `CatchService` w Kroku 6:

```java
DogCatch catch = DogCatch.builder()
    .user(user)
    .breed(breed)
    .photoUrl("...")
    .thumbnailUrl("...")
    .isPublic(true)
    .build();
```

---

### DogCatchRepository — zapytania dla Pokedeksu

| Metoda                              | Zwraca                                              | Uzycie                         |
|-------------------------------------|-----------------------------------------------------|--------------------------------|
| `findBreedCatchCountsByUserId`      | `List<Object[]>` (breedId, count) DESC              | Lista wpisow Pokedeksu         |
| `findFavoriteBreedIdByUserId`       | `Integer` (ID rasy z max catchow)                   | Ulubiona rasa w statystykach   |
| `countCatchesByUserId`              | `long` (lacznie)                                    | `totalCatches` w statystykach  |
| `countUniqueBreedsByUserId`         | `long` (unikalnych ras)                             | `uniqueBreeds` oraz postep     |
| `countCatchersByBreedId`            | `long` (unikalnych uzytkownikow)                    | `totalCatchers` w GET /breeds/:id |
| `countTotalCatchesByBreedId`        | `long` (wszystkich zlowien)                         | `totalCatches` w GET /breeds/:id  |
| `findDistinctCatchDatesByUserId`    | `List<java.sql.Date>` posortowane DESC              | Obliczanie streak              |

---

### BreedService — logika Pokedeksu

**Filtrowanie list `getBreeds`:** Puste stringi sa normalizowane do `null` przed przekazaniem do
repozytorium — klient moze wyslac `?q=` (pusty parametr) i zachowanie jest identyczne jak brak parametru.

**Dostep do prywatnego profilu:** Sprawdzany w metodach `getPokedex` i `getPokedexStats` przez
`checkPokedexAccess`. Rzu\\ca `PokedexAccessDeniedException` gdy profil jest prywatny i requester
nie jest wlascicielem.

**Obliczanie streak:**

```java
private int calculateStreak(UUID userId) {
  List<LocalDate> dates = ...sorted DESC...
  if (mostRecent.isBefore(today.minusDays(1))) return 0;
  int streak = 1;
  LocalDate expected = mostRecent.minusDays(1);
  for each date:
    if date == expected: streak++; expected--;
    else: break;
  return streak;
}
```

Streak jest zerowany jezeli ostatni catch byl starszy niz wczoraj. Liczenie wstecz od najnowszego
dnia — ciaglosc musi byc nieprzerwana.

---

### Seed V3 — pokrycie grup AKC/FCI

Migracja `V3__seed_breeds_extended.sql` dodaje 65 ras podzielonych na grupy:

| Grupa         | Przykladowe rasy                              | Rarity  |
|---------------|-----------------------------------------------|---------|
| Sporting      | Cocker Spaniel, Vizsla, Weimaraner            | 1-3     |
| Hound         | Beagle, Greyhound, Rhodesian Ridgeback        | 1-4     |
| Working       | Rottweiler, Doberman, Bernese Mountain Dog    | 1-3     |
| Herding       | Australian Shepherd, Belgian Malinois, Corgi  | 1-3     |
| Terrier       | Yorkshire Terrier, Bull Terrier, Airedale     | 1-2     |
| Toy           | Chihuahua, Pomeranian, Cavalier KC Spaniel    | 1-2     |
| Non-Sporting  | Dalmatian, Chow Chow, Boston Terrier          | 1-3     |
| Rzadkie       | Basenji, Xoloitzcuintli, Kai Ken, Cirneco     | 4-5     |

Razem z V2 (10 ras): **~75 aktywnych ras** jest dostepnych po uruchomieniu aplikacji.

---

### GlobalExceptionHandler — nowe handlery

| Wyjatek                       | HTTP status  | ErrorCode   |
|-------------------------------|--------------|-------------|
| `BreedNotFoundException`      | 404 Not Found| `NOT_FOUND` |
| `PokedexAccessDeniedException`| 403 Forbidden| `FORBIDDEN` |

---

## Testy

### BreedIntegrationTest (17 testow)

| Metoda testowa                                                          | Co sprawdza                                                  |
|-------------------------------------------------------------------------|--------------------------------------------------------------|
| `getBreeds_withNoFilter_returnsAllActiveBreeds`                         | Bez filtra zwraca >10 ras, rarity 1-5                        |
| `getBreeds_withNameQuery_returnsMatchingBreeds`                         | Filtr `q=golden` -> Golden Retriever (1 wynik)               |
| `getBreeds_withNameQueryPolish_returnsMatchingBreeds`                   | Filtr po polskiej nazwie (`owczarek`) dziala                 |
| `getBreeds_withGroupFilter_returnsOnlyMatchingGroup`                    | Filtr `group=Herding` -> tylko rasy Herding                  |
| `getBreeds_withSizeFilter_returnsOnlyMatchingSize`                      | Filtr `size=small` -> tylko male rasy                        |
| `getBreeds_withBlankQuery_treatedAsNoFilter`                            | Pusty string = null = brak filtra                            |
| `getBreeds_withNonMatchingQuery_returnsEmptyList`                       | Brak dopasowania -> pusta lista                              |
| `getBreedById_withExistingBreed_returnsDetailWithStats`                 | Szczegoly zawieraja statystyki (0 zlowien na poczatku)       |
| `getBreedById_withNonExistentId_throwsBreedNotFoundException`           | Nieistnieje ID -> 404                                        |
| `getPokedex_withNoCatches_returnsEmptyList`                             | Nowy uzytkownik ma pusty Pokedex                             |
| `getPokedex_withCatches_returnsDiscoveredBreeds`                        | 2 catche tej samej rasy -> 1 wpis z catchCount=2             |
| `getPokedex_withMultipleBreeds_sortedByCatchCountDesc`                  | Posortowane malejaco po liczbie zlowien                      |
| `getPokedex_forPrivateProfile_withDifferentRequester_throwsPokedexAccessDeniedException` | Prywatny profil -> 403 dla obcego |
| `getPokedex_forPrivateProfile_asOwner_succeeds`                         | Wlasciciel moze czytac swoj prywatny Pokedex                 |
| `getPokedexStats_withNoCatches_returnsZeroedStats`                      | Brak zlowien -> zera, favoriteBreed null                     |
| `getPokedexStats_withCatches_returnsCorrectStats`                       | Statystyki sa poprawne po dodaniu catchow                    |
| `getPokedexStats_withNonExistentUser_throwsUserNotFoundException`        | Nieistniejacy uzytkownik -> 404                              |

---

## Weryfikacja

```bash
# Pelna weryfikacja
cd backend
mvn clean verify

# Tylko testy Kroku 5
mvn test -Dtest="BreedIntegrationTest"

# Wynik oczekiwany:
# Tests run: 63, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

---

## Decyzje architektoniczne

**`PokedexController` w pakiecie `breed/` zamiast `user/`** — kontroler odpowiada na
zapytania o rasy konkretnego uzytkownika, ale jego logika naleza do domeny Pokedeksu/ras.
Zrodlem prawdy jest `BreedService` — nie rozdzielamy jednej domeny na dwa pakiety.

**`DogCatch` zdefiniowany w Kroku 5 zamiast Kroku 6** — `DogCatchRepository` potrzebuje znac
encje `DogCatch` dla zapytan Pokedeksu. Alternatywne podejscie (native SQL stringu) bylob
kruche. Definiowanie calej encji z Builderem jest czystsze i nie wymaga refaktoryzacji w Kroku 6.

**Seed V3 jako osobna migracja (nie rozszerzenie V2)** — zgodnie z regula AI_RULES.md "nie
modyfikuj istniejacych migracji Flyway". V2 zostaje nienaruszony, V3 dokoncza pelny seed.

**Streak liczony z distinct dat** — unika liczenia wielokrotnych catchow tego samego dnia jako
oddzielnych dni. `DISTINCT CAST(caught_at AS DATE)` w native query zamiast agregacji w Javie —
wydajniejsze na duzych zbiorach danych.

**brak cache na `/breeds`** — rasy zmieniaja sie rzadko (tylko przez Flyway). W MVP przyjeto,
ze zapytanie do PostgreSQL z indeksowanym `isActive` jest wystarczajaco szybkie.
Cache Redis zostanie dodany w Kroku 7 przy okazji implementacji feed.
