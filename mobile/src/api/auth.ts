import apiClient from './client';

// Typy odpowiedzi backendu
// Backend zawsze zwraca: { success: true, data: { ... } }

type ApiResponse<T> = {
  data: T;
};

type TokensData = {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
};

type MessageData = {
  message: string;
};

// ─────────────────────────────────────────────
// Rejestracja
// POST /auth/register
// Zwraca wiadomość (nie tokeny — trzeba zweryfikować email)
// ─────────────────────────────────────────────
export async function registerUser(
  email: string,
  username: string,
  password: string
): Promise<string> {
  const response = await apiClient.post<ApiResponse<MessageData>>('/auth/register', {
    email,
    username,
    password,
  });
  return response.data.data.message;
}

// ─────────────────────────────────────────────
// Weryfikacja emaila — wpisanie 6-cyfrowego kodu
// POST /auth/verify-email
// Zwraca tokeny (od razu zalogowany)
// ─────────────────────────────────────────────
export async function verifyEmail(
  email: string,
  code: string
): Promise<TokensData> {
  const response = await apiClient.post<ApiResponse<TokensData>>('/auth/verify-email', {
    email,
    code,
  });
  return response.data.data;
}

// ─────────────────────────────────────────────
// Ponowne wysłanie kodu weryfikacyjnego
// POST /auth/resend-verification
// ─────────────────────────────────────────────
export async function resendVerificationCode(email: string): Promise<string> {
  const response = await apiClient.post<ApiResponse<MessageData>>(
    '/auth/resend-verification',
    { email }
  );
  return response.data.data.message;
}

// ─────────────────────────────────────────────
// Logowanie
// POST /auth/login
// ─────────────────────────────────────────────
export async function loginUser(
  email: string,
  password: string
): Promise<TokensData> {
  const response = await apiClient.post<ApiResponse<TokensData>>('/auth/login', {
    email,
    password,
  });
  return response.data.data;
}

// ─────────────────────────────────────────────
// Wylogowanie
// POST /auth/logout
// Wymaga refreshToken — backend unieważnia token dla tego urządzenia
// ─────────────────────────────────────────────
export async function logoutUser(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', { refreshToken });
}