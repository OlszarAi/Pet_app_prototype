# PetsApp Mobile — Plan Implementacji

Styl: czarny motyw, full-screen vertical feed (TikTok / Instagram Reels).
Platforma: iOS pierwszoplanowo, Android równolegle.
Zasada: **jeden task = jeden PR = działający ekran lub feature. Im krótszy task, tym osoba go robi szybciej i sprawdza pull request.**

> Backend jest w pełni gotowy. Swagger UI dostępny na `http://localhost:8080/swagger-ui.html` po uruchomieniu `docker compose up -d`.

---

## 1. Stack technologiczny

| Warstwa | Biblioteka | Dlaczego |
|---|---|---|
| Routing | Expo Router v4 (file-based) | Zero konfiguracji, deep links, typowane ścieżki |
| Server state | TanStack Query v5 | Cache, deduplication requestów, infinite scroll, optimistic updates |
| Client state | Zustand v5 | Minimalistyczny, bez boilerplate, auth + UI state |
| HTTP | Axios | Interceptors (token attach, 401 refresh, retry) |
| Style | NativeWind v4 | Tailwind classes w RN, dark mode trivialny |
| Listy | FlashList (@shopify/flash-list) | 2-3x szybszy od FlatList, obowiązkowy dla feedu |
| Obrazy | expo-image | Agresywne caching, blurhash placeholder |
| Tokeny | expo-secure-store | Bezpieczny keychain/keystore dla refresh tokena |
| Animacje | react-native-reanimated v3 | Płynne 60/120fps animacje |

---

## 1b. Stylowanie z NativeWind — jak to działa

NativeWind to Tailwind CSS dla React Native. Zamiast pisać `StyleSheet.create({})`, piszesz klasy jak w HTML.

### Setup (jeden raz na początku Task 01)

**`tailwind.config.js`** — musi znać ścieżki do wszystkich plików z `className`:
```js
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./app/**/*.{tsx,ts}', './src/**/*.{tsx,ts}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        bg: '#000000',
        surface: '#1C1C1E',
        'surface-alt': '#2C2C2E',
        accent: '#FF3B5C',
        'accent-green': '#30D158',
        muted: '#8E8E93',
        placeholder: '#636366',
        border: '#38383A',
      },
    },
  },
};
```

**`global.css`** — jeden plik, importowany w root layout:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

**`babel.config.js`:**
```js
module.exports = function (api) {
  api.cache(true);
  return {
    presets: [
      ['babel-preset-expo', { jsxImportSource: 'nativewind' }],
    ],
    plugins: ['react-native-reanimated/plugin'],
  };
};
```

**`metro.config.js`:**
```js
const { getDefaultConfig } = require('expo/metro-config');
const { withNativeWind } = require('nativewind/metro');

const config = getDefaultConfig(__dirname);
module.exports = withNativeWind(config, { input: './global.css' });
```

**`app/_layout.tsx`** — importuj CSS tutaj, raz:
```tsx
import '../global.css';
```

**`src/types/nativewind-env.d.ts`** — żeby TypeScript nie krzyczał na `className`:
```ts
/// <reference types="nativewind/types" />
```

---

### Jak używać w komponentach

NativeWind dodaje prop `className` do wszystkich komponentów RN (`View`, `Text`, `TouchableOpacity` itd.):

```tsx
// app/(auth)/login.tsx
import { View, Text, TouchableOpacity } from 'react-native';

export default function LoginScreen() {
  return (
    <View className="flex-1 bg-bg px-6 justify-center">
      <Text className="text-white text-3xl font-bold mb-8">Zaloguj się</Text>
      
      <TouchableOpacity className="bg-accent rounded-xl py-4 items-center">
        <Text className="text-white font-semibold text-base">Zaloguj</Text>
      </TouchableOpacity>
    </View>
  );
}
```

### Kiedy `className`, kiedy `StyleSheet`

| Sytuacja | Użyj |
|---|---|
| Layout, kolory, padding, margin, border | `className` |
| Animowane wartości (Reanimated) | `style` z `useAnimatedStyle` |
| Dynamiczny kolor zależny od JS variable | `style={{ backgroundColor: color }}` |
| Shadow na iOS (nie obsługiwane przez TW/RN) | `StyleSheet.create` lub `style` |

