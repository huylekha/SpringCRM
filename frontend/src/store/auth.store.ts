import { create } from "zustand";

interface AuthUser {
  id: string;
  username: string;
  roles: string[];
  claims: string[];
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUser: (user: AuthUser) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  isAuthenticated: false,

  setTokens: (accessToken, refreshToken) =>
    set({ accessToken, refreshToken, isAuthenticated: true }),

  setUser: (user) => set({ user }),

  clearAuth: () =>
    set({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
    }),
}));
