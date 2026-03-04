# Krok 9 — Frontend: Setup (Expo + TypeScript + nawigacja + API client)

**Cel:** Działająca aplikacja Expo z nawigacją, globalnym stanem, klientem API i połączeniem z backendem. Po tym kroku deweloper może zalogować się przez aplikację mobilną, zobaczyć własny profil i wyjść.

---

## Wymagania wstępne

Przed startem backend musi działać lokalnie:

```bash
docker compose up -d
# PostgreSQL :5432, Redis :6379, MailHog :8025, MinIO :9000
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Serwer słucha na http://localhost:8080/api/v1
```

Sprawdź że backend żyje:
```bash
curl http://localhost:8080/api/v1/health
# {"success":true,"data":{"status":"UP"}}
```

---

## Środowisko deweloperskie

### Node.js + narzędzia

```bash
# wymagane: Node.js >= 20 LTS
node -v   # v20.x lub v22.x

# package manager — używamy npm (spójność z CRA tooling)
npm -v    # >= 10

# Expo CLI globalnie
npm install -g expo-cli eas-cli

# Dla iOS (tylko macOS):
sudo gem install cocoapods
xcode-select --install

# Dla Android: Android Studio + SDK 34
# Upewnij się że ANDROID_HOME jest ustawiony
echo $ANDROID_HOME   # np. /Users/adam/Library/Android/sdk
```

### Uruchomienie na telefonie (najszybsza droga)