**Zasada:** zacznij od `className`. Sięgaj po `style` tylko gdy `className` nie wystarczy.

### Dynamiczne klasy — jak to robić poprawnie

NativeWind kompiluje klasy w build time — **nie można budować nazw klas przez string concatenation**:

```tsx
// NIEPOPRAWNIE — NativeWind nie znajdzie tej klasy:
<View className={`bg-${isActive ? 'accent' : 'surface'}`} />

// POPRAWNIE — pełne nazwy klas muszą być widoczne w kodzie:
<View className={isActive ? 'bg-accent' : 'bg-surface'} />
```

### Reużywalne komponenty z wariantami

Zamiast zewnętrznej biblioteki, własny komponent z typed props:

```tsx
// src/components/ui/Button.tsx
interface ButtonProps {
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'outline' | 'ghost';
  disabled?: boolean;
}

const variantClasses = {
  primary: 'bg-accent',
  outline: 'bg-transparent border border-accent',
  ghost: 'bg-transparent',
};

export default function Button({ label, onPress, variant = 'primary', disabled }: ButtonProps) {
  return (
    <TouchableOpacity
      onPress={onPress}
      disabled={disabled}
      className={`rounded-xl py-4 items-center ${variantClasses[variant]} ${disabled ? 'opacity-40' : ''}`}
    >
      <Text className="text-white font-semibold text-base">{label}</Text>
    </TouchableOpacity>
  );
}
```

### Struktura każdego ekranu

Każdy ekran to **jeden plik** w `app/`. Logika jest w hooku, wygląd w pliku ekranu:

```tsx
// app/(tabs)/profile.tsx
import { View, Text, ScrollView } from 'react-native';
import { useCurrentUser } from '../../src/hooks/useUser';
import Avatar from '../../src/components/ui/Avatar';
import ScreenLoader from '../../src/components/ui/ScreenLoader';

export default function ProfileScreen() {
  const { data: user, isLoading } = useCurrentUser();

  if (isLoading) return <ScreenLoader />;

  return (
    <ScrollView className="flex-1 bg-bg">
      <View className="items-center pt-12 pb-6">
        <Avatar uri={user.avatarUrl} size={80} />
        <Text className="text-white text-xl font-bold mt-3">@{user.username}</Text>
        <Text className="text-muted text-sm mt-1">{user.bio}</Text>
      </View>

      <View className="flex-row justify-around py-4 border-t border-border">
        <StatItem label="Złapane" value={user.totalCatches} />
        <StatItem label="Rasy" value={user.uniqueBreeds} />
      </View>
    </ScrollView>
  );
}

function StatItem({ label, value }: { label: string; value: number }) {
  return (
    <View className="items-center">
      <Text className="text-white text-2xl font-bold">{value}</Text>
      <Text className="text-muted text-xs mt-1">{label}</Text>
    </View>
  );
}
```

`StatItem` to prywatny podkomponent — w tym samym pliku bo nigdzie nie jest reużywany. Gdy zaczniesz go używać w 2+ miejscach → wynieś do `src/components/`.

---

## 2. Struktura folderów

