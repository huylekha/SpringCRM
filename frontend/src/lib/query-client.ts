import { QueryClient } from '@tanstack/react-query';
import { showErrorToast } from '@/components/error-toast';
import { isAuthError } from '@/lib/error-parser';

/**
 * Create QueryClient with global error handling
 * 
 * Features:
 * - Automatic error toast display for mutations
 * - Custom handling for authentication errors
 * - Retry logic with exponential backoff
 * - Stale time and cache configuration
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        // Don't retry on auth errors
        if (isAuthError(error)) {
          return false;
        }
        // Retry up to 3 times for other errors
        return failureCount < 3;
      },
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
      refetchOnWindowFocus: false,
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes (formerly cacheTime)
    },
    mutations: {
      onError: (error) => {
        // Show error toast for all mutation errors
        // Auth errors are handled by axios interceptor (redirect to login)
        if (!isAuthError(error)) {
          showErrorToast(error);
        }
      },
      retry: false, // Don't retry mutations by default
    },
  },
});

/**
 * Reset query client (useful for logout)
 */
export function resetQueryClient() {
  queryClient.clear();
}
