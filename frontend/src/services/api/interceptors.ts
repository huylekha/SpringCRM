import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from "axios";
import { useAuthStore } from "@/store/auth.store";

const TRACE_ID_HEADER = 'X-Trace-Id';
const TRACE_ID_STORAGE_KEY = 'app-trace-id';

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach((p) => {
    if (error) {
      p.reject(error);
    } else {
      p.resolve(token!);
    }
  });
  failedQueue = [];
}

/**
 * Generate or retrieve trace ID for request correlation
 */
function getOrCreateTraceId(): string {
  if (typeof window === 'undefined') return generateTraceId();
  
  let traceId = sessionStorage.getItem(TRACE_ID_STORAGE_KEY);
  if (!traceId) {
    traceId = generateTraceId();
    sessionStorage.setItem(TRACE_ID_STORAGE_KEY, traceId);
  }
  return traceId;
}

function generateTraceId(): string {
  return crypto.randomUUID().replace(/-/g, '').substring(0, 16);
}

/**
 * Get current locale from document HTML lang attribute
 * This is set by next-intl middleware
 */
function getCurrentLocale(): string {
  if (typeof window !== 'undefined') {
    return document.documentElement.lang || 'en';
  }
  return 'en';
}

export function setupInterceptors(client: AxiosInstance) {
  client.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = useAuthStore.getState().accessToken;
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      
      // Add Accept-Language header for i18n support
      if (config.headers) {
        config.headers['Accept-Language'] = getCurrentLocale();
      }
      
      // Add trace ID header for distributed tracing
      if (config.headers) {
        config.headers[TRACE_ID_HEADER] = getOrCreateTraceId();
      }
      
      // Log request for debugging
      if (typeof process !== 'undefined' && process.env && process.env.NODE_ENV === 'development') {
        console.debug(`[${getOrCreateTraceId()}] ${config.method?.toUpperCase()} ${config.url}`);
      }
      
      return config;
    },
    (error) => Promise.reject(error),
  );

  client.interceptors.response.use(
    (response: AxiosResponse) => {
      // Capture trace ID from response header
      const traceId = response.headers[TRACE_ID_HEADER.toLowerCase()];
      if (traceId && typeof window !== 'undefined') {
        sessionStorage.setItem(TRACE_ID_STORAGE_KEY, traceId);
      }
      return response;
    },
    async (error: unknown) => {
      // Attach trace ID to error for debugging
      const traceId = (error as any)?.response?.headers[TRACE_ID_HEADER.toLowerCase()];
      if (traceId) {
        (error as any).traceId = traceId;
      }
      
      const originalRequest = (error as any)?.config;

      if ((error as any)?.response?.status === 401 && !originalRequest?._retry) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject });
          }).then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return client(originalRequest);
          });
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
          const refreshToken = useAuthStore.getState().refreshToken;
          const { data } = await client.post("/auth/refresh", { refreshToken });
          const { accessToken, refreshToken: newRefreshToken } = data;

          useAuthStore.getState().setTokens(accessToken, newRefreshToken);
          processQueue(null, accessToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return client(originalRequest);
        } catch (refreshError) {
          processQueue(refreshError, null);
          useAuthStore.getState().clearAuth();
          if (typeof window !== "undefined") {
            window.location.href = "/login";
          }
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      }

      return Promise.reject(error);
    },
  );
}