```
mobile/
├── app/                          # Expo Router — TYLKO route files, zero logiki
│   ├── _layout.tsx               # Root: providers (QueryClient, Auth gate)
│   ├── index.tsx                 # Redirect → (tabs)/feed lub (auth)/login
│   ├── (auth)/
│   │   ├── _layout.tsx
│   │   ├── login.tsx
│   │   ├── register.tsx
│   │   ├── verify-email.tsx
│   │   └── forgot-password.tsx
│   ├── (tabs)/
│   │   ├── _layout.tsx           # Bottom tab bar
│   │   ├── feed.tsx              # Publiczny feed (dostępny bez logowania)
│   │   ├── catches.tsx            # Złapany pies (wymaga auth) 
│   │   ├── pokedex.tsx           # Pokédex (wymaga auth)
│   │   └── profile.tsx           # Profil (wymaga auth)
│   ├── catch/
│   │   └── [id].tsx              # Szczegóły złapania
│   ├── user/
│   │   └── [id].tsx              # Profil innego usera
│   ├── settings.tsx
│   ├── notifications.tsx
│   └── achievements.tsx
│
├── src/
│   ├── api/                      # Warstwa API — TYLKO wywołania HTTP
│   │   ├── client.ts             # Axios instance + interceptors (token, 401)
│   │   ├── health.ts
│   │   ├── auth.ts
│   │   ├── feed.ts
│   │   ├── catches.ts
│   │   ├── users.ts
│   │   ├── breeds.ts
│   │   ├── friends.ts
│   │   └── notifications.ts
│   │
│   ├── hooks/                    # React Query hooks per feature
│   │   ├── useFeed.ts
│   │   ├── useCatch.ts
│   │   ├── useUser.ts
│   │   ├── useBreeds.ts
│   │   ├── useFriends.ts
│   │   └── useNotifications.ts
│   │
│   ├── stores/
│   │   ├── auth.store.ts         # Zustand: accessToken, user, login, logout
│   │   └── ui.store.ts           # Zustand: modals, toasts
│   │
│   ├── components/
│   │   ├── ui/                   # Atomy wielokrotnego użytku
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── Avatar.tsx
│   │   │   ├── Badge.tsx
│   │   │   └── ScreenLoader.tsx
│   │   ├── feed/
│   │   │   ├── FeedCard.tsx      # Jeden post w feedzie (full-screen)
│   │   │   └── FeedActions.tsx   # Like, komentarz, share (prawa kolumna)
│   │   ├── catch/
│   │   │   └── CatchThumbnail.tsx
│   │   └── auth/
│   │       └── SocialButton.tsx
│   │
│   ├── constants/
│   │   ├── colors.ts             # Paleta kolorów (dark theme)
│   │   ├── layout.ts             # SCREEN_WIDTH, SCREEN_HEIGHT, TAB_BAR_HEIGHT
│   │   └── queryKeys.ts          # React Query key factories
│   │
│   └── types/
│       └── api.ts                # Typy DTO z backendu (dokładne mapowanie)
│
├── global.css                    # NativeWind — @tailwind directives
├── tailwind.config.js
├── babel.config.js
├── metro.config.js
└── .env.local                    # EXPO_PUBLIC_API_URL=http://192.168.x.x:8080
```

---

## 3. Jak tworzyć nowy feature / ekran

### Przykład: ekran "Szczegóły rasy"

**Krok 1 — Typy** (jeśli nie ma w `src/types/api.ts`):
```ts
// src/types/api.ts
export interface BreedDetail {
  id: number;
  name: string;
  rarityScore: number;
  // ...
}
```

**Krok 2 — API function** (`src/api/breeds.ts`):
```ts
export const getBreed = (id: number) =>
  apiClient.get<ApiResponse<BreedDetail>>(`/breeds/${id}`).then(r => r.data.data);
```

**Krok 3 — React Query hook** (`src/hooks/useBreeds.ts`):
```ts
export const useBreed = (id: number) =>
  useQuery({ queryKey: queryKeys.breeds.detail(id), queryFn: () => getBreed(id), staleTime: 10 * 60 * 1000 });
```

**Krok 4 — Screen** (`app/breed/[id].tsx`):
```tsx
export default function BreedScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data, isLoading } = useBreed(Number(id));
  // render
}
```

**Reguła:** Jeden plik = jedna odpowiedzialność. Ekran nie dotyka axios bezpośrednio.

---

## 4. Zasady wydajności

### React Query — konfiguracja staleTime (zapobieganie przeciążeniu backendu)

| Dane | staleTime | Powód |
|---|---|---|
| Feed publiczny | 30 sekund | Odświeżany przez Redis co 15 min |
| Feed znajomych | 15 sekund | Bardziej dynamiczny |
| Trending | 60 sekund | Backend cache 5 min |
| Lista ras | 10 minut | Prawie statyczne |
| Profil usera | 2 minuty | Rzadko się zmienia |
| Powiadomienia | 30 sekund | Chcemy świeże, ale nie za często |
| Health check | 0 (bez cache) | Tylko na stronie dev/debug |

**Globalny retry:** `retry: 1` — bez nieskończonych pętli przy błędach 4xx.

### FlashList zamiast FlatList
```tsx
// ZAWSZE tak dla list z obrazkami:
<FlashList
  data={catches}
  renderItem={({ item }) => <FeedCard catch={item} />}
  estimatedItemSize={SCREEN_HEIGHT} // full-screen card
  keyExtractor={item => item.id}
/>
```

