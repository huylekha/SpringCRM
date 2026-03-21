import { AxiosError } from 'axios';
import { ApiError, FieldError } from '@/types/api';
import { isErrorCode as isValidErrorCode } from '@/types/error-codes';

/**
 * Parse unknown error into structured ApiError
 * Handles AxiosError and extracts API error response
 */
export function parseApiError(error: unknown): ApiError | null {
  if (!error) return null;

  // Handle Axios errors with API error response
  if (error instanceof AxiosError && error.response?.data) {
    const data = error.response.data;

    // Check if it matches ApiError structure
    if (data.code && data.message) {
      return {
        code: data.code,
        message: data.message,
        traceId: data.traceId || 'unknown',
        timestamp: data.timestamp || new Date().toISOString(),
        details: data.details || [],
      };
    }
  }

  // Handle Axios errors without proper API error response
  if (error instanceof AxiosError) {
    return {
      code: 'SYSTEM_500',
      message: error.message || 'Network error occurred',
      traceId: 'unknown',
      timestamp: new Date().toISOString(),
      details: [],
    };
  }

  // Handle generic errors
  if (error instanceof Error) {
    return {
      code: 'SYSTEM_500',
      message: error.message || 'An unexpected error occurred',
      traceId: 'unknown',
      timestamp: new Date().toISOString(),
      details: [],
    };
  }

  return null;
}

/**
 * Get translated error message with fallback strategy
 * 
 * Fallback chain:
 * 1. Translation from locale file (errors.{CODE})
 * 2. Backend message from API response
 * 3. Generic error message (errors.generic)
 */
export function getErrorMessage(
  error: unknown,
  t: (key: string) => string
): string {
  const apiError = parseApiError(error);

  if (!apiError) {
    return t('errors.generic');
  }

  // Try to get translation from locale files
  const translationKey = `errors.${apiError.code}`;
  const translated = t(translationKey);

  // If translation exists (not equal to key), use it
  if (translated !== translationKey) {
    return translated;
  }

  // Fallback to backend message if translation missing
  if (apiError.message) {
    return apiError.message;
  }

  // Final fallback to generic message
  return t('errors.generic');
}

/**
 * Get first field error message
 * Useful for form validation display
 */
export function getFirstFieldError(
  error: unknown,
  t: (key: string) => string
): string | null {
  const apiError = parseApiError(error);

  if (apiError?.details && apiError.details.length > 0) {
    const firstError = apiError.details[0];
    return `${firstError.field}: ${firstError.message}`;
  }

  return null;
}

/**
 * Get all field errors as a map
 * Useful for form field-level error display
 */
export function getFieldErrors(
  error: unknown
): Record<string, string> | null {
  const apiError = parseApiError(error);

  if (!apiError?.details || apiError.details.length === 0) {
    return null;
  }

  return apiError.details.reduce((acc, fieldError) => {
    acc[fieldError.field] = fieldError.message;
    return acc;
  }, {} as Record<string, string>);
}

/**
 * Check if error is a specific error code
 */
export function isErrorCode(error: unknown, code: string): boolean {
  const apiError = parseApiError(error);
  return apiError?.code === code;
}

/**
 * Check if error is an authentication error (AUTH_* codes)
 */
export function isAuthError(error: unknown): boolean {
  const apiError = parseApiError(error);
  return apiError?.code?.startsWith('AUTH_') || false;
}

/**
 * Check if error is a validation error
 */
export function isValidationError(error: unknown): boolean {
  const apiError = parseApiError(error);
  return apiError?.code === 'VALIDATION_400' || apiError?.code?.startsWith('VALIDATION_') || false;
}

/**
 * Check if error has field-level validation details
 */
export function hasFieldErrors(error: unknown): boolean {
  const apiError = parseApiError(error);
  return !!(apiError?.details && apiError.details.length > 0);
}
