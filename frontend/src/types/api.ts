/**
 * API Error Response Structure
 * Mirrors backend ApiError.java structure
 */
export interface ApiError {
  code: string;
  message: string;
  traceId: string;
  timestamp: string;
  details?: FieldError[];
}

/**
 * Field-level validation error
 * Mirrors backend FieldError.java structure
 */
export interface FieldError {
  field: string;
  message: string;
}

/**
 * Generic API Response wrapper
 */
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: ApiError;
}

/**
 * Paginated response structure
 * Mirrors backend Page structure
 */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  sort: Array<{ field: string; direction: 'ASC' | 'DESC' }>;
}

/**
 * Sort parameters for pagination
 */
export interface SortParam {
  field: string;
  direction: 'ASC' | 'DESC';
}

/**
 * Pagination request parameters
 */
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: SortParam[];
}