### expo-image zamiast Image
```tsx
// NIE: <Image source={{ uri: url }} />
// TAK:
<Image source={url} contentFit="cover" cachePolicy="memory-disk" />
```

### Optimistic updates dla lajków
```ts
// src/hooks/useCatch.ts
const likeMutation = useMutation({
  mutationFn: (id: string) => toggleLike(id),
  onMutate: async (id) => {
    await queryClient.cancelQueries({ queryKey: queryKeys.catches.detail(id) });
    const prev = queryClient.getQueryData(queryKeys.catches.detail(id));
    queryClient.setQueryData(queryKeys.catches.detail(id), old => ({
      ...old, liked: !old.liked, likeCount: old.liked ? old.likeCount - 1 : old.likeCount + 1
    }));
    return { prev };
  },
  onError: (_, id, ctx) => queryClient.setQueryData(queryKeys.catches.detail(id), ctx?.prev),
});
```

### Nie duplikuj requestów
React Query automatycznie deduplikuje — jeśli 3 komponenty używają `useQuery` z tym samym `queryKey`, jest **jeden** request. Nie potrzeba żadnego `fetchOnce` ani singletonów.

---

## 5. Paleta kolorów (dark theme TikTok/Reels)

```ts
// src/constants/colors.ts
export const colors = {
  bg: '#000000',          // tło całej apki
  surface: '#1C1C1E',     // karty, bottom sheet
  surfaceAlt: '#2C2C2E',  // inputy, bordered elementy
  accent: '#FF3B5C',      // lajki, CTA (różowo-czerwony)
  accentGreen: '#30D158', // sukcesy, streaki
  text: '#FFFFFF',
  textMuted: '#8E8E93',
  textPlaceholder: '#636366',
  border: '#38383A',
  tabBar: 'rgba(0,0,0,0.85)', // półprzezroczysty tab bar
};
```

---

## 6. API Client — jak działa token refresh

```
Request → attach Bearer header → 401? → refresh token → retry → 401 again? → logout
```

Axios interceptor obsługuje to automatycznie. Komponenty **nigdy** nie wiedzą o tokenach.

---

## 7. Auth flow w apce

```
App Start
  └── Brak refresh tokena w SecureStore → (auth)/login
  └── Jest refresh token → POST /auth/refresh
        └── Sukces → (tabs)/feed
        └── Błąd → (auth)/login
        
Feed jest dostępny publicznie (jeśli niezalogowany, wywołania API bez Bearer)
Catch/upload → wymaga auth → redirect do login z powrotem do catch
Profil własny → wymaga auth
```

---

## 8. Pełna lista tasków

### FAZA 1 — Infrastruktura (zrób to zanim napiszesz jakikolwiek ekran)

#### Task 01 — Setup: packages + API client + ekran Health
Cel: apka startuje, łączy się z backendem, `/api/v1/health` działa.

**Paczki do zainstalowania:**
```bash
npx expo install expo-router expo-secure-store expo-image expo-image-picker expo-camera expo-notifications
npx expo install @tanstack/react-query axios zustand @shopify/flash-list
npx expo install react-native-reanimated react-native-gesture-handler react-native-safe-area-context
npx expo install nativewind tailwindcss
```

**Pliki do stworzenia (w tej kolejności):**
1. `tailwind.config.js` — extend colors z palety z sekcji 5
2. `babel.config.js` — dodaj `nativewind/babel`
3. `metro.config.js` — dodaj `withNativeWind`
4. `global.css` — `@tailwind base/components/utilities`
5. `.env.local` — `EXPO_PUBLIC_API_URL=http://localhost:8080/api/v1` (symulator iOS) / IP sieci dla fizycznego urządzenia
6. `src/constants/colors.ts` — paleta z sekcji 5
7. `src/constants/layout.ts` — `SCREEN_WIDTH`, `SCREEN_HEIGHT`, `TAB_BAR_HEIGHT`
8. `src/constants/queryKeys.ts` — key factories dla każdego zasobu
9. `src/types/api.ts` — typy `ApiResponse<T>`, `PaginationMeta`, `AuthResponse`, `UserProfile` (mapowanie z backendu)
10. `src/api/client.ts` — Axios instance, interceptor token attach, interceptor 401 → refresh → retry
11. `src/api/health.ts` — jedna funkcja `getHealth()`
12. `src/hooks/useHealth.ts` — `useQuery` bez cache (`staleTime: 0`)
13. `src/stores/auth.store.ts` — Zustand: `accessToken`, `user`, `setTokens`, `logout`
14. `app/_layout.tsx` — `QueryClientProvider` + `SafeAreaProvider` + załadowanie tokenów z SecureStore
15. `app/index.tsx` — prosty redirect (na razie prosto na ekran health)
16. `app/health.tsx` — wyświetla status backendu, base URL, czas odpowiedzi

