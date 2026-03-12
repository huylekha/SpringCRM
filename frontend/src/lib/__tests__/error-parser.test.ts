import { describe, it, expect } from 'vitest';
import { 
  parseApiError, 
  getErrorMessage, 
  getFieldErrors, 
  isAuthError,
  isValidationError,
  hasFieldErrors 
} from '../error-parser';
import { AxiosError } from 'axios';

describe('parseApiError', () => {
  it('should parse axios error with API error response', () => {
    const axiosError = {
      isAxiosError: true,
      response: {
        data: {
          code: 'USER_100',
          message: 'User not found',
          traceId: 'abc-123',
          timestamp: '2026-03-11T10:00:00Z',
          details: []
        }
      }
    } as AxiosError;

    const result = parseApiError(axiosError);

    expect(result).toBeDefined();
    expect(result?.code).toBe('USER_100');
    expect(result?.message).toBe('User not found');
    expect(result?.traceId).toBe('abc-123');
    expect(result?.timestamp).toBe('2026-03-11T10:00:00Z');
  });

  it('should parse axios error with field validation errors', () => {
    const axiosError = {
      isAxiosError: true,
      response: {
        data: {
          code: 'VALIDATION_400',
          message: 'Validation failed',
          traceId: 'xyz-456',
          timestamp: '2026-03-11T10:00:00Z',
          details: [
            { field: 'email', message: 'Email is required' },
            { field: 'password', message: 'Password must be at least 8 characters' }
          ]
        }
      }
    } as AxiosError;

    const result = parseApiError(axiosError);

    expect(result).toBeDefined();
    expect(result?.code).toBe('VALIDATION_400');
    expect(result?.details).toHaveLength(2);
    expect(result?.details?.[0].field).toBe('email');
  });

  it('should return null for non-API errors', () => {
    const error = new Error('Network error');

    const result = parseApiError(error);

    expect(result).toBeDefined();
    expect(result?.code).toBe('SYSTEM_500');
  });

  it('should return null for undefined error', () => {
    const result = parseApiError(undefined);

    expect(result).toBeNull();
  });

  it('should handle axios error without proper API response', () => {
    const axiosError = {
      isAxiosError: true,
      message: 'Network Error'
    } as AxiosError;

    const result = parseApiError(axiosError);

    expect(result).toBeDefined();
    expect(result?.code).toBe('SYSTEM_500');
    expect(result?.message).toContain('Network Error');
  });
});

describe('getErrorMessage', () => {
  const mockT = (key: string) => {
    const translations: Record<string, string> = {
      'errors.USER_100': 'User not found',
      'errors.AUTH_001': 'Invalid username or password',
      'errors.VALIDATION_400': 'Validation failed',
      'errors.generic': 'An error occurred'
    };
    return translations[key] || key;
  };

  it('should return translated error message for known code', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: { 
          code: 'USER_100', 
          message: 'Backend message',
          traceId: 'abc',
          timestamp: new Date().toISOString()
        }
      }
    } as AxiosError;

    const message = getErrorMessage(error, mockT);

    expect(message).toBe('User not found');
  });

  it('should fallback to backend message if translation missing', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: { 
          code: 'UNKNOWN_CODE', 
          message: 'Backend fallback message',
          traceId: 'abc',
          timestamp: new Date().toISOString()
        }
      }
    } as AxiosError;

    const message = getErrorMessage(error, mockT);

    expect(message).toBe('Backend fallback message');
  });

  it('should fallback to generic message if no backend message', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: { 
          code: 'UNKNOWN_CODE',
          traceId: 'abc',
          timestamp: new Date().toISOString()
        }
      }
    } as AxiosError;

    const message = getErrorMessage(error, mockT);

    expect(message).toBe('An error occurred');
  });

  it('should return generic message for null error', () => {
    const message = getErrorMessage(null, mockT);

    expect(message).toBe('An error occurred');
  });
});

describe('getFieldErrors', () => {
  it('should extract field errors as a map', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          code: 'VALIDATION_400',
          message: 'Validation failed',
          traceId: 'abc',
          timestamp: new Date().toISOString(),
          details: [
            { field: 'email', message: 'Email is required' },
            { field: 'password', message: 'Password too short' }
          ]
        }
      }
    } as AxiosError;

    const fieldErrors = getFieldErrors(error);

    expect(fieldErrors).toBeDefined();
    expect(fieldErrors?.email).toBe('Email is required');
    expect(fieldErrors?.password).toBe('Password too short');
  });

  it('should return null if no field errors', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          code: 'USER_100',
          message: 'User not found',
          traceId: 'abc',
          timestamp: new Date().toISOString()
        }
      }
    } as AxiosError;

    const fieldErrors = getFieldErrors(error);

    expect(fieldErrors).toBeNull();
  });
});

describe('isAuthError', () => {
  it('should return true for auth error codes', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          code: 'AUTH_001',
          message: 'Invalid credentials',
          traceId: 'abc',
          timestamp: new Date().toISOString()
        }
      }
    } as AxiosError;

    expect(isAuthError(error)).toBe(true);
  });

  it('should return false for non-auth error codes', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          code: 'USER_100',
          message: 'User not found',
          traceId: 'abc',
          timestamp: new Date().toISOString()
        }
      }
    } as AxiosError;

    expect(isAuthError(error)).toBe(false);
  });
});

describe('isValidationError', () => {
  it('should return true for validation error', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          code: 'VALIDATION_400',
          message: 'Validation failed',
          traceId: 'abc',
          timestamp: new Date().toISOString()
        }
      }
    } as AxiosError;

    expect(isValidationError(error)).toBe(true);
  });

  it('should return false for non-validation error', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          code: 'USER_100',
          message: 'User not found',
          traceId: 'abc',
          timestamp: new Date().toISOString()
        }
      }
    } as AxiosError;

    expect(isValidationError(error)).toBe(false);
  });
});

describe('hasFieldErrors', () => {
  it('should return true when field errors exist', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          code: 'VALIDATION_400',
          message: 'Validation failed',
          traceId: 'abc',
          timestamp: new Date().toISOString(),
          details: [
            { field: 'email', message: 'Email is required' }
          ]
        }
      }
    } as AxiosError;

    expect(hasFieldErrors(error)).toBe(true);
  });

  it('should return false when no field errors', () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          code: 'USER_100',
          message: 'User not found',
          traceId: 'abc',
          timestamp: new Date().toISOString()
        }
      }
    } as AxiosError;

    expect(hasFieldErrors(error)).toBe(false);
  });
});
