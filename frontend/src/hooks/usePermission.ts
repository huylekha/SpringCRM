import { useAuthStore } from "@/store/auth.store";

export function usePermission() {
  const user = useAuthStore((s) => s.user);

  function hasRole(role: string): boolean {
    return user?.roles?.includes(role) ?? false;
  }

  function hasClaim(claim: string): boolean {
    return user?.claims?.includes(claim) ?? false;
  }

  function hasAnyRole(roles: string[]): boolean {
    return roles.some((r) => hasRole(r));
  }

  return { hasRole, hasClaim, hasAnyRole };
}