**Definition of Done:** uruchamiasz symulator, widzisz "Backend: OK" na czarnym tle.

---

#### Task 02 — Auth: login, rejestracja, weryfikacja email

**Flow do zaimplementowania** (backend już to obsługuje):
- `POST /auth/register` → backend wysyła kod na email
- `POST /auth/verify-email` → backend zwraca `{ accessToken, refreshToken, expiresIn }`
- `POST /auth/login` → zwraca `{ accessToken, refreshToken, expiresIn }`
- `POST /auth/refresh` → nowy access token

**Kontrakty API** (dokładne pola z backendu):
```ts
// POST /auth/register → ApiResponse<{ message: string }>
// POST /auth/verify-email → ApiResponse<AuthResponse>
// POST /auth/login → ApiResponse<AuthResponse>
// POST /auth/forgot-password → ApiResponse<{ message: string }>
// POST /auth/reset-password → ApiResponse<{ message: string }>

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;  // sekundy
}
```

**Pliki do stworzenia (w tej kolejności):**
1. `src/api/auth.ts` — funkcje: `register`, `verifyEmail`, `resendVerification`, `login`, `refreshToken`, `logout`, `forgotPassword`, `resetPassword`
2. `src/hooks/useAuth.ts` — mutacje: `useLogin`, `useRegister`, `useVerifyEmail`
3. `src/components/ui/Button.tsx` — reużywalny przycisk (variants: primary, outline, ghost)
4. `src/components/ui/Input.tsx` — reużywalny input (label, error message, secureTextEntry)
5. `src/components/ui/ScreenLoader.tsx` — fullscreen spinner
6. `app/(auth)/_layout.tsx` — layout dla ekranów auth (bez tab bar)
7. `app/(auth)/login.tsx`
8. `app/(auth)/register.tsx`
9. `app/(auth)/verify-email.tsx` — 6-cyfrowy kod wpisywany cyfrę po cyfrze (6 osobnych inputów)
10. `app/(auth)/forgot-password.tsx`
11. Aktualizacja `src/stores/auth.store.ts` — po login/verify zapisuj tokeny w SecureStore
12. Aktualizacja `app/index.tsx` — sprawdź SecureStore → jeśli jest token → `/health`, jeśli nie → `/login`

**Definition of Done:** możesz się zarejestrować nowym emailem, zweryfikować kod, zalogować, token trafia do SecureStore.

**Jak przetestować lokalnie:**
```bash
# backend musi być uruchomiony
docker compose up -d
# sprawdź logi emaila (MailHog dev UI)
open http://localhost:8025
```

---

### FAZA 2 — Shell Apki

- **Task 03** — Tab bar z 5 zakładkami (Feed, Catch, Pokédex, Znajomi, Profil), dark theme, ikony
- **Task 04** — Auth guard w root layout: protected tabs redirect do `(auth)/login`; po login wróć do poprzedniej trasy

### FAZA 3 — Feed (core feature)

- **Task 05** — Publiczny feed: `GET /feed/public`, FlashList z `estimatedItemSize={SCREEN_HEIGHT}`, paginacja cursorowa
- **Task 06** — FeedCard UI: full-screen zdjęcie, overlay z lewej (avatar, username, caption, breed tag), overlay z prawej (like count, comment count)
- **Task 07** — Like: `POST /catches/:id/like`, optimistic update w React Query, animacja serca (Reanimated)
- **Task 08** — Feed znajomych: `GET /feed/friends` (wymaga auth), chronologiczny
- **Task 09** — Trending feed: `GET /feed/trending`, 10 wyników, osobna zakładka nad feedem

### FAZA 4 — Catch / Upload