Zainstaluj **Expo Go** na swoim telefonie:
- iOS: [App Store](https://apps.apple.com/app/expo-go/id982107779)
- Android: [Google Play](https://play.google.com/store/apps/details?id=host.exp.exponent)

Telefon i komputer muszą być w **tej samej sieci WiFi**.

---

## Krok 9.1 — Inicjalizacja projektu Expo

```bash
# Z katalogu głównego repozytorium
cd /home/adam/coding/Pet_app_prototype

npx create-expo-app mobile --template blank-typescript
cd mobile

# Sprawdź że startuje
npx expo start
# → Skanuj QR kodem w Expo Go
```

Oczekiwany ekran: biały "Hello World" na telefonie.

---

## Krok 9.2 — Instalacja wszystkich zależności

Wszystkie zależności krok po kroku z wyjaśnieniem po co każda:

```bash
# Nawigacja (Expo Router = file-based navigation jak Next.js)
npx expo install expo-router react-native-safe-area-context react-native-screens \
  expo-linking expo-constants expo-status-bar

# Animacje (wymagane przez expo-router i react-navigation)
npx expo install react-native-reanimated react-native-gesture-handler

# Stylowanie (NativeWind = Tailwind dla React Native)
npm install nativewind tailwindcss

# Zapytania do API + zarządzanie cache
npm install @tanstack/react-query axios

# Globalny stan
npm install zustand

# Bezpieczne przechowywanie tokenów (zamiast AsyncStorage)
npx expo install expo-secure-store

# Aparat i galeria
npx expo install expo-camera expo-image-picker expo-image-manipulator

# Obraz z cache i optymalizacjami
npx expo install expo-image

# Notyfikacje push
npx expo install expo-notifications expo-device

# OAuth Google
npx expo install expo-auth-session expo-web-browser expo-crypto

# Kontent inset i keyboard handling
npx expo install react-native-keyboard-controller
npx expo install expo-haptics

# TypeScript typy
npm install -D @types/react @types/react-native
```

### Weryfikacja instalacji

```bash
npx expo-doctor
# Wszystkie checks PASS
```

---

## Krok 9.3 — Konfiguracja projektu

### `app.json` — manifest aplikacji

```json
{
  "expo": {
    "name": "PetsApp",
    "slug": "petsapp",
    "version": "1.0.0",
    "orientation": "portrait",
    "icon": "./assets/images/icon.png",
    "scheme": "petsapp",
    "userInterfaceStyle": "automatic",
    "splash": {
      "image": "./assets/images/splash-icon.png",
      "resizeMode": "contain",
      "backgroundColor": "#ffffff"
    },
    "ios": {
      "supportsTablet": true,
      "bundleIdentifier": "com.petsapp.mobile",
      "infoPlist": {
        "NSCameraUsageDescription": "PetsApp potrzebuje kamery do fotografowania psów.",
        "NSPhotoLibraryUsageDescription": "PetsApp potrzebuje dostępu do zdjęć.",
        "NSLocationWhenInUseUsageDescription": "PetsApp opcjonalnie zapisuje lokalizację złapania."
      }
    },
    "android": {
      "adaptiveIcon": {
        "foregroundImage": "./assets/images/adaptive-icon.png",
        "backgroundColor": "#ffffff"
      },
      "package": "com.petsapp.mobile",
      "permissions": [
        "CAMERA",
        "READ_EXTERNAL_STORAGE",
        "RECORD_AUDIO"
      ]
    },
    "plugins": [
      "expo-router",
      "expo-secure-store",
      [
        "expo-camera",
        { "cameraPermission": "PetsApp potrzebuje kamery do fotografowania psów." }
      ],
      [
        "expo-notifications",
        {
          "icon": "./assets/images/notification-icon.png",
          "color": "#FF6B35"
        }
      ]
    ],
    "experiments": {
      "typedRoutes": true
    }
  }
}
```

### `babel.config.js` — wymagany dla NativeWind + Reanimated

```js
module.exports = function (api) {
  api.cache(true);
  return {
    presets: [
      ['babel-preset-expo', { jsxImportSource: 'nativewind' }],
    ],
    plugins: [
      'react-native-reanimated/plugin', // MUSI być ostatni
    ],
  };
};
```

### `tailwind.config.js` — NativeWind

```js
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./app/**/*.{js,jsx,ts,tsx}', './components/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        primary: '#FF6B35',
        secondary: '#2EC4B6',
        surface: '#F5F5F5',
      },
    },
  },
  plugins: [],
};
```

### `metro.config.js` — NativeWind + SVG support

```js
const { getDefaultConfig } = require('expo/metro-config');
const { withNativeWind } = require('nativewind/metro');

const config = getDefaultConfig(__dirname);

module.exports = withNativeWind(config, { input: './global.css' });
```

### `global.css` — NativeWind entry point

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

### `nativewind-env.d.ts` — typy NativeWind

```ts
/// <reference types="nativewind/types" />
```

---

## Krok 9.4 — Struktura katalogów

Pełna struktura którą budujemy:

```
mobile/
├── app/                          # Expo Router — każdy plik = ekran/layout
│   ├── _layout.tsx               # Root layout (QueryClient, stores, fonts)
│   ├── index.tsx                 # Redirect: auth? → feed : → login
│   ├── (auth)/
│   │   ├── _layout.tsx           # Auth stack (bez tab bar)
│   │   ├── login.tsx
│   │   ├── register.tsx
│   │   ├── verify-email.tsx
│   │   ├── forgot-password.tsx
│   │   └── onboarding.tsx
│   ├── (tabs)/
│   │   ├── _layout.tsx           # Bottom tab bar
│   │   ├── feed.tsx
│   │   ├── friends.tsx
│   │   ├── catch.tsx
│   │   ├── pokedex.tsx
│   │   └── profile.tsx
│   ├── catch/
│   │   └── [id].tsx              # Szczegóły złapania
│   ├── user/
│   │   └── [id].tsx              # Profil innego usera
│   ├── notifications.tsx
│   ├── achievements.tsx
│   └── settings.tsx
├── components/
│   ├── ui/                       # Atomy (Button, Input, Avatar, Badge...)
│   ├── feed/                     # CatchCard, LikeButton, CommentList...
│   ├── catch/                    # CatchForm, ImagePicker, BreedSelector...
│   └── pokedex/                  # BreedGrid, BreedCard, ProgressBar...
├── hooks/
│   ├── useAuth.ts                # Zwraca user, login, logout
│   ├── useFeeds.ts               # TanStack query hooks dla feedu
│   ├── useBreeds.ts
│   ├── useCatches.ts
│   └── usePushNotifications.ts
├── services/
│   ├── api.ts                    # axios instance + interceptory
│   ├── auth.service.ts           # login, register, refresh, logout
│   ├── user.service.ts
│   ├── catch.service.ts
│   ├── feed.service.ts
│   ├── breed.service.ts
│   ├── friend.service.ts
│   └── notification.service.ts
├── stores/
│   ├── auth.store.ts             # Zustand: user, tokens, isAuthenticated
│   └── ui.store.ts               # Zustand: theme, loading states
├── types/
│   ├── api.types.ts              # ApiResponse<T>, PagedResponse<T>
│   ├── user.types.ts
│   ├── catch.types.ts
│   ├── breed.types.ts
│   └── friend.types.ts
├── utils/
│   ├── formatters.ts             # daty, liczby, adresy URL
│   └── validators.ts
├── constants/
│   └── config.ts                 # API_URL, timeouty, limity
├── app.json
├── babel.config.js
├── metro.config.js
├── tailwind.config.js
├── tsconfig.json
└── global.css
```

---

## Krok 9.5 — Klient API i obsługa tokenów

Najważniejszy plik w projekcie — `services/api.ts`. Odpowiada za:
1. Automatyczne dołączanie `Authorization: Bearer <token>` do każdego requestu
2. Automatyczne odświeżanie access tokena gdy wygaśnie (401 → refresh → retry)
3. Wylogowanie gdy refresh token też wygaśnie

### `constants/config.ts`

```ts
// URL backendu — zmień dla poszczególnych środowisk
const DEV_API_URL = 'http://10.0.2.2:8080/api/v1';   // Android emulator
// const DEV_API_URL = 'http://localhost:8080/api/v1'; // iOS simulator
// const DEV_API_URL = 'http://192.168.1.x:8080/api/v1'; // fizyczny telefon (Twoje IP)

export const API_URL =
  process.env.EXPO_PUBLIC_API_URL ?? DEV_API_URL;

export const REQUEST_TIMEOUT_MS = 10_000;
```

> **Ważne:** Adres IP dla fizycznego telefonu musi wskazywać na Twój komputer w sieci lokalnej.
> Sprawdź go przez: `ifconfig | grep "inet " | grep -v 127.0.0.1` (macOS/Linux)

### `types/api.types.ts`

```ts
export interface ApiResponse<T> {
  success: true;
  data: T;
  pagination?: {
    cursor: string | null;
    hasMore: boolean;
  };
}

export interface ApiError {
  success: false;
  error: {
    code: string;
    message: string;
    fields?: Record<string, string>;
  };
}

export type ApiResult<T> = ApiResponse<T> | ApiError;
```

### `stores/auth.store.ts` — Zustand + SecureStore

```ts
import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserMe | null;
  isAuthenticated: boolean;
  setTokens: (access: string, refresh: string) => Promise<void>;
  setUser: (user: UserMe) => void;
  clearAuth: () => Promise<void>;
  loadFromStorage: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  isAuthenticated: false,

  setTokens: async (access, refresh) => {
    await SecureStore.setItemAsync('access_token', access);
    await SecureStore.setItemAsync('refresh_token', refresh);
    set({ accessToken: access, refreshToken: refresh, isAuthenticated: true });
  },

  setUser: (user) => set({ user }),

  clearAuth: async () => {
    await SecureStore.deleteItemAsync('access_token');
    await SecureStore.deleteItemAsync('refresh_token');
    set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false });
  },

  loadFromStorage: async () => {
    const access = await SecureStore.getItemAsync('access_token');
    const refresh = await SecureStore.getItemAsync('refresh_token');
    if (access && refresh) {
      set({ accessToken: access, refreshToken: refresh, isAuthenticated: true });
    }
  },
}));
```

### `services/api.ts` — axios z auto-refresh

```ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { API_URL, REQUEST_TIMEOUT_MS } from '@/constants/config';
import { useAuthStore } from '@/stores/auth.store';

export const api = axios.create({
  baseURL: API_URL,
  timeout: REQUEST_TIMEOUT_MS,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — dołącz access token
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Flaga zapobiegająca równoległym próbom refreshu
let isRefreshing = false;
let failedQueue: Array<{ resolve: (v: unknown) => void; reject: (e: unknown) => void }> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    error ? prom.reject(error) : prom.resolve(token);
  });
  failedQueue = [];
};

// Response interceptor — obsługa 401 + refresh
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const { refreshToken, setTokens, clearAuth } = useAuthStore.getState();

      try {
        const { data } = await axios.post(`${API_URL}/auth/refresh`, {
          refreshToken,
        });
        const newAccess: string = data.data.accessToken;
        await setTokens(newAccess, refreshToken!);
        processQueue(null, newAccess);
        originalRequest.headers.Authorization = `Bearer ${newAccess}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as Error, null);
        await clearAuth();
        // Redirect do login — obsłuż w root _layout.tsx przez nasłuchiwanie isAuthenticated
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);
```

---

## Krok 9.6 — Auth service i hook

### `services/auth.service.ts`

```ts
import { api } from './api';
import type { ApiResponse } from '@/types/api.types';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}

