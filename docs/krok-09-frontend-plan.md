# Frontend Plan — PetsApp (Expo + React Native)

> Zastepuje stary `krok-09-frontend-setup.md`. Ten dokument jest zrodlem prawdy dla calego frontendu.
>
> Nadrzedna zasada: **jedna funkcjonalnosc = jeden modul = jeden katalog.** Kazdy ficer ma swoja strukture plików i nie zasmiecza cudzych modułow.

---

## Spis tresci

1. [Stos technologiczny i decyzje projektowe](#1-stos-technologiczny-i-decyzje-projektowe)
2. [System designu — styl graficzny](#2-system-designu--styl-graficzny)
3. [Architektura katalogow (feature-first)](#3-architektura-katalogow-feature-first)
4. [Konwencja stylowania](#4-konwencja-stylowania)
5. [Konwencja tworzenia nowego modulu](#5-konwencja-tworzenia-nowego-modulu)
6. [Plan budowy krok po kroku](#6-plan-budowy-krok-po-kroku)
   - [Krok F-01 — Inicjalizacja i design system](#krok-f-01--inicjalizacja-i-design-system)
   - [Krok F-02 — Shell nawigacji i publiczny feed](#krok-f-02--shell-nawigacji-i-publiczny-feed)
   - [Krok F-03 — Auth flow](#krok-f-03--auth-flow)
   - [Krok F-04 — Feed zalogowanego uzytkownika + znajomi](#krok-f-04--feed-zalogowanego-uzytkownika--znajomi)
   - [Krok F-05 — Zlapanie psa (kamera + upload)](#krok-f-05--zlapanie-psa-kamera--upload)
   - [Krok F-06 — Profil uzytkownika i Pokedex](#krok-f-06--profil-uzytkownika-i-pokedex)
   - [Krok F-07 — Znajomi i wyszukiwanie](#krok-f-07--znajomi-i-wyszukiwanie)
   - [Krok F-08 — Powiadomienia i achievementy](#krok-f-08--powiadomienia-i-achievementy)
   - [Krok F-09 — Ustawienia, dark mode, push](#krok-f-09--ustawienia-dark-mode-push)
7. [Srodowisko i uruchamianie](#7-srodowisko-i-uruchamianie)
8. [Najczestsze bledy i ich rozwiazania](#8-najczestsze-bledy-i-ich-rozwiazania)

---

## 1. Stos technologiczny i decyzje projektowe

### Podstawowe biblioteki

| Obszar | Biblioteka | Wersja | Uzasadnienie |
|---|---|---|---|
| Platforma | Expo SDK | 52 | Managed workflow — zero native config dla standardowych ficerów |
| Nawigacja | Expo Router | 4 | File-based routing jak Next.js — naturalny dla deweloperów JS |
| Stylowanie | NativeWind | 4 | Tailwind w RN — szybka iteracja, projektant i deweloper mówia tym samym jezykiem |
| Serwerowy stan | TanStack Query | 5 | Cache, invalidation, background refresh, optimistic updates |
| Globalny stan | Zustand | 5 | Minimalistyczny, bez boilerplate, TypeScript-native |
| HTTP | Axios | 1.7 | Interceptory, timeouty, FormData upload — pewniejszy niz fetch w RN |
| Formularze | react-hook-form + zod | — | Mniej rerenderów niz Formik, schema walidacja z automatycznymi typami TS |
| Listy | @shopify/flash-list | 1.7 | 10x szybszy niz FlatList dla long list (feed, pokedex) |
| Obrazy | expo-image | — | Lazy loading, pamiec podreczna, placeholdery BlurHash |
| Ikony | lucide-react-native | 0.475 | Spójna linia, 1000+ ikon, PNG jako fallback, TypeScript friendly |
| Animacje | react-native-reanimated + moti | — | Reanimated — silnik, Moti — deklaratywny API na jego bazie |
| Bottom sheet | @gorhom/bottom-sheet | 5 | Natywna gestura, snap points, lista wewnatrz sheeta |
| Toast | sonner-native | — | Port Sonner — jeden import, dziala z Reanimated |
| Tokeny | expo-secure-store | — | Szyfrowany keychain, nie AsyncStorage |
| Czcionki | expo-font + Geist | — | Geist (Vercel) — czysta, techniczna, swietna czytelnosc |
| Aparat | expo-camera | — | Expo managed, nie wymaga native build |
| Galeria | expo-image-picker | — | Spójne UX na iOS i Android |
| Haptics | expo-haptics | — | Feedback dotykowy na like, succesach |
| Srodowisko | expo-constants | — | Dostep do zmiennych przez `Constants.expoConfig` |

### Czego NIE uzywamy i dlaczego

| Biblioteka | Alternatywa | Powod odrzucenia |
|---|---|---|
| `styled-components` / `emotion` | NativeWind | Runtime overhead, gorszy DX w RN niz CSS klasy |
| `react-navigation` (raw) | Expo Router | Expo Router to nakładka na react-navigation z file-based API |
| `AsyncStorage` | `expo-secure-store` | Nieszyfrowany — nie dla tokenów |
| `FlatList` | `@shopify/flash-list` | Zwalnia przy 100+ elementach |
| `Formik` | `react-hook-form` | Wiecej rerenderów, wolniejszy |
| `moment.js` | `date-fns` | 41 KB vs 4 KB dla podstawowych funkcji |

---

## 2. System designu — styl graficzny

### Koncepcja wizualna

PetsApp to aplikacja spolecznosciowa dla milosnikow psow. Styl: **ciepla prostota** — organiczne ksztalty, ciepla paleta kolorow, duze zdjecia jako glówny element. Wzorzec: Duolingo + Instagram bez nadmiernej gamifikacji.

### Paleta kolorow

```ts
// src/design/tokens.ts

export const colors = {
  // Brand
  primary:   '#FF6B35',   // Pomelo — glówny kolor akcji (przyciski, aktywne taby)
  secondary: '#2EC4B6',   // Teal — rasy, achievementy
  accent:    '#FFB347',   // Amber — rarity stars, premium

  // Neutrals
  gray: {
    50:  '#FAFAFA',
    100: '#F5F5F5',
    200: '#E5E5E5',
    300: '#D4D4D4',
    400: '#A3A3A3',
    500: '#737373',
    600: '#525252',
    700: '#404040',
    800: '#262626',
    900: '#171717',
  },

  // Semantyczne
  success:  '#22C55E',
  warning:  '#F59E0B',
  error:    '#EF4444',
  info:     '#3B82F6',

  // Tlo
  background: '#FAFAFA',  // light mode
  surface:    '#FFFFFF',
  surfaceAlt: '#F5F5F5',

  // Dark mode — definiujemy od razu
  dark: {
    background: '#0A0A0A',
    surface:    '#171717',
    surfaceAlt: '#262626',
  },
} as const;
```

### Typografia

Czcionka: **Geist** (Vercel, open source). Dlaczego:
- Czytelna w malych rozmiarach (karty, podpisy)
- Spójna z nowoczesna estetyka techniczna
- Swietna alternatywa dla SF Pro / Roboto — wyroznia aplikacje

```ts
// src/design/typography.ts

export const typography = {
  fonts: {
    sans:  'Geist-Regular',
    sansMedium: 'Geist-Medium',
    sansBold:   'Geist-Bold',
    mono:  'GeistMono-Regular',
  },

  sizes: {
    xs:   11,
    sm:   13,
    base: 15,
    md:   17,
    lg:   20,
    xl:   24,
    '2xl': 28,
    '3xl': 34,
  },

  lineHeights: {
    tight:   1.2,
    normal:  1.4,
    relaxed: 1.6,
  },
} as const;
```

### Spacing i Border Radius

```ts
// src/design/spacing.ts

export const spacing = {
  1:  4,
  2:  8,
  3:  12,
  4:  16,
  5:  20,
  6:  24,
  8:  32,
  10: 40,
  12: 48,
  16: 64,
} as const;

export const radius = {
  sm:   8,
  md:   12,
  lg:   16,
  xl:   24,
  full: 9999,
} as const;
```

### Ikony — Lucide

Biblioteka: `lucide-react-native`. Kazda ikona jest importowana indywidualnie (tree shaking).

```tsx
// Przykład uzycia
import { Heart, MessageCircle, Home, Camera } from 'lucide-react-native';

// Zawsze uzywaj size i color przez props
<Heart size={20} color={colors.primary} strokeWidth={1.75} />
```

**Referencja ikon per ficer:**

| Ekran / Akcja | Ikona Lucide |
|---|---|
| Tab: Feed | `Home` |
| Tab: Znajomi | `Users` |
| Tab: Zlap! | `Camera` |
| Tab: Pokedex | `BookOpen` |
| Tab: Profil | `User` |
| Polub | `Heart` / `HeartOff` |
| Komentarz | `MessageCircle` |
| Udostepnij | `Share2` |
| Powiadomienia | `Bell` |
| Ustawienia | `Settings` |
| Szukaj | `Search` |
| Dodaj znajomego | `UserPlus` |
| Achievement | `Trophy` |
| Rare breed | `Star` |
| Lokalizacja | `MapPin` |
| Wyloguj | `LogOut` |
| Zamknij / wstecz | `X` / `ChevronLeft` |
| Edytuj | `Pencil` |
| Menu | `MoreHorizontal` |
| Blokada (prywatny profil) | `Lock` |
| Sprawdzono | `CheckCircle2` |

### Komponenty UI — biblioteka wewnetrzna

Nie uzywamy zewnetrznej biblioteki komponentow UI (NativeBase, Tamagui) — budujemy wlasna minimalna biblioteke w `src/components/ui/`. Daje to pelna kontrole i unika konfliktu stylów.

**Lista atomow do zbudowania w F-01:**

| Komponent | Opis |
|---|---|
| `Button` | variant: `primary`, `secondary`, `ghost`, `destructive`; size: `sm`, `md`, `lg`; loading state |
| `Input` | label, error, leftIcon, rightIcon, secureTextEntry |
| `Avatar` | src, size, fallback initials |
| `Badge` | variant: `default`, `success`, `warning`, `error`; + rarity (1-5 stars) |
| `Card` | padding, shadow, rounded — kontener dla kart |
| `Divider` | poziomy, opcjonalny label |
| `Skeleton` | placeholder loading dla kart |
| `EmptyState` | ikona + tytul + opis + opcjonalny CTA |
| `LoadingSpinner` | fullscreen lub inline |

---

## 3. Architektura katalogow (feature-first)

```
mobile/
├── app/                          # Expo Router — TYLKO routing i layout. Zero logiki biznesowej.
│   ├── _layout.tsx               # Root: czcionki, QueryClient, Toaster, theme
│   ├── index.tsx                 # Redirect na podstawie auth (patrz F-02)
│   ├── (public)/                 # Trasy dostepne BEZ logowania
│   │   ├── _layout.tsx           # Stack layout bez tab bar
│   │   └── feed.tsx              # Publiczny feed (entry point aplikacji)
│   ├── (auth)/
│   │   ├── _layout.tsx
│   │   ├── login.tsx
│   │   ├── register.tsx
│   │   ├── verify-email.tsx
│   │   └── forgot-password.tsx
│   ├── (tabs)/                   # Trasy PO zalogowaniu
│   │   ├── _layout.tsx           # Bottom tab bar
│   │   ├── feed.tsx
│   │   ├── friends.tsx
│   │   ├── catch.tsx
│   │   ├── pokedex.tsx
│   │   └── profile.tsx
│   ├── catch/
│   │   └── [id].tsx
│   ├── user/
│   │   └── [id].tsx
│   ├── breed/
│   │   └── [id].tsx
│   ├── notifications.tsx
│   ├── achievements.tsx
│   └── settings.tsx
│
├── src/
│   ├── design/                   # Design tokens (jednokrotna definicja, import wszedzie)
│   │   ├── tokens.ts             # colors, spacing, radius
│   │   ├── typography.ts
│   │   └── index.ts              # re-export wszystkiego
│   │
│   ├── components/
│   │   ├── ui/                   # Atomy (Button, Input, Avatar...) — BRAK zaleznosci od ficerów
│   │   │   ├── Button/
│   │   │   │   ├── Button.tsx
│   │   │   │   ├── Button.styles.ts    # StyleSheet.create tylko dla zlozonych styli
│   │   │   │   └── index.ts
│   │   │   ├── Input/
│   │   │   ├── Avatar/
│   │   │   ├── Badge/
│   │   │   ├── Card/
│   │   │   ├── Skeleton/
│   │   │   ├── EmptyState/
│   │   │   └── index.ts          # Barrel export: export { Button } from './Button'
│   │   │
│   │   └── layout/               # Komponenty layoutu (SafeArea wrappers, KeyboardAvoiding)
│   │       ├── Screen.tsx        # SafeAreaView + ScrollView wrapper
│   │       └── Header.tsx
│   │
│   ├── modules/                  # FEATURE MODULES — serce architektury
│   │   │
│   │   ├── feed/                 # Modul: feed
│   │   │   ├── components/
│   │   │   │   ├── CatchCard/
│   │   │   │   │   ├── CatchCard.tsx
│   │   │   │   │   ├── CatchCard.styles.ts
│   │   │   │   │   └── index.ts
│   │   │   │   ├── LikeButton/
│   │   │   │   ├── CommentList/
│   │   │   │   └── FeedList/      # FlashList wrapper
│   │   │   ├── hooks/
│   │   │   │   ├── usePublicFeed.ts
│   │   │   │   ├── useFriendsFeed.ts
│   │   │   │   └── useTrendingFeed.ts
│   │   │   ├── services/
│   │   │   │   └── feed.service.ts
│   │   │   ├── types/
│   │   │   │   └── feed.types.ts
│   │   │   └── index.ts
│   │   │
│   │   ├── auth/
│   │   │   ├── components/
│   │   │   │   ├── LoginForm/
│   │   │   │   ├── RegisterForm/
│   │   │   │   └── VerifyEmailForm/
│   │   │   ├── hooks/
│   │   │   │   └── useAuth.ts
│   │   │   ├── services/
│   │   │   │   └── auth.service.ts
│   │   │   ├── types/
│   │   │   │   └── auth.types.ts
│   │   │   └── index.ts
│   │   │
│   │   ├── catch/                # Modul: zdjecia psow
│   │   │   ├── components/
│   │   │   │   ├── CatchDetail/
│   │   │   │   ├── BreedSelector/
│   │   │   │   └── CaptureButton/
│   │   │   ├── hooks/
│   │   │   │   ├── useCatch.ts
│   │   │   │   └── useCamera.ts
│   │   │   ├── services/
│   │   │   │   └── catch.service.ts
│   │   │   ├── types/
│   │   │   │   └── catch.types.ts
│   │   │   └── index.ts
│   │   │
│   │   ├── user/
│   │   │   ├── components/
│   │   │   │   ├── ProfileHeader/
│   │   │   │   ├── CatchGrid/
│   │   │   │   └── StatsRow/
│   │   │   ├── hooks/
│   │   │   │   ├── useProfile.ts
│   │   │   │   └── useMe.ts
│   │   │   ├── services/
│   │   │   │   └── user.service.ts
│   │   │   ├── types/
│   │   │   │   └── user.types.ts
│   │   │   └── index.ts
│   │   │
│   │   ├── breed/
│   │   │   ├── components/
│   │   │   │   ├── BreedCard/
│   │   │   │   ├── BreedGrid/
│   │   │   │   └── PokedexProgress/
│   │   │   ├── hooks/
│   │   │   │   ├── useBreeds.ts
│   │   │   │   └── usePokedex.ts
│   │   │   ├── services/
│   │   │   │   └── breed.service.ts
│   │   │   ├── types/
│   │   │   │   └── breed.types.ts
│   │   │   └── index.ts
│   │   │
│   │   ├── friend/
│   │   │   ├── components/
│   │   │   │   ├── FriendCard/
│   │   │   │   ├── FriendSearch/
│   │   │   │   └── Leaderboard/
│   │   │   ├── hooks/
│   │   │   │   ├── useFriends.ts
│   │   │   │   └── useFriendRequests.ts
│   │   │   ├── services/
│   │   │   │   └── friend.service.ts
│   │   │   ├── types/
│   │   │   │   └── friend.types.ts
│   │   │   └── index.ts
│   │   │
│   │   ├── notification/
│   │   │   ├── components/
│   │   │   │   └── NotificationItem/
│   │   │   ├── hooks/
│   │   │   │   ├── useNotifications.ts
│   │   │   │   └── usePushPermission.ts
│   │   │   ├── services/
│   │   │   │   └── notification.service.ts
│   │   │   ├── types/
│   │   │   │   └── notification.types.ts
│   │   │   └── index.ts
│   │   │
│   │   └── achievement/
│   │       ├── components/
│   │       │   └── AchievementCard/
│   │       ├── hooks/
│   │       │   └── useAchievements.ts
│   │       ├── services/
│   │       │   └── achievement.service.ts
│   │       ├── types/
│   │       │   └── achievement.types.ts
│   │       └── index.ts
│   │
│   ├── api/                      # HTTP layer — wspolny dla wszystkich modulow
│   │   ├── client.ts             # Axios instance + interceptory auth + refresh
│   │   └── types.ts              # ApiResponse<T>, ApiError, PagedResponse<T>
│   │
│   ├── stores/                   # Globalny stan Zustand (NIE server state — to jest TanStack Query)
│   │   ├── auth.store.ts         # Tokeny, user zalogowany, isAuthenticated
│   │   └── ui.store.ts           # Theme, global loading
│   │
│   └── utils/
│       ├── date.ts               # format daty przez date-fns
│       ├── image.ts              # budowanie URL zdjec z backendu
│       ├── api-error.ts          # parseApiError, isConflictError
│       └── validators.ts         # Schemat Zod (reuzywalne) 
│
├── app.json
├── babel.config.js
├── metro.config.js
├── tailwind.config.js
├── tsconfig.json
└── global.css
```

---

## 4. Konwencja stylowania

### Zasada podstawowa

**NativeWind na pierwszym miejscu.** Uzywamy klas Tailwind do pozycjonowania, marginesow, paddingów, kolorow tekstu i tla, flex — wszedzie tam gdzie klasy wyrazaja intencje czytelnie.

`StyleSheet.create` (plik `.styles.ts`) dla:
- Skomplikowanych styli kart z wieloma cieniami i gradientami
- Wariantow komponentu (primary/secondary/ghost — logika stilów)
- Styli ktore zaleza od `Animated.Value` lub dynamicznych obliczen

```tsx
// Dobrze — NativeWind dla prostego layoutu
<View className="flex-1 bg-white px-4 pt-safe-offset-4">
  <Text className="text-xl font-bold text-gray-900">Tytul</Text>
</View>

// Dobrze — StyleSheet dla zlozonego komponentu karty
// CatchCard.styles.ts
import { StyleSheet } from 'react-native';
import { colors, radius, spacing } from '@/design';

export const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 3,
  },
  image: {
    width: '100%',
    aspectRatio: 4 / 3,
  },
});

// CatchCard.tsx — mieszamy oba podejscia
<View style={styles.card} className="mb-4">
  <Image style={styles.image} source={{ uri: photoUrl }} />
  <View className="p-4">
    <Text className="font-bold text-gray-900">@{username}</Text>
  </View>
</View>
```

### Konwencja plikow styli

Pliki `.styles.ts` zyja OBOK komponentu w tym samym katalogu:

```
CatchCard/
├── CatchCard.tsx         # Komponent
├── CatchCard.styles.ts   # StyleSheet — tylko jesli potrzebny
└── index.ts              # export { CatchCard } from './CatchCard'
```

### Dark mode

Uzywamy Tailwind `dark:` prefix przez NativeWind:

```tsx
<View className="bg-white dark:bg-gray-900">
  <Text className="text-gray-900 dark:text-white">Tekst</Text>
</View>
```

Aktywacja dark mode przez `useColorScheme()` z React Native — ustawiane w root `_layout.tsx` przez `colorScheme` prop na `<TailwindProvider>`.

---

## 5. Konwencja tworzenia nowego modulu

Krok po kroku jak dodac nowy ficer (przyklad: `report` — zglaszanie tresci).

### 1. Utwórz katalog modulu

```bash
mkdir -p src/modules/report/{components,hooks,services,types}
touch src/modules/report/index.ts
```

### 2. Zdefiniuj typy

```ts
// src/modules/report/types/report.types.ts

export type ReportReason =
  | 'INAPPROPRIATE_CONTENT'
  | 'SPAM'
  | 'WRONG_BREED'
  | 'FAKE_PHOTO';

export interface CreateReportRequest {
  catchId: string;
  reason: ReportReason;
  details?: string;
}
```

### 3. Napisz serwis

```ts
// src/modules/report/services/report.service.ts
import { apiClient } from '@/api/client';
import type { ApiResponse } from '@/api/types';
import type { CreateReportRequest } from '../types/report.types';

export const reportService = {
  createReport: async (body: CreateReportRequest): Promise<void> => {
    await apiClient.post<ApiResponse<void>>(`/catches/${body.catchId}/reports`, {
      reason: body.reason,
      details: body.details,
    });
  },
};
```

### 4. Napisz hook

```ts
// src/modules/report/hooks/useCreateReport.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { reportService } from '../services/report.service';
import { parseApiError } from '@/utils/api-error';

export const useCreateReport = () => {
  return useMutation({
    mutationFn: reportService.createReport,
    onError: (err) => {
      const parsed = parseApiError(err);
      console.error('Report failed:', parsed.message);
    },
  });
};
```

### 5. Zbuduj komponent

```
src/modules/report/components/ReportSheet/
├── ReportSheet.tsx
├── ReportSheet.styles.ts   (jesli potrzebny)
└── index.ts
```

### 6. Zeksportuj z modulu

```ts
// src/modules/report/index.ts
export { ReportSheet } from './components/ReportSheet';
export { useCreateReport } from './hooks/useCreateReport';
export type { CreateReportRequest, ReportReason } from './types/report.types';
```

### 7. Dodaj trasę (jesli potrzebna)

```tsx
// app/report/[catchId].tsx
import { ReportSheet } from '@/modules/report';
// ... logika nawigacji
```

### Zasady modulu

- Modul **nie importuje** z innych modulow bezposrednio. Jesli potrzebuje danych z innego modulu — przyjmuje je przez props lub uzywá wspólnych `@/api/types`.
- Modul eksportuje publiczne API przez `index.ts`. Reszta jest prywatna.
- Jeden hook = jedna odpowiedzialnosc. `usePublicFeed` nie obsługuje lajkow — do tego jest `useLike`.

---

## 6. Plan budowy krok po kroku

### Krok F-01 — Inicjalizacja i design system

**Cel:** Dzialajaca aplikacja Expo z prawidlowa konfiguracja i pelnym design tokenem. Zadnych ekranow funkcjonalnych.

#### Instalacja

```bash
npx create-expo-app mobile --template blank-typescript
cd mobile

# Nawigacja
npx expo install expo-router react-native-safe-area-context react-native-screens \
  expo-linking expo-constants expo-status-bar

# Animacje
npx expo install react-native-reanimated react-native-gesture-handler

# Stylowanie
npm install nativewind tailwindcss

# FlashList
npx expo install @shopify/flash-list

# Ikony
npm install lucide-react-native

# Czcionki
npx expo install expo-font @expo-google-fonts/geist

# Query + HTTP
npm install @tanstack/react-query axios

# Stan + SecureStore
npm install zustand
npx expo install expo-secure-store

# Formularze
npm install react-hook-form zod @hookform/resolvers

# Zdjecia
npx expo install expo-image expo-camera expo-image-picker expo-image-manipulator

# Animacje high-level
npm install moti

# Toast
npm install sonner-native

# Bottom sheet
npm install @gorhom/bottom-sheet

# Haptics
npx expo install expo-haptics

# Push
npx expo install expo-notifications expo-device

# OAuth
npx expo install expo-auth-session expo-web-browser expo-crypto

# Date
npm install date-fns

# TypeScript
npm install -D @types/react
```

#### Pliki konfiguracyjne do stworzenia

Wszystkie ponizej musza istniec po F-01:

- `babel.config.js` — reanimated plugin **musi byc ostatni**
- `tailwind.config.js` — `content`: `./app/**/*` + `./src/**/*`
- `metro.config.js` — NativeWind wrapper
- `global.css` — `@tailwind base/components/utilities`
- `nativewind-env.d.ts` — `/// <reference types="nativewind/types" />`
- `tsconfig.json` — `strict: true`, `"@/*": ["./src/*"]`

```json
// tsconfig.json
{
  "extends": "expo/tsconfig.base",
  "compilerOptions": {
    "strict": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

```json
// app.json (fragment wymagany)
{
  "expo": {
    "scheme": "petsapp",
    "plugins": ["expo-router", "expo-secure-store"],
    "experiments": { "typedRoutes": true }
  }
}
```

#### Design tokens do stworzenia

- `src/design/tokens.ts` — kolory (z sekcji 2)
- `src/design/typography.ts` — typy
- `src/design/spacing.ts` — spacingi
- `src/design/index.ts` — re-export

#### Komponenty UI do zbudowania

Wszystkie atomy z sekcji 2: `Button`, `Input`, `Avatar`, `Badge`, `Card`, `Skeleton`, `EmptyState`, `LoadingSpinner`.

#### Weryfikacja F-01

```bash
npx expo-doctor        # wszystkie checks PASS
npx tsc --noEmit       # zero bledów TS
npx expo start         # startuje bez bledów
```

---

### Krok F-02 — Shell nawigacji i publiczny feed

**Cel:** Uzytkownik otwiera aplikacje i widzi publiczny feed (lista kart ze zdjeciami psow) BEZ potrzeby logowania. Feed jest prawdziwym GET /feed/public z backendu.

#### Architektura nawigacji

```
app/
├── _layout.tsx       # Root: QueryClientProvider, Toaster, auth guard
├── index.tsx         # Redirect: zalogowany → (tabs)/feed, niezalogowany → (public)/feed
├── (public)/
│   ├── _layout.tsx   # Stack bez tab bar, z przyciskiem "Zaloguj sie" w naglowku
│   └── feed.tsx      # Publiczny feed
└── (tabs)/
    ├── _layout.tsx   # Tab bar
    └── feed.tsx      # Feed zalogowanego (ten sam modul, inne parametry)
```

#### Auth guard w root `_layout.tsx`

Root layout laduje tokeny ze SecureStore przy starcie. Nie redirectuje az `isLoaded === true` — eliminuje miganie ekranow.

```tsx
// app/_layout.tsx
import { useEffect, useState } from 'react';
import { Stack, useRouter, useSegments } from 'expo-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { Toaster } from 'sonner-native';
import { useAuthStore } from '@/stores/auth.store';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 5 * 60 * 1000, retry: 2 },
  },
});

function AuthGuard() {
  const { isAuthenticated, loadFromStorage } = useAuthStore();
  const [isLoaded, setIsLoaded] = useState(false);
  const segments = useSegments();
  const router = useRouter();

  useEffect(() => {
    loadFromStorage().finally(() => setIsLoaded(true));
  }, []);

  useEffect(() => {
    if (!isLoaded) return;

    const inAuthGroup   = segments[0] === '(auth)';
    const inPublicGroup = segments[0] === '(public)';
    const inTabsGroup   = segments[0] === '(tabs)';

    if (isAuthenticated && (inAuthGroup || inPublicGroup)) {
      router.replace('/(tabs)/feed');
    }
    // Niezalogowany moze byc w (public) — nie redirectujemy
    // Niezalogowany probujacy wejsc na (tabs) — redirectujemy na (auth)
    if (!isAuthenticated && inTabsGroup) {
      router.replace('/(auth)/login');
    }
  }, [isAuthenticated, segments, isLoaded]);

  return null;
}

export default function RootLayout() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <QueryClientProvider client={queryClient}>
        <AuthGuard />
        <Stack screenOptions={{ headerShown: false }} />
        <Toaster />
      </QueryClientProvider>
    </GestureHandlerRootView>
  );
}
```

#### `app/index.tsx` — redirect

```tsx
import { Redirect } from 'expo-router';
import { useAuthStore } from '@/stores/auth.store';

export default function Index() {
  const { isAuthenticated } = useAuthStore();
  return <Redirect href={isAuthenticated ? '/(tabs)/feed' : '/(public)/feed'} />;
}
```

#### Publiczny feed

Ekran (`app/(public)/feed.tsx`) tylko deleguje do komponentu modulu:

```tsx
// app/(public)/feed.tsx
import { PublicFeedScreen } from '@/modules/feed';
export default PublicFeedScreen;
```

Modul `feed` dostarcza gotowy ekran z hookiem `usePublicFeed`:

```ts
// src/modules/feed/hooks/usePublicFeed.ts
import { useInfiniteQuery } from '@tanstack/react-query';
import { feedService } from '../services/feed.service';

export const usePublicFeed = () =>
  useInfiniteQuery({
    queryKey: ['feed', 'public'],
    queryFn: ({ pageParam }) => feedService.getPublicFeed({ cursor: pageParam }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.pagination?.hasMore ? lastPage.pagination.cursor : undefined,
  });
```

#### CatchCard — karta zdjecia na feedzie

Karta zajmuje pelna szerokosc ekranu. Proporcje zdjecia: 4:3.

```
+----------------------------------+
| [Avatar] @username   [...]       |  <- naglowek karty
+----------------------------------+
|                                  |
|           ZDJECIE PSA            |  <- aspektRatio 4:3, expo-image (cache + blur placeholder)
|                                  |
+----------------------------------+
| [Heart 12] 47  [Msg 12] 8        |  <- akcje: like, komentarz
| Golden Retriever  ★★★★☆ (rare 4) |  <- rasa + rarity badge
| "Spotkany w parku Jordana"       |  <- caption (max 2 linie, expandable)
| 2 godziny temu                   |  <- czas relatywny przez date-fns
+----------------------------------+
```

Niezalogowany uzytkownik widzi feed ale:
- Przycisk "Like" pokazuje modal "Zaloguj sie aby polubyc"
- "Skomentuj" → redirect do login
- W naglowku tab bar NIE jest widoczny (publiczny layout nie ma tab bar)
- W prawym gornym rogu: przycisk "Zaloguj sie / Zarejestruj sie"

#### Weryfikacja F-02

- Feed laduje sie bez logowania
- Scroll do konca laduje kolejna strone (infinite scroll)
- Brak wifi — widoczny EmptyState z przyciskiem "Spróbuj ponownie"
- Skeleton widoczny przez czas ladowania pierwszej strony

---

### Krok F-03 — Auth flow

**Cel:** Pelna sciezka rejestracja → weryfikacja emaila → login → wylogowanie.

#### Ekrany

| Ekran | Sciezka | Opis |
|---|---|---|
| Login | `/(auth)/login` | Email + haslo, przycisk Google OAuth, link do rejestracji |
| Rejestracja | `/(auth)/register` | Email + username + haslo z strength indicator |
| Weryfikacja emaila | `/(auth)/verify-email` | 6 cyfr OTP input, resend link |
| Reset hasla | `/(auth)/forgot-password` | Email → link do resetu |

#### Walidacja formularzy przez Zod

```ts
// src/modules/auth/types/auth.types.ts
import { z } from 'zod';

export const loginSchema = z.object({
  email:    z.string().email('Nieprawidlowy email'),
  password: z.string().min(8, 'Min. 8 znakow'),
});

export const registerSchema = z.object({
  email:    z.string().email('Nieprawidlowy email'),
  username: z.string()
    .min(3, 'Min. 3 znaki')
    .max(30, 'Max 30 znakow')
    .regex(/^[a-z0-9_]+$/, 'Tylko male litery, cyfry i _'),
  password: z.string()
    .min(8, 'Min. 8 znakow')
    .regex(/[A-Z]/, 'Wymagana wielka litera')
    .regex(/[0-9]/, 'Wymagana cyfra'),
});

export type LoginFormValues    = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
```

#### Weryfikacja F-03

- Rejestracja dziala end-to-end (email w MailHog → kod → zalogowany)
- Bledy backendu (email zajety, zly kod) wyswietlaja sie pod polem (nie tylko Alert)
- Po zalogowaniu auth guard przekierowuje na `/(tabs)/feed`
- Wylogowanie czysci token i przekierowuje na `/(public)/feed`

---

### Krok F-04 — Feed zalogowanego uzytkownika + znajomi

**Cel:** Zalogowany uzytkownik widzi dwa taby w obrebie ekranu Feed: "Dla Ciebie" (publiczny + scoring) i "Znajomi" (chronologiczny). Dziala polubienie.

#### Layout zakkadek wewnatrz ekranu Feed

```
+----------------------------------+
|  PetsApp        [bell] [search]  |  <- naglowek z powiadomieniami
+----------------------------------+
| [ Dla Ciebie ] | [ Znajomi ]     |  <- horizontal tab switcher (nie router tabs)
+----------------------------------+
|                                  |
|           FEED LISTA             |  <- FlashList z CatchCard
|                                  |
```

Wewnetrzne taby (`Dla Ciebie` | `Znajomi`) realizujemy przez `react-native-pager-view` (platforma nativa) albo prosty `useState` + dwa FlashListy (prostsze, wystarczajace).

#### Optymistyczne polubienie

```ts
// src/modules/feed/hooks/useLike.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { feedService } from '../services/feed.service';

export const useLike = (catchId: string) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ liked }: { liked: boolean }) =>
      liked ? feedService.unlike(catchId) : feedService.like(catchId),

    // Optimistic update — nie czekamy na serwer
    onMutate: async ({ liked }) => {
      await queryClient.cancelQueries({ queryKey: ['feed'] });
      // snapshot poprzedniego stanu
      const snapshot = queryClient.getQueryData(['feed', 'public']);
      // aktualizacja w cache
      queryClient.setQueriesData({ queryKey: ['feed'] }, (old: any) =>
        patchCatchLike(old, catchId, !liked),
      );
      return { snapshot };
    },

    onError: (_err, _vars, context) => {
      // rollback jesli blad
      queryClient.setQueryData(['feed', 'public'], context?.snapshot);
    },
  });
};
```

#### Weryfikacja F-04

- Podwójny tap na zdjecie = polubienie (z wiggle animacja serca przez Moti)
- Lajk pojawia sie natychmiast (optimistic), nawet na wolnym polaczeniu
- Feed znajomych pusty dla nowego konta → EmptyState z CTA "Znajdz znajomych"

---

### Krok F-05 — Zlapanie psa (kamera + upload)

**Cel:** Uzytkownik moze zrobic zdjecie lub wybrac z galerii, wybrac rase i opublikowac.

#### Flow

```
Tab "Zlap!" otwiera bottom sheet (nie nowy ekran)
  → Wybierz: [Aparat] lub [Galeria]
     ↓
  Podglad zdjecia + przyciski: [Ponów] [Dalej →]
     ↓
  Formularz: [Wybierz rase ▼] [Podpis (opcjonalny)] [Publiczne Toggle]
     ↓
  Wyslij → progress bar uploadu → sukces toast → redirect na profil
```

#### Wybor rasy

Bottom sheet z `@gorhom/bottom-sheet` zawierajacy:
- Pole wyszukiwania (debounced, filtruje lokalnie)
- Lista ras posortowana po: ulubione (jesli zalogowany), nastepnie alfabetycznie
- Kazda rasa: nazwa + badge z `★` rarity (1-5)

#### Upload z progress

```ts
// src/modules/catch/services/catch.service.ts
import { apiClient } from '@/api/client';

export const catchService = {
  create: async (
    params: { photo: string; breedId: string; caption?: string; isPublic: boolean },
    onProgress?: (pct: number) => void,
  ) => {
    const form = new FormData();
    form.append('photo', {
      uri: params.photo,
      name: 'photo.jpg',
      type: 'image/jpeg',
    } as any);
    form.append('breedId', params.breedId);
    if (params.caption) form.append('caption', params.caption);
    form.append('isPublic', String(params.isPublic));

    return apiClient.post('/catches', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (e.total) onProgress?.(Math.round((e.loaded / e.total) * 100));
      },
    });
  },
};
```

#### Weryfikacja F-05

- Zdjecie z aparatu i galerii dziala na iOS i Android
- Upload z progress barem
- Po sukcesie — haptic feedback (`expo-haptics`) + toast + nowy catch widoczny na feedzie po refresh

---

### Krok F-06 — Profil uzytkownika i Pokedex

**Cel:** Wlasny profil z siatka zdjeciem, statystykami, edycja bio/avatara. Podglad profilu innego uzytkownika. Pokedex z odblokowanymi rasami.

#### Layout profilu

```
+----------------------------------+
| [<] @username          [...]     |
+----------------------------------+
| [Avatar 80px]  12   8            |
|  Imie          zlap rasy         |
|  bio bio bio ...                 |
| [Edytuj profil] / [Dodaj znajom] |
+----------------------------------+
| [Zdjecia] | [Pokedex]            |  <- taby wewnatrz profilu
+----------------------------------+
| [img][img][img]                  |  <- 3-kolumnowa siatka
| [img][img][img]                  |
```

#### Pokedex

Lista ras poslotowana: odkryte (duze, kolorowe) → nieodkryte (male, szare, rozmyte). Dla kazdej odkrytej: ile razy zlapana, kiedy pierwszy raz.

**Progress bar:** `X / 50 ras odkrytych` + animowany pasek.

#### Weryfikacja F-06

- Edycja bio dziala (PATCH /users/me)
- Upload avatara dziala (POST /users/me/avatar)
- Pokedex pokazuje odkryte rasy po uploadzie nowego zdjecia (query invalidation)

---

### Krok F-07 — Znajomi i wyszukiwanie

**Cel:** Wyszukiwanie userow, wysylanie zaproszen, akceptowanie, blokowanie. Leaderboard znajomych.

#### Ekran znajomych — 3 seklcje

1. **Oczekujace zaproszenia** (jesli > 0 — baner z iloscia)
2. **Leaderboard** — ranking znajomych po unique_breeds (top 10, podswietlony aktualny user)
3. **Lista znajomych** — avatar + username + unique_breeds

#### Wyszukiwanie

Dedykowane pole w naglowku z debounce 300ms. Wyniki: lista userow z przyciskiem "Dodaj" / "Oczekuje" / "Juz znajomy".

#### Weryfikacja F-07

- Zaproszenie wysłane przez user A widoczne u user B bez restartu (query polling lub manualne pull-to-refresh)
- Blokowanie usuwa usera z listy wynikow wyszukiwania

---

### Krok F-08 — Powiadomienia i achievementy

**Cel:** Lista powiadomien in-app z badzem, achievementy z animacja odblokowania.

#### Powiadomienia

- Badz na ikonie dzwonka w naglowku (nieodczytane)
- Infinite scroll przez cursor pagination
- Klikniecie → nawiguje do odpowiedniego zasobu (catch, user)
- "Oznacz wszystkie jako przeczytane" — jeden przycisk

#### Achievementy

Ekran `/achievements`:
- Odblokowane: duza karta z ikonka + data odblokowania
- Nieodblokowane: rozmyta karta z "???" i wskazówka postępu (`Zlap 5 ras - masz 3/5`)
- Animacja konfetti (przez Moti) gdy po restarcie aplikacji jest nowe odblokowane

#### Rejestracja tokena push

```ts
// src/modules/notification/hooks/usePushPermission.ts
import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import { useEffect } from 'react';
import { notificationService } from '../services/notification.service';

export const usePushPermission = () => {
  useEffect(() => {
    if (!Device.isDevice) return;  // nie na emulatorze

    (async () => {
      const { status } = await Notifications.requestPermissionsAsync();
      if (status !== 'granted') return;

      const token = (await Notifications.getExpoPushTokenAsync()).data;
      await notificationService.registerDevice(token);
    })();
  }, []);
};
```

Pytamy o pozwolenie push **po pierwszym sukcesie ulozenia psa** — nie przy starcie aplikacji.

---

### Krok F-09 — Ustawienia, dark mode, push

**Cel:** Ekran ustawien z przelacznikami, persystencja dark mode, synchronizacja z backendem.

#### Ustawienia

| Sekcja | Element | Backend endpoint |
|---|---|---|
| Konto | Zmień haslo | PATCH /users/me/password |
| Konto | Usun konto | DELETE /users/me |
| Prywatnosc | Profil prywatny | PATCH /users/me/settings |
| Powiadomienia | Push on/off | PATCH /users/me/settings |
| Wygląd | Dark mode | Lokalnie (ui.store.ts) |
| Sesja | Wyloguj sie | POST /auth/logout |

Dark mode zapisywany w `expo-secure-store` (persystencja miedzy sesjami) oraz w `ui.store.ts`.

---

## 7. Srodowisko i uruchamianie

### Zmienne srodowiskowe

```bash
# mobile/.env.local  (nie commituj — jest w .gitignore)
EXPO_PUBLIC_API_URL=http://192.168.1.x:8080/api/v1   # fizyczny telefon
# EXPO_PUBLIC_API_URL=http://10.0.2.2:8080/api/v1    # Android emulator
# EXPO_PUBLIC_API_URL=http://localhost:8080/api/v1    # iOS simulator
```

```ts
// src/api/client.ts — odczyt
import Constants from 'expo-constants';

const API_URL =
  process.env.EXPO_PUBLIC_API_URL ??
  Constants.expoConfig?.extra?.apiUrl ??
  'http://localhost:8080/api/v1';
```

### Uruchamianie

```bash
cd mobile

# Development
npx expo start

# iOS simulator
npx expo start --ios

# Android emulator
npx expo start --android

# Sprawdz typescript
npx tsc --noEmit

# Linter
npx eslint src/ app/ --ext .ts,.tsx
```

### Prebuild (natywne moduły poza Expo Go)

Gdy pojawi sie modul wymagajacy natywnego kodu (np. `react-native-maps`):

```bash
npx expo prebuild --clean
# iOS
npx expo run:ios
# Android
npx expo run:android
```

### Build produkcyjny przez EAS

```bash
# Instalacja EAS CLI
npm install -g eas-cli
eas login

# Konfiguracja (jednorazowo)
eas build:configure

# Build testowy (Internal Distribution)
eas build --platform all --profile preview

# Build produkcyjny
eas build --platform all --profile production

# Submit do store
eas submit --platform ios
eas submit --platform android
```

---

## 8. Najczestsze bledy i ich rozwiazania

| Problem | Przyczyna | Rozwiazanie |
|---|---|---|
| `Network Error` na telefonie | `localhost` nie wskazuje na komputer | Ustaw `EXPO_PUBLIC_API_URL` na IP komputera w sieci |
| `CORS blocked` | Backend odrzuca origin | Sprawdz `CorsConfig` w backendzie — `addAllowedOriginPattern("*")` na dev |
| Pętla redirect auth | `isAuthenticated` przed zaladowaniem storage | Dodaj `isLoaded` — nie redirectuj dopoki `loadFromStorage()` nie zakonczy |
| `Module not found @/*` | Brak path alias | Dodaj `"@/*": ["./src/*"]` do `tsconfig.json` |
| NativeWind klasy nie dzialaja | Brak importu `global.css` | `import '../global.css'` w root `_layout.tsx` |
| FlashList blank screen | Brak `estimatedItemSize` | Dodaj `estimatedItemSize={200}` do `<FlashList>` |
| Reanimated crash | Plugin nie jest ostatni w babel | `'react-native-reanimated/plugin'` musi byc ostatni w `plugins[]` |
| `expo-camera` crash w Expo Go | Expo Go nie ma custom native modules | Uzyj `npx expo run:ios` lub `run:android` zamiast Expo Go |
| Optimistic update nie dziala | Zly queryKey przy `setQueriesData` | Uzyj dokladnie tego samego klucza co w `useQuery` |
| Upload timeout na slow network | Domyslny 10s timeout zbyt krotki | Ustaw `timeout: 60_000` tylko dla endpointu `/catches` |