- **Task 10** — Ekran "Złap psa": `expo-image-picker`, wybór zdjęcia z galerii lub aparatu, preview
- **Task 11** — Formularz przed uploadem: wybór rasy (SearchableSelect z listy `GET /breeds`), caption, toggle `isPublic`
- **Task 12** — Upload: `POST /catches` multipart/form-data, progress bar, sukces → przekierowanie na nowy catch
- **Task 13** — Szczegóły catch: zdjęcie full, info o rasie, komentarze paginated, usunięcie własnego

### FAZA 5 — Pokédex / Rasy

- **Task 14** — Lista ras: `GET /breeds`, wyszukiwanie, filtr po grupie FCI, FlashList
- **Task 15** — Szczegóły rasy: sylwetka, rarity stars, liczba catcherów, `GET /breeds/:id`
- **Task 16** — Pokédex usera: `GET /users/me/pokedex` — zablokowane (sylwetka) vs odblokowane, % ukończenia

### FAZA 6 — Profil

- **Task 17** — Własny profil: `GET /users/me`, avatar, bio, statystyki (total_catches, unique_breeds, streak)
- **Task 18** — Grid zdjęć na profilu: `GET /users/me/catches`, 3 kolumny, paginacja
- **Task 19** — Edycja profilu: `PATCH /users/me`, zmiana avatara `POST /users/me/avatar`
- **Task 20** — Profil innego usera: `GET /users/:id`, `GET /users/:id/catches`
- **Task 21** — Wyszukiwanie userów: `GET /users/search?q=`, debounce 300ms

### FAZA 7 — Znajomi

- **Task 22** — Wyślij zaproszenie: `POST /friends/request`, przycisk na profilu
- **Task 23** — Lista zaproszeń: `GET /friends/requests`, accept `POST /friends/accept`, reject `POST /friends/reject`
- **Task 24** — Lista znajomych: `GET /friends`, leaderboard `GET /friends/leaderboard`

### FAZA 8 — Powiadomienia

- **Task 25** — Lista powiadomień: `GET /notifications`, badge na tab bar, `PATCH /notifications/read-all`
- **Task 26** — Rejestracja FCM token: `expo-notifications`, `POST /notifications/device-token`
- **Task 27** — Obsługa push w foreground (banner) + background (tap → deep link)

### FAZA 9 — Achievementy

- **Task 28** — Ekran achievementów: `GET /achievements`, odblokowane vs locked, progress bar
- **Task 29** — Popup po odblokowaniu: po upload catch sprawdź response na nowe achievementy, pokaż modal z animacją

### FAZA 10 — Ustawienia

- **Task 30** — Ekran ustawień: `GET/PATCH /users/me/settings` — push toggle, dark mode, prywatność
- **Task 31** — Zmiana hasła: `PATCH /users/me/password`
- **Task 32** — Usunięcie konta: `DELETE /users/me` z potwierdzeniem hasłem (wymóg App Store)

### FAZA 11 — Polish & QA

- **Task 33** — Skeleton loading screens dla feed, profilu, listy ras
- **Task 34** — Pull-to-refresh wszędzie gdzie ma sens (`onRefresh` w React Query)
- **Task 35** — Offline: brak internetu → informacja + retry button (nie biały ekran)
- **Task 36** — Deep links: `petsapp://catch/:id`, `petsapp://user/:id`
- **Task 37** — Testy E2E: rejestracja → upload → like (Detox lub MAAS)

---

## 9. Jak debugować połączenie z backendem

```bash
# Uruchom backend + sprawdź czy działa
cd /home/adam/coding/Pet_app_prototype
docker compose up -d
curl http://localhost:8080/api/v1/health

# W .env.local (plik nigdy nie jest commitowany):
EXPO_PUBLIC_API_URL=http://192.168.X.X:8080  # fizyczne urządzenie
EXPO_PUBLIC_API_URL=http://localhost:8080     # simulator iOS

# Swagger UI (dokumentacja API):
open http://localhost:8080/swagger-ui.html
```

---

## 10. Konwencje kodu

- Nazwy plików: `PascalCase.tsx` dla komponentów, `camelCase.ts` dla hooków/utility
- Eksporty: `export default` dla komponentów, named exports dla utility/hooks/types
- Typy: zawsze otypuj props przez `interface` lub `type`, nigdy `any`
- NativeWind: klasy zamiast `StyleSheet.create()` dla prostych layoutów
- `StyleSheet.create()` tylko gdy potrzebne dynamiczne wartości (np. animacje)
- Komentarze tylko gdy logika jest nieoczywista — nie opisuj co jest widoczne

