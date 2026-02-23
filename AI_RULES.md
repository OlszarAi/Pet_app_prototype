# AI_RULES.md — Zasady dla AI przy projekcie PetsApp

> **Obowiązkowe.** Każdy prompt do AI (Copilot, Cursor, Gemini, ChatGPT itp.) musi zaczynać się od wskazania tego pliku jako kontekstu, lub bezpośrednio wklejać te zasady.

---

## 1. Ogólne zasady — zawsze

- **Pisz kod produkcyjny, nie prototypowy.** Każdy plik ma być gotowy do code review przez seniora.
- **Jeden plik = jedna odpowiedzialność.** Nie łącz logiki biznesowej, dostępu do danych i prezentacji w jednym pliku.
- **Brak magic strings i magic numbers.** Używaj stałych, enumów i konfiguracji.
- **Nazwy muszą mówić same za siebie.** Żadnych `temp`, `data2`, `stuff`, `helper` bez kontekstu.
- **Komentarze opisują DLACZEGO, nie CO.** Jeśli kod wymaga komentarza żeby rozumieć CO robi — przepisz go.
- **Nie duplikuj kodu.** Wyciągnij do wspólnej metody/komponentu zanim skopujesz linię drugi raz.
- **Błędy muszą być obsługiwane.** Żadnych pustych catch bloków, żadnego połykania wyjątków.
- **Nie generuj kodu z TODO / placeholder / FIXME** — albo implementujesz w pełni, albo pytasz o zakres.

---

## 2. Backend — Java / Spring Boot

### Architektura
- Pakiet per feature (`auth/`, `user/`, `catch_/`, `feed/`, ...) — `Controller → Service → Repository`.
- **Controller** — tylko routing, walidacja DTO wejściowych (`@Valid`), mapowanie na Response.
- **Service** — logika biznesowa, transakcje (`@Transactional`).
- **Repository** — tylko dostęp do bazy, zero logiki.
- **DTO** — oddzielne klasy na wejście (`CreateCatchRequest`) i wyjście (`CatchResponse`). Nigdy nie eksponuj encji JPA bezpośrednio.
- Mapowanie encja ↔ DTO — używaj statycznych metod `CatchResponse.from(DogCatch entity)` lub dedykowanego `CatchMapper`.

### Styl kodu
- Google Java Style + Checkstyle + Spotless (sprawdź konfigurację w `pom.xml`).
- `record` dla DTO gdzie to możliwe (Java 21).
- Immutable gdzie się da — `final` pola, brak setterów w encjach JPA (używaj konstruktora/buildera).
- `Optional<T>` zamiast returna `null`.
- Logowanie przez `SLF4J` — `@Slf4j`, bez `System.out.println`.

### Bezpieczeństwo (KRYTYCZNE)
- Każdy endpoint musi mieć jawnie określone uprawnienia w `SecurityConfig`.
- Nigdy nie wkładaj sekretów do kodu — tylko `application.yml` + env vars.
- Zawsze waliduj dane wejściowe przez `@Valid` + Bean Validation.
- Upload zdjęć: whitelist MIME types, max size, strip EXIF — bez wyjątków.
- SQL: tylko JPA/JPQL lub `@Query` z bind parameters — nigdy konkatenacja stringów.

### Testy
- Każda nowa metoda serwisu = test jednostkowy w JUnit 5 + Mockito.
- Każdy nowy endpoint = test integracyjny z Testcontainers (PostgreSQL + Redis).
- Nazwy testów: `methodName_scenario_expectedResult()` np. `register_withExistingEmail_throwsConflictException`.

### Response format
- Zawsze zwracaj `ApiResponse<T>` — wrapper z `success`, `data`, opcjonalnym `pagination`.
- Błędy przez `GlobalExceptionHandler` — nigdy bezpośrednio z kontrolera.
- Kody błędów z enumów `ErrorCode` — nigdy hard-coded stringi.

---

## 3. Frontend — React Native / TypeScript

### Architektura
- Expo Router dla nawigacji (`app/` directory).
- `services/` — tylko wywołania API przez `apiClient`. Żadnych `fetch` bezpośrednio w komponentach.
- `stores/` — tylko globalny stan Zustand. Stan lokalny → `useState` lub `useReducer`.
- `hooks/` — wyciągaj logikę z komponentów do hooków, jeśli to więcej niż 15 linii.
- `types/` — wspólne typy TypeScript. Importuj z jednego miejsca.