export const authService = {
  login: async (body: LoginRequest) => {
    const res = await api.post<ApiResponse<AuthTokens>>('/auth/login', body);
    return res.data.data;
  },

  register: async (body: RegisterRequest) => {
    const res = await api.post<ApiResponse<void>>('/auth/register', body);
    return res.data;
  },

  verifyEmail: async (email: string, code: string) => {
    const res = await api.post<ApiResponse<AuthTokens>>('/auth/verify-email', { email, code });
    return res.data.data;
  },

  resendVerification: async (email: string) => {
    const res = await api.post<ApiResponse<void>>('/auth/resend-verification', { email });
    return res.data;
  },

  refreshToken: async (refreshToken: string) => {
    const res = await api.post<ApiResponse<AuthTokens>>('/auth/refresh', { refreshToken });
    return res.data.data;
  },

  logout: async (refreshToken: string) => {
    await api.post('/auth/logout', { refreshToken });
  },

  forgotPassword: async (email: string) => {
    const res = await api.post<ApiResponse<void>>('/auth/forgot-password', { email });
    return res.data;
  },

  resetPassword: async (token: string, newPassword: string) => {
    const res = await api.post<ApiResponse<void>>('/auth/reset-password', { token, newPassword });
    return res.data;
  },
};
```

### `hooks/useAuth.ts`

```ts
import { useCallback } from 'react';
import { useAuthStore } from '@/stores/auth.store';
import { authService } from '@/services/auth.service';
import { userService } from '@/services/user.service';

