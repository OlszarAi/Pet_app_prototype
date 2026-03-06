// src/stores/authStore.ts
import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { logoutUser } from '../api/auth';

// Klucze w AsyncStorage
const ACCESS_TOKEN_KEY = 'auth_access_token';
const REFRESH_TOKEN_KEY = 'auth_refresh_token';

type AuthState = {
  accessToken: string | null;
  refreshToken: string | null;
  isReady: boolean; // true gdy store załadował dane z AsyncStorage
  login: (tokens: { accessToken: string; refreshToken: string }) => Promise<void>;
  logout: () => Promise<void>;
  loadFromStorage: () => Promise<void>;
};

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  refreshToken: null,
  isReady: false,

  // Zapisuje tokeny w pamięci i na dysku
  login: async ({ accessToken, refreshToken }) => {
    await AsyncStorage.multiSet([
      [ACCESS_TOKEN_KEY, accessToken],
      [REFRESH_TOKEN_KEY, refreshToken],
    ]);
    set({ accessToken, refreshToken });
  },

  // Wylogowuje — informuje backend, czyści pamięć
  logout: async () => {
    const { refreshToken } = get();
    try {
      if (refreshToken) {
        await logoutUser(refreshToken);
      }
    } catch {
      // Nawet jeśli backend nie odpowie, czyścimy lokalnie
    } finally {
      await AsyncStorage.multiRemove([ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY]);
      set({ accessToken: null, refreshToken: null });
    }
  },

  // Wczytuje tokeny przy starcie aplikacji
  loadFromStorage: async () => {
    const values = await AsyncStorage.multiGet([ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY]);
    const accessToken = values[0][1];
    const refreshToken = values[1][1];
    set({ accessToken, refreshToken, isReady: true });
  },
}));