### Komponenty
- **Jeden komponent = jeden plik.** Małe komponenty (`< 150 linii`), duże rozbij.
- Props muszą być zawsze otypowane (interface lub type — nie `any`).
- Żadnego `any`. Żadnego `// @ts-ignore` bez komentarza wyjaśniającego.
- Używaj `NativeWind` klas do stylowania — nie twórz inline `style={{}}` dla złożonych layoutów.
- Memoizacja: `React.memo` dla komponentów listy (FlatList items), `useCallback` dla handlerów przekazywanych w dół.

### API / Data fetching
- Wszystkie zapytania przez TanStack Query (`useQuery`, `useMutation`).
- Żadnych bezpośrednich `await fetch()` w komponentach.
- Obsługa stanów: `isLoading`, `isError`, `data` — zawsze wszystkie trzy.
- Optymistyczne update'y (`onMutate`) dla lajków i komentarzy — nie czekaj na serwer.

### TypeScript
- `strict: true` w `tsconfig.json`.
- Typy generuj na podstawie API response lub wspólnych `types/` — nie duplikuj.
- DTO z backendu mają odpowiadające typy w `types/api.ts`.

### Testy
- `jest` + `@testing-library/react-native` dla komponentów.
- Mockuj serwisy, nie implementację HTTP.

---

## 4. Baza danych / Migracje

- **Tylko Flyway** — żadnych zmian schematu ręcznie ani przez `spring.jpa.hibernate.ddl-auto`.
- Pliki: `V{n}__{opis_snake_case}.sql`. Numer sekwencyjny, opis obowiązkowy.
- Migracje są `nieodwracalne` — zawsze przemyśl przed dodaniem `NOT NULL` bez DEFAULT.
- Nowa kolumna → zawsze `nullable` lub z `DEFAULT` w pierwszej migracji (bezpieczny deploy).
- Indeksy dodawaj w tej samej migracji co kolumna/tabela, na której działają.
- Seed data (`V2__seed_breeds.sql`, `V3__seed_achievements.sql`) — statyczne pliki, nie generuj dynamicznie.

---

## 5. Czego AI NIE MOŻE robić

- ❌ NIE Generować kodu z zakomentowanymi sekcjami "// TODO: implement this later"
- ❌ NIE Tworzyć pliku `Utils.java` lub `Helpers.ts` jako worka na wszystko
- ❌ NIE Wklejać logiki biznesowej do kontrolera/komponentu
- ❌ NIE Używać `var` w Javie gdzie typ jest nieoczywisty
- ❌ NIE Generować pustych implementacji interfejsów (stub bez treści)
- ❌ NIE Tworzyć pliku przekraczającego ~300 linii bez podziału na klasy/moduły
- ❌ NIE Zmieniać istniejącej migracji Flyway (tylko nowa wersja)
- ❌ NIE Hardkodować URL-i, portów, haseł czy kluczy API
- ❌ NIE Pomijać obsługi błędów (pustych catch, braku .isError w query)
- ❌ NIE Tworzyć nowego endpointu bez odpowiedniej autoryzacji w SecurityConfig

---

## 6. Jak pracować z AI (workflow)

1. **Daj kontekst.** Wklej opis zadania + wskaż pliki których dotyczy. AI nie zna całego projektu.
2. **Jeden task = jeden PR.** Nie każ AI pisać 5 ficzerów naraz.
3. **Sprawdź output.** Każdy wygenerowany plik przeczytaj przed commitem. AI się myli.
4. **Commit po każdym kroku.** Mały, opisowy commit message (conventional commits).
5. **Nie merguj bez review.** Min. 1 approval od innej osoby — nawet jeśli AI wygenerowało.
6. **Testy przed mergem.** Build musi przechodzić. Testy jednostkowe i integracyjne muszą być zielone.
7. **Opisz w PR co AI zrobiło.** Jeśli używasz AI do generowania kodu, zaznacz to w opisie PR.

---

## 7. Conventional Commits (obowiązkowy format)

```
feat(auth): add email verification flow
fix(catch): handle null breed_id in upload
test(user): add integration test for profile update
refactor(feed): extract scoring logic to FeedScoringService
chore(db): add V4__add_index_on_notification_user.sql
docs(api): update catch endpoint swagger annotations
```

Prefix: `feat|fix|test|refactor|chore|docs|style|ci`
Scope: (`auth|user|catch|feed|friend|notification|achievement|breed|report|infra`)

---

*Plik AI_RULES.md jest źródłem prawdy dla zespołu. Każda zmiana wymaga code review i zgody całego zespołu.*