export const useAuth = () => {
  const store = useAuthStore();

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await authService.login({ email, password });
    await store.setTokens(tokens.accessToken, tokens.refreshToken);
    const user = await userService.getMe();
    store.setUser(user);
  }, []);

  const logout = useCallback(async () => {
    if (store.refreshToken) {
      try {
        await authService.logout(store.refreshToken);
      } catch {
        // Ignoruj błąd — i tak czyścimy lokalny stan
      }
    }
    await store.clearAuth();
  }, [store.refreshToken]);

  return {
    user: store.user,
    isAuthenticated: store.isAuthenticated,
    login,
    logout,
  };
};
```

---

## Krok 9.7 — Root layout i nawigacja

### `app/_layout.tsx` — Root layout (QueryClient + auto-redirect)

```tsx
import { useEffect } from 'react';
import { Stack, useRouter, useSegments } from 'expo-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { useAuthStore } from '@/stores/auth.store';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,   // 5 min — dane "świeże" przez 5 minut
      retry: 2,
    },
  },
});

function AuthGuard() {
  const { isAuthenticated, loadFromStorage } = useAuthStore();
  const segments = useSegments();
  const router = useRouter();

  // Ładuj tokeny z SecureStore przy starcie aplikacji
  useEffect(() => {
    loadFromStorage();
  }, []);

  // Redirect na podstawie stanu auth
  useEffect(() => {
    const inAuthGroup = segments[0] === '(auth)';

    if (!isAuthenticated && !inAuthGroup) {
      router.replace('/(auth)/login');
    } else if (isAuthenticated && inAuthGroup) {
      router.replace('/(tabs)/feed');
    }
  }, [isAuthenticated, segments]);

  return null;
}

