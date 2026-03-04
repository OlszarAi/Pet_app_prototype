import { api } from './client';

// ---- typy żądań ----

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface VerifyEmailRequest {
  email: string;
  code: string;
}

export interface ResendVerificationRequest {
  email: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

// ---- typy odpowiedzi ----

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number; // sekundy
}

export interface MessageResponse {
  message: string;
}

// ---- wywołania API ----

export const authApi = {
  register(req: RegisterRequest) {
    return api.post<MessageResponse>('/auth/register', req);
  },

  login(req: LoginRequest) {
    return api.post<AuthTokens>('/auth/login', req);
  },

  verifyEmail(req: VerifyEmailRequest) {
    return api.post<AuthTokens>('/auth/verify-email', req);
  },

  resendVerification(req: ResendVerificationRequest) {
    return api.post<MessageResponse>('/auth/resend-verification', req);
  },

  refresh(req: RefreshTokenRequest) {
    return api.post<AuthTokens>('/auth/refresh', req);
  },

  logout(req: RefreshTokenRequest, accessToken: string) {
    return api.post<MessageResponse>('/auth/logout', req, accessToken);
  },
};