---

## 11. Jak radzić sobie z dużą aplikacją

### Zasada: feature-first, nie layer-first
Każdy feature (auth, feed, catch, profil) ma swoje pliki rozrzucone po warstwach. Gdy otwierasz task "dodaj komentarze", wiesz dokładnie co dotknąć:
```
src/types/api.ts          → dodaj CommentResponse
src/api/catches.ts        → dodaj getComments(), postComment()
src/hooks/useCatch.ts     → dodaj useComments(), usePostComment()
src/components/catch/     → dodaj CommentList.tsx, CommentInput.tsx
app/catch/[id].tsx        → złóż w całość
```
Nigdy nie otwierasz 10 różnych folderów — liniowy flow w dół.

### Zasada: szukaj po query key, nie po nazwie pliku
Gdy coś nie działa z cache/danymi — zacznij od `queryKeys.ts`. Każde query ma zdefiniowany klucz:
```ts
export const queryKeys = {
  feed: {
    public: (cursor?: string) => ['feed', 'public', cursor] as const,
    friends: (cursor?: string) => ['feed', 'friends', cursor] as const,
    trending: () => ['feed', 'trending'] as const,
  },
  catches: {
    detail: (id: string) => ['catches', id] as const,
    byUser: (userId: string) => ['catches', 'user', userId] as const,
  },
  users: {
    me: () => ['users', 'me'] as const,
    profile: (id: string) => ['users', id] as const,
    search: (q: string) => ['users', 'search', q] as const,
  },
  breeds: {
    all: () => ['breeds'] as const,
    detail: (id: number) => ['breeds', id] as const,
  },
  notifications: {
    list: () => ['notifications'] as const,
  },
};
```

### Zasada: API client jest jedynym miejscem z axios
Nigdy nie importuj `axios` bezpośrednio do hooka ani komponentu. Tylko `src/api/client.ts` wie o HTTP.

### Zasada: komponent nie wie o auth
Token attach jest w interceptorze axios. Komponent wywołuje hook, hook wywołuje API function, API function wysyła request — token idzie automatycznie. Komponent jest ślepy na JWT.

### Jak znaleźć plik gdy coś nie działa
1. Problem z UI → `app/` lub `src/components/`
2. Problem z danymi/cache → `src/hooks/`
3. Problem z requestem HTTP → `src/api/`
4. Problem z typami → `src/types/api.ts`
5. Problem z tokenem/sesją → `src/stores/auth.store.ts` + `src/api/client.ts`
6. Problem z nawigacją → `app/_layout.tsx` lub `app/(tabs)/_layout.tsx`

### Jak dodać nowy feature — checklista
```
[ ] Typ w src/types/api.ts (jeśli nie istnieje)
[ ] Funkcja API w src/api/{feature}.ts
[ ] Query hook w src/hooks/use{Feature}.ts
[ ] Komponenty w src/components/{feature}/
[ ] Ekran w app/...
[ ] Query key w src/constants/queryKeys.ts
[ ] staleTime dopasowany do tabeli w sekcji 4
```

### Rozmiar pliku — kiedy dzielić
- Plik komponentu > 150 linii → wyciągnij podkomponenty
- Hook > 80 linii → podziel na mniejsze hooki
- API file > 60 linii → OK, to jest lista funkcji, nie trzeba dzielić
- Plik typów > 200 linii → rozdziel na `api.auth.ts`, `api.feed.ts` itd.

### iOS vs Android — na co uważać
- `SafeAreaView` zawsze zamiast `View` dla root ekranów (notch, Dynamic Island)
- `KeyboardAvoidingView` z `behavior="padding"` na iOS, `behavior="height"` na Android
- Ikony: `expo-vector-icons/Ionicons` (spójny zestaw na obu platformach)
- Haptics: `expo-haptics` — lekki feedback przy like, dodaniu znajomego
- Status bar: `expo-status-bar` z `style="light"` (ciemna apka, jasny tekst)
- Bottom sheet: `@gorhom/bottom-sheet` — natywna responsywność na obu platformach