export default function RootLayout() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <QueryClientProvider client={queryClient}>
        <AuthGuard />
        <Stack screenOptions={{ headerShown: false }}>
          <Stack.Screen name="(auth)" />
          <Stack.Screen name="(tabs)" />
          <Stack.Screen name="catch/[id]" options={{ presentation: 'modal' }} />
          <Stack.Screen name="user/[id]" />
          <Stack.Screen name="notifications" />
          <Stack.Screen name="settings" />
        </Stack>
      </QueryClientProvider>
    </GestureHandlerRootView>
  );
}
```

### `app/(tabs)/_layout.tsx` — Bottom Tab Bar

```tsx
import { Tabs } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#FF6B35',
        tabBarStyle: { borderTopWidth: 0.5 },
        headerShown: false,
      }}
    >
      <Tabs.Screen
        name="feed"
        options={{
          title: 'Feed',
          tabBarIcon: ({ color, size }) => <Ionicons name="home-outline" size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="friends"
        options={{
          title: 'Znajomi',
          tabBarIcon: ({ color, size }) => <Ionicons name="people-outline" size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="catch"
        options={{
          title: 'Złap!',
          tabBarIcon: ({ color, size }) => <Ionicons name="camera-outline" size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="pokedex"
        options={{
          title: 'Pokédex',
          tabBarIcon: ({ color, size }) => <Ionicons name="book-outline" size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profil',
          tabBarIcon: ({ color, size }) => <Ionicons name="person-outline" size={size} color={color} />,
        }}
      />
    </Tabs>
  );
}
```

---

## Krok 9.8 — Ekran logowania (minimalny, działa z backendem)

### `app/(auth)/login.tsx`

```tsx
import { useState } from 'react';
import { View, Text, TextInput, Pressable, Alert, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { useAuth } from '@/hooks/useAuth';
import { isAxiosError } from 'axios';

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const router = useRouter();

  const handleLogin = async () => {
    if (!email || !password) {
      Alert.alert('Błąd', 'Wypełnij wszystkie pola');
      return;
    }
    setLoading(true);
    try {
      await login(email.trim().toLowerCase(), password);
      // AuthGuard w _layout.tsx automatycznie przekieruje na feed
    } catch (err) {
      if (isAxiosError(err)) {
        const code = err.response?.data?.error?.code;
        if (code === 'UNAUTHORIZED') {
          Alert.alert('Błąd', 'Nieprawidłowy email lub hasło');
        } else if (code === 'EMAIL_NOT_VERIFIED') {
          router.push({ pathname: '/(auth)/verify-email', params: { email } });
        } else {
          Alert.alert('Błąd', err.response?.data?.error?.message ?? 'Spróbuj ponownie');
        }
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <View className="flex-1 justify-center px-6 bg-white">
      <Text className="text-3xl font-bold mb-8 text-center">PetsApp 🐾</Text>

      <TextInput
        className="border border-gray-300 rounded-xl px-4 py-3 mb-4 text-base"
        placeholder="Email"
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
        autoComplete="email"
      />

      <TextInput
        className="border border-gray-300 rounded-xl px-4 py-3 mb-6 text-base"
        placeholder="Hasło"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        autoComplete="current-password"
      />

      <Pressable
        className="bg-primary rounded-xl py-4 items-center mb-4"
        onPress={handleLogin}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color="white" />
        ) : (
          <Text className="text-white font-semibold text-base">Zaloguj się</Text>
        )}
      </Pressable>

      <Pressable onPress={() => router.push('/(auth)/forgot-password')}>
        <Text className="text-center text-gray-500">Zapomniałeś hasła?</Text>
      </Pressable>

      <Pressable className="mt-4" onPress={() => router.push('/(auth)/register')}>
        <Text className="text-center text-primary font-medium">
          Nie masz konta? Zarejestruj się
        </Text>
      </Pressable>
    </View>
  );
}
```

---

## Krok 9.9 — Weryfikacja połączenia z backendem

Prosty test: zaloguj się i pobierz własny profil.

### `services/user.service.ts` (fragment)

```ts
import { api } from './api';
import type { ApiResponse } from '@/types/api.types';
import type { UserMe } from '@/types/user.types';

export const userService = {
  getMe: async (): Promise<UserMe> => {
    const res = await api.get<ApiResponse<UserMe>>('/users/me');
    return res.data.data;
  },
};
```

### `types/user.types.ts`

```ts
export interface UserMe {
  id: string;
  username: string;
  email: string;
  avatarUrl: string | null;
  bio: string | null;
  totalCatches: number;
  uniqueBreeds: number;
  emailVerified: boolean;
  isPrivate: boolean;
  createdAt: string;
}
```

### `app/(tabs)/profile.tsx` — minimalny ekran profilu

```tsx
import { View, Text, ActivityIndicator } from 'react-native';
import { useQuery } from '@tanstack/react-query';
import { userService } from '@/services/user.service';
import { useAuth } from '@/hooks/useAuth';
import { Pressable } from 'react-native';

export default function ProfileScreen() {
  const { logout } = useAuth();

  const { data: user, isLoading, error } = useQuery({
    queryKey: ['users', 'me'],
    queryFn: () => userService.getMe(),
  });

  if (isLoading) return (
    <View className="flex-1 items-center justify-center">
      <ActivityIndicator size="large" color="#FF6B35" />
    </View>
  );

  if (error || !user) return (
    <View className="flex-1 items-center justify-center px-6">
      <Text className="text-red-500 text-center">Nie można załadować profilu</Text>
    </View>
  );

  return (
    <View className="flex-1 bg-white px-6 pt-16">
      <Text className="text-2xl font-bold">@{user.username}</Text>
      <Text className="text-gray-500 mt-1">{user.email}</Text>
      {user.bio && <Text className="mt-2">{user.bio}</Text>}

      <View className="flex-row mt-6 gap-8">
        <View className="items-center">
          <Text className="text-2xl font-bold">{user.totalCatches}</Text>
          <Text className="text-gray-500 text-sm">złapane</Text>
        </View>
        <View className="items-center">
          <Text className="text-2xl font-bold">{user.uniqueBreeds}</Text>
          <Text className="text-gray-500 text-sm">rasy</Text>
        </View>
      </View>

      <Pressable
        className="mt-8 border border-red-300 rounded-xl py-3 items-center"
        onPress={logout}
      >
        <Text className="text-red-500 font-medium">Wyloguj się</Text>
      </Pressable>
    </View>
  );
}
```

---

## Krok 9.10 — CORS i środowiskowe URL-e

Backend musi zezwolić na requesty z aplikacji mobilnej. W dev Spring Boot powinien już mieć `CorsConfig`, ale sprawdź:

```bash
# Test CORS z curl (symuluje request mobilny)
curl -X OPTIONS http://localhost:8080/api/v1/auth/login \
  -H "Origin: http://localhost:8081" \
  -H "Access-Control-Request-Method: POST" \
  -v \
  | grep "Access-Control"
# Oczekiwane: Access-Control-Allow-Origin: *
```

Jeśli brakuje nagłówków — upewnij się że w backend `CorsConfig` masz:
```java
config.addAllowedOriginPattern("*");  // dev only — prod zawęź do własnych domen
```

### Zmienne środowiskowe dla różnych środowisk

Utwórz pliki `.env.local` (gitignore!):
```bash
# mobile/.env.local
EXPO_PUBLIC_API_URL=http://192.168.1.42:8080/api/v1
```

```bash
# mobile/.env.staging
EXPO_PUBLIC_API_URL=https://api.staging.petsapp.pl/api/v1
```

```bash
# mobile/.env.production
EXPO_PUBLIC_API_URL=https://api.petsapp.pl/api/v1
```

Uruchomienie z konkretnym env:
```bash
EXPO_PUBLIC_API_URL=http://192.168.1.42:8080/api/v1 npx expo start
```

---

## Krok 9.11 — Obsługa błędów API (globalnie)

Każde wywołanie API może zwrócić błąd. Unikaj powielania `try/catch` we wszystkich hookach — stwórz helper:

### `utils/api-error.ts`

```ts
import { isAxiosError } from 'axios';

export interface ParsedApiError {
  code: string;
  message: string;
  fields?: Record<string, string>;
}

export function parseApiError(err: unknown): ParsedApiError {
  if (isAxiosError(err) && err.response?.data?.error) {
    return err.response.data.error as ParsedApiError;
  }
  return {
    code: 'INTERNAL_ERROR',
    message: 'Coś poszło nie tak. Spróbuj ponownie.',
  };
}

export function isConflictError(err: unknown): boolean {
  if (isAxiosError(err)) return err.response?.status === 409;
  return false;
}

export function isRateLimitError(err: unknown): boolean {
  if (isAxiosError(err)) return err.response?.status === 429;
  return false;
}
```

---

## Weryfikacja kroku 9

Po wykonaniu wszystkich podkroków sprawdź:

```bash
cd mobile

# 1. Uruchom metro bundler
npx expo start

# 2. Skanuj QR z Expo Go (telefon w tej samej sieci co backend)

# 3. Ekran logowania powinien się pojawić (redirect przez AuthGuard)

# 4. Zarejestruj testowego usera przez Swagger: http://localhost:8080/swagger-ui.html
#    POST /auth/register → pobierz kod z MailHog: http://localhost:8025
#    POST /auth/verify-email → skopiuj accessToken i refreshToken

# 5. Zaloguj się przez aplikację mobilną
#    Powinieneś zostać przekierowany na feed i widzieć swój profil na zakładce Profil

# 6. Sprawdź TypeScript
npx tsc --noEmit
# Brak błędów

# 7. Sprawdź lintera
npx eslint app/ services/ hooks/ --ext .ts,.tsx
# Brak błędów (ostrzeżenia OK)
```

---

## Najczęstsze problemy

| Problem | Przyczyna | Rozwiązanie |
|---|---|---|
| `Network Error` na telefonie | Backend nie nasłuchuje na `0.0.0.0` lub zły IP | Zmień `API_URL` na IP komputera (nie `localhost`) |
| `CORS policy blocked` | Backend odrzuca origin | Sprawdź `CorsConfig` w backendzie |
| `401 Unauthorized` na każdym requeście | Token nie jest dołączany | Sprawdź interceptor requestu w `api.ts` |
| Pętla redirect auth | `isAuthenticated` zmienia się asynchronicznie | Dodaj stan `isLoading` podczas `loadFromStorage()` |
| `Module not found` dla `@/...` | Brak `paths` w `tsconfig.json` | Dodaj `"@/*": ["./*"]` do `tsconfig.json` |
| NativeWind klasy nie działają | Brak `global.css` importu | Zaimportuj `./global.css` w `_layout.tsx` |
| Expo Go crashuje po dodaniu modułu natywnego | Expo Go nie obsługuje custom native modules | Użyj `npx expo prebuild` + uruchom w symulatorze/Xcode |

### `tsconfig.json` — path aliases (wymagane dla `@/...`)

```json
{
  "extends": "expo/tsconfig.base",
  "compilerOptions": {
    "strict": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./*"]
    }
  }
}
```

---

## Co dalej (Krok 10+)

| Krok | Co budujesz |
|---|---|
| **Krok 10** | Pełne ekrany auth: rejestracja → weryfikacja emaila → reset hasła → Google OAuth |
| **Krok 11** | Feed publiczny + feed znajomych + infinite scroll + lajki + komentarze |
| **Krok 12** | Ekran kamery + upload zdjęcia + wybór rasy + Pokédex |
| **Krok 13** | Profil pełny + znajomi + leaderboard + achievementy + push notyfikacje |
| **Krok 14** | CI/CD z EAS Build + testy E2E z Detox |

---

## Checklist przed przejściem do Kroku 10

- [ ] `npx expo start` startuje bez błędów
- [ ] Ekran logowania wyświetla się na telefonie/symulatorze
- [ ] Logowanie działa: użytkownik jest przekierowywany na feed po zalogowaniu
- [ ] Zakładka Profil wyświetla dane z `GET /users/me`
- [ ] Wylogowanie działa: przekierowanie z powrotem na login
- [ ] `npx tsc --noEmit` — brak błędów TypeScript
- [ ] Tokeny są zapisywane w `expo-secure-store` (nie `AsyncStorage`!)
- [ ] Auto-refresh tokena działa (sprawdź przez skrócenie czasu życia access tokena do 1 min w backendzie)
