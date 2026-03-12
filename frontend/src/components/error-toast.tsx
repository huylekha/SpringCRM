'use client';

import { useTranslations } from 'next-intl';
import { toast } from 'sonner';
import { getErrorMessage, parseApiError, hasFieldErrors, getFieldErrors } from '@/lib/error-parser';

/**
 * Show error toast with translated message
 * Uses error parser with fallback strategy
 */
export function showErrorToast(error: unknown) {
  const t = useTranslations();
  const message = getErrorMessage(error, t);
  
  const apiError = parseApiError(error);
  
  toast.error(message, {
    description: apiError?.traceId ? `Trace ID: ${apiError.traceId}` : undefined,
    duration: 5000,
  });
}

/**
 * Show field-level validation errors as individual toasts
 */
export function showFieldErrorToasts(error: unknown) {
  const apiError = parseApiError(error);
  
  if (!apiError?.details || apiError.details.length === 0) {
    return;
  }
  
  apiError.details.forEach(({ field, message }) => {
    toast.error(`${field}: ${message}`, {
      duration: 5000,
    });
  });
}

/**
 * Show success toast with translated message
 */
export function showSuccessToast(messageKey: string, params?: Record<string, any>) {
  const t = useTranslations();
  toast.success(t(messageKey, params), {
    duration: 3000,
  });
}

/**
 * Show warning toast with translated message
 */
export function showWarningToast(messageKey: string, params?: Record<string, any>) {
  const t = useTranslations();
  toast.warning(t(messageKey, params), {
    duration: 4000,
  });
}

/**
 * Show info toast with translated message
 */
export function showInfoToast(messageKey: string, params?: Record<string, any>) {
  const t = useTranslations();
  toast.info(t(messageKey, params), {
    duration: 3000,
  });
}

/**
 * Custom hook for toast notifications with translations
 */
export function useErrorToast() {
  const t = useTranslations();
  
  return {
    showError: (error: unknown) => {
      const message = getErrorMessage(error, t);
      const apiError = parseApiError(error);
      
      toast.error(message, {
        description: apiError?.traceId ? `Trace ID: ${apiError.traceId}` : undefined,
        duration: 5000,
      });
      
      // If there are field errors, show them as well
      if (hasFieldErrors(error)) {
        const fieldErrors = getFieldErrors(error);
        if (fieldErrors) {
          Object.entries(fieldErrors).forEach(([field, msg]) => {
            toast.error(`${field}: ${msg}`, {
              duration: 5000,
            });
          });
        }
      }
    },
    showSuccess: (messageKey: string, params?: Record<string, any>) => {
      toast.success(t(messageKey, params), { duration: 3000 });
    },
    showWarning: (messageKey: string, params?: Record<string, any>) => {
      toast.warning(t(messageKey, params), { duration: 4000 });
    },
    showInfo: (messageKey: string, params?: Record<string, any>) => {
      toast.info(t(messageKey, params), { duration: 3000 });
    },
  };
}
