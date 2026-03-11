import { useAuthStore } from "@/store/auth.store";

export function useAuth() {
  const { user, isAuthenticated, clearAuth } = useAuthStore();

  return {
    user,
    isAuthenticated,
    logout: clearAuth,
  };
}
