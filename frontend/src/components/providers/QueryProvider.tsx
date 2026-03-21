'use client';

import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from '@/lib/query-client';
import { Toaster } from 'sonner';
import dynamic from 'next/dynamic';

// Dynamically import devtools only in development
const ReactQueryDevtools = dynamic(
  () => import('@tanstack/react-query-devtools').then((mod) => ({ default: mod.ReactQueryDevtools })),
  { ssr: false }
);

/**
 * Query Provider with error handling integration
 * 
 * Wraps the application with:
 * - TanStack Query client with global error handling
 * - Sonner toast provider for error display
 * - React Query DevTools (development only)
 */
export function QueryProvider({ children }: { children: React.ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <Toaster 
        position="top-right"
        richColors
        closeButton
        duration={4000}
      />
      {process.env.NODE_ENV === 'development' && (
        <ReactQueryDevtools initialIsOpen={false} />
      )}
    </QueryClientProvider>
  );
}
