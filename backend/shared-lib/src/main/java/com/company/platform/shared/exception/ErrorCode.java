package com.company.platform.shared.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

  // ==================== AUTH (001-099) ====================
  AUTH_INVALID_CREDENTIALS("AUTH_001", "Invalid username or password"),
  AUTH_TOKEN_EXPIRED("AUTH_002", "Token has expired"),
  AUTH_TOKEN_INVALID("AUTH_003", "Invalid token"),
  AUTH_TOKEN_REVOKED("AUTH_004", "Token has been revoked"),
  AUTH_TOKEN_MISSING("AUTH_005", "Authentication token is required"),
  AUTH_TOKEN_MALFORMED("AUTH_006", "Token format is invalid"),
  AUTH_REFRESH_TOKEN_EXPIRED("AUTH_007", "Refresh token has expired"),
  AUTH_REFRESH_TOKEN_INVALID("AUTH_008", "Invalid refresh token"),
  AUTH_ACCOUNT_LOCKED("AUTH_009", "Account is locked due to multiple failed login attempts"),
  AUTH_ACCOUNT_INACTIVE("AUTH_010", "Account is inactive"),
  AUTH_ACCOUNT_SUSPENDED("AUTH_011", "Account has been suspended"),
  AUTH_ACCOUNT_DELETED("AUTH_012", "Account has been deleted"),
  AUTH_PASSWORD_EXPIRED("AUTH_013", "Password has expired"),
  AUTH_PASSWORD_RESET_REQUIRED("AUTH_014", "Password reset is required"),
  AUTH_PASSWORD_WEAK("AUTH_015", "Password does not meet security requirements"),
  AUTH_PASSWORD_REUSED("AUTH_016", "Password has been used recently"),
  AUTH_PASSWORD_MISMATCH("AUTH_017", "Current password does not match"),
  AUTH_SESSION_EXPIRED("AUTH_018", "Session has expired"),
  AUTH_SESSION_INVALID("AUTH_019", "Invalid session"),
  AUTH_MFA_REQUIRED("AUTH_020", "Multi-factor authentication is required"),
  AUTH_MFA_INVALID("AUTH_021", "Invalid MFA code"),
  AUTH_MFA_EXPIRED("AUTH_022", "MFA code has expired"),
  AUTH_INSUFFICIENT_PERMISSION("AUTH_023", "Insufficient permissions"),
  AUTH_ACCESS_DENIED("AUTH_024", "Access denied"),
  AUTH_ROLE_NOT_FOUND("AUTH_025", "Role not found"),
  AUTH_PERMISSION_NOT_FOUND("AUTH_026", "Permission not found"),
  AUTH_CLAIM_NOT_FOUND("AUTH_027", "Claim not found"),
  AUTH_ROLE_ALREADY_EXISTS("AUTH_028", "Role already exists"),
  AUTH_PERMISSION_ALREADY_EXISTS("AUTH_029", "Permission already exists"),
  AUTH_CLAIM_ALREADY_EXISTS("AUTH_030", "Claim already exists"),
  AUTH_LAST_ADMIN_PROTECTED("AUTH_031", "Cannot deactivate the last administrator"),
  AUTH_DUPLICATE_USERNAME("AUTH_032", "Username already exists"),
  AUTH_DUPLICATE_EMAIL("AUTH_033", "Email already exists"),
  AUTH_DUPLICATE_ROLE_CODE("AUTH_034", "Role code already exists"),
  AUTH_DUPLICATE_CLAIM_CODE("AUTH_035", "Claim code already exists"),
  AUTH_DUPLICATE_PERMISSION_CODE("AUTH_036", "Permission code already exists"),
  AUTH_VALIDATION_FAILED("AUTH_037", "Authentication validation failed"),
  AUTH_PRIVILEGE_ESCALATION_DENIED("AUTH_038", "Privilege escalation denied"),

  // ==================== USER (100-199) ====================
  USER_NOT_FOUND("USER_100", "User not found"),
  USER_ALREADY_EXISTS("USER_101", "User already exists"),
  USER_EMAIL_ALREADY_EXISTS("USER_102", "Email already registered"),
  USER_USERNAME_ALREADY_EXISTS("USER_103", "Username already taken"),
  USER_PHONE_ALREADY_EXISTS("USER_104", "Phone number already registered"),
  USER_INACTIVE("USER_105", "User account is inactive"),
  USER_SUSPENDED("USER_106", "User account is suspended"),
  USER_DELETED("USER_107", "User account has been deleted"),
  USER_PROFILE_INCOMPLETE("USER_108", "User profile is incomplete"),
  USER_PROFILE_UPDATE_FAILED("USER_109", "Failed to update user profile"),
  USER_AVATAR_UPLOAD_FAILED("USER_110", "Failed to upload user avatar"),
  USER_AVATAR_TOO_LARGE("USER_111", "Avatar file size exceeds limit"),
  USER_AVATAR_INVALID_FORMAT("USER_112", "Invalid avatar file format"),
  USER_EMAIL_VERIFICATION_REQUIRED("USER_113", "Email verification is required"),
  USER_EMAIL_VERIFICATION_FAILED("USER_114", "Email verification failed"),
  USER_EMAIL_VERIFICATION_EXPIRED("USER_115", "Email verification link has expired"),
  USER_PHONE_VERIFICATION_REQUIRED("USER_116", "Phone verification is required"),
  USER_PHONE_VERIFICATION_FAILED("USER_117", "Phone verification failed"),
  USER_AGE_RESTRICTION("USER_118", "User does not meet age requirements"),
  USER_REGION_RESTRICTED("USER_119", "Service not available in user's region"),

  // ==================== ORDER (200-299) ====================
  ORDER_NOT_FOUND("ORDER_200", "Order not found"),
  ORDER_ALREADY_EXISTS("ORDER_201", "Order already exists"),
  ORDER_INSUFFICIENT_BALANCE("ORDER_202", "Insufficient account balance"),
  ORDER_INSUFFICIENT_STOCK("ORDER_203", "Insufficient stock available"),
  ORDER_INVALID_STATUS("ORDER_204", "Invalid order status"),
  ORDER_CANNOT_CANCEL("ORDER_205", "Order cannot be cancelled"),
  ORDER_CANNOT_MODIFY("ORDER_206", "Order cannot be modified"),
  ORDER_ALREADY_CANCELLED("ORDER_207", "Order has already been cancelled"),
  ORDER_ALREADY_COMPLETED("ORDER_208", "Order has already been completed"),
  ORDER_ALREADY_SHIPPED("ORDER_209", "Order has already been shipped"),
  ORDER_EXPIRED("ORDER_210", "Order has expired"),
  ORDER_PAYMENT_REQUIRED("ORDER_211", "Payment is required to process order"),
  ORDER_PAYMENT_PENDING("ORDER_212", "Payment is pending"),
  ORDER_ITEM_UNAVAILABLE("ORDER_213", "One or more items are unavailable"),
  ORDER_QUANTITY_EXCEEDED("ORDER_214", "Order quantity exceeds maximum limit"),
  ORDER_MINIMUM_AMOUNT_NOT_MET("ORDER_215", "Order does not meet minimum amount"),
  ORDER_MAXIMUM_AMOUNT_EXCEEDED("ORDER_216", "Order exceeds maximum amount"),
  ORDER_SHIPPING_ADDRESS_INVALID("ORDER_217", "Invalid shipping address"),
  ORDER_BILLING_ADDRESS_INVALID("ORDER_218", "Invalid billing address"),
  ORDER_COUPON_INVALID("ORDER_219", "Invalid or expired coupon code"),

  // ==================== PAYMENT (300-399) ====================
  PAYMENT_NOT_FOUND("PAYMENT_300", "Payment not found"),
  PAYMENT_FAILED("PAYMENT_301", "Payment processing failed"),
  PAYMENT_DECLINED("PAYMENT_302", "Payment was declined"),
  PAYMENT_CANCELLED("PAYMENT_303", "Payment was cancelled"),
  PAYMENT_EXPIRED("PAYMENT_304", "Payment session has expired"),
  PAYMENT_INSUFFICIENT_FUNDS("PAYMENT_305", "Insufficient funds"),
  PAYMENT_INVALID_AMOUNT("PAYMENT_306", "Invalid payment amount"),
  PAYMENT_AMOUNT_MISMATCH("PAYMENT_307", "Payment amount does not match order"),
  PAYMENT_METHOD_NOT_SUPPORTED("PAYMENT_308", "Payment method not supported"),
  PAYMENT_METHOD_INVALID("PAYMENT_309", "Invalid payment method"),
  PAYMENT_CARD_DECLINED("PAYMENT_310", "Card was declined"),
  PAYMENT_CARD_EXPIRED("PAYMENT_311", "Card has expired"),
  PAYMENT_CARD_INVALID("PAYMENT_312", "Invalid card details"),
  PAYMENT_CVV_INVALID("PAYMENT_313", "Invalid CVV code"),
  PAYMENT_DUPLICATE_TRANSACTION("PAYMENT_314", "Duplicate transaction detected"),
  PAYMENT_REFUND_FAILED("PAYMENT_315", "Refund processing failed"),
  PAYMENT_REFUND_ALREADY_PROCESSED("PAYMENT_316", "Refund has already been processed"),
  PAYMENT_REFUND_PERIOD_EXPIRED("PAYMENT_317", "Refund period has expired"),
  PAYMENT_GATEWAY_ERROR("PAYMENT_318", "Payment gateway error"),
  PAYMENT_GATEWAY_TIMEOUT("PAYMENT_319", "Payment gateway timeout"),

  // ==================== VALIDATION (400-499) ====================
  VALIDATION_FAILED("VALIDATION_400", "Validation failed"),
  VALIDATION_EMAIL_REQUIRED("VALIDATION_401", "Email is required"),
  VALIDATION_EMAIL_FORMAT("VALIDATION_402", "Invalid email format"),
  VALIDATION_PASSWORD_REQUIRED("VALIDATION_403", "Password is required"),
  VALIDATION_PASSWORD_MIN_LENGTH("VALIDATION_404", "Password must be at least 8 characters"),
  VALIDATION_PASSWORD_COMPLEXITY(
      "VALIDATION_405", "Password must contain uppercase, lowercase, number and special character"),
  VALIDATION_USERNAME_REQUIRED("VALIDATION_406", "Username is required"),
  VALIDATION_USERNAME_SIZE("VALIDATION_407", "Username must be between 3 and 50 characters"),
  VALIDATION_USERNAME_FORMAT(
      "VALIDATION_408", "Username can only contain letters, numbers and underscores"),
  VALIDATION_PHONE_REQUIRED("VALIDATION_409", "Phone number is required"),
  VALIDATION_PHONE_FORMAT("VALIDATION_410", "Invalid phone number format"),
  VALIDATION_NAME_REQUIRED("VALIDATION_411", "Name is required"),
  VALIDATION_NAME_SIZE("VALIDATION_412", "Name must be between 2 and 100 characters"),
  VALIDATION_ADDRESS_REQUIRED("VALIDATION_413", "Address is required"),
  VALIDATION_CITY_REQUIRED("VALIDATION_414", "City is required"),
  VALIDATION_STATE_REQUIRED("VALIDATION_415", "State is required"),
  VALIDATION_COUNTRY_REQUIRED("VALIDATION_416", "Country is required"),
  VALIDATION_POSTAL_CODE_REQUIRED("VALIDATION_417", "Postal code is required"),
  VALIDATION_POSTAL_CODE_FORMAT("VALIDATION_418", "Invalid postal code format"),
  VALIDATION_DATE_REQUIRED("VALIDATION_419", "Date is required"),
  VALIDATION_DATE_FORMAT("VALIDATION_420", "Invalid date format"),
  VALIDATION_DATE_PAST("VALIDATION_421", "Date must be in the past"),
  VALIDATION_DATE_FUTURE("VALIDATION_422", "Date must be in the future"),
  VALIDATION_AMOUNT_REQUIRED("VALIDATION_423", "Amount is required"),
  VALIDATION_AMOUNT_POSITIVE("VALIDATION_424", "Amount must be positive"),
  VALIDATION_AMOUNT_MIN("VALIDATION_425", "Amount must be at least {0}"),
  VALIDATION_INVALID_VALUE("VALIDATION_426", "Invalid value provided"),

  // ==================== SYSTEM (500-599) ====================
  SYSTEM_INTERNAL_ERROR("SYSTEM_500", "Internal server error"),
  SYSTEM_SERVICE_UNAVAILABLE("SYSTEM_501", "Service temporarily unavailable"),
  SYSTEM_MAINTENANCE_MODE("SYSTEM_502", "System is under maintenance"),
  SYSTEM_RESOURCE_EXHAUSTED("SYSTEM_503", "System resources exhausted"),
  SYSTEM_TIMEOUT("SYSTEM_504", "Request timeout"),
  SYSTEM_RATE_LIMIT_EXCEEDED("SYSTEM_505", "Rate limit exceeded"),
  SYSTEM_TOO_MANY_REQUESTS("SYSTEM_506", "Too many requests"),
  SYSTEM_CONFIGURATION_ERROR("SYSTEM_507", "System configuration error"),
  SYSTEM_FEATURE_DISABLED("SYSTEM_508", "Feature is currently disabled"),
  SYSTEM_VERSION_MISMATCH("SYSTEM_509", "API version mismatch"),
  SYSTEM_UNSUPPORTED_OPERATION("SYSTEM_510", "Operation not supported"),
  SYSTEM_QUOTA_EXCEEDED("SYSTEM_511", "Quota exceeded"),
  SYSTEM_FILE_TOO_LARGE("SYSTEM_512", "File size exceeds limit"),
  SYSTEM_INVALID_FILE_TYPE("SYSTEM_513", "Invalid file type"),
  SYSTEM_UPLOAD_FAILED("SYSTEM_514", "File upload failed"),

  // ==================== DATABASE (600-699) ====================
  DATABASE_CONNECTION_ERROR("DATABASE_600", "Database connection failed"),
  DATABASE_TIMEOUT("DATABASE_601", "Database operation timeout"),
  DATABASE_CONSTRAINT_VIOLATION("DATABASE_602", "Database constraint violation"),
  DATABASE_UNIQUE_VIOLATION("DATABASE_603", "Unique constraint violation"),
  DATABASE_FOREIGN_KEY_VIOLATION("DATABASE_604", "Foreign key constraint violation"),
  DATABASE_DEADLOCK("DATABASE_605", "Database deadlock detected"),
  DATABASE_QUERY_ERROR("DATABASE_606", "Database query error"),
  DATABASE_TRANSACTION_FAILED("DATABASE_607", "Database transaction failed"),
  DATABASE_MIGRATION_FAILED("DATABASE_608", "Database migration failed"),
  DATABASE_INTEGRITY_ERROR("DATABASE_609", "Database integrity error"),

  // ==================== EXTERNAL_API (700-799) ====================
  EXTERNAL_API_TIMEOUT("EXTERNAL_700", "External API timeout"),
  EXTERNAL_API_UNAVAILABLE("EXTERNAL_701", "External API is unavailable"),
  EXTERNAL_API_ERROR("EXTERNAL_702", "External API error"),
  EXTERNAL_API_INVALID_RESPONSE("EXTERNAL_703", "Invalid response from external API"),
  EXTERNAL_API_AUTHENTICATION_FAILED("EXTERNAL_704", "External API authentication failed"),
  EXTERNAL_API_RATE_LIMIT("EXTERNAL_705", "External API rate limit exceeded"),
  EXTERNAL_API_NOT_FOUND("EXTERNAL_706", "External API endpoint not found"),
  EXTERNAL_API_INVALID_REQUEST("EXTERNAL_707", "Invalid request to external API"),
  EXTERNAL_API_SERVICE_ERROR("EXTERNAL_708", "External service error"),
  EXTERNAL_API_CONNECTION_ERROR("EXTERNAL_709", "Failed to connect to external API"),

  // ==================== CRM (800-899) ====================
  CRM_CUSTOMER_NOT_FOUND("CRM_800", "Customer not found"),
  CRM_CUSTOMER_ALREADY_EXISTS("CRM_801", "Customer already exists"),
  CRM_LEAD_NOT_FOUND("CRM_802", "Lead not found"),
  CRM_LEAD_ALREADY_CONVERTED("CRM_803", "Lead has already been converted"),
  CRM_OPPORTUNITY_NOT_FOUND("CRM_804", "Opportunity not found"),
  CRM_OPPORTUNITY_CLOSED("CRM_805", "Opportunity is already closed"),
  CRM_ACTIVITY_NOT_FOUND("CRM_806", "Activity not found"),
  CRM_TASK_NOT_FOUND("CRM_807", "Task not found"),
  CRM_TASK_ALREADY_COMPLETED("CRM_808", "Task is already completed"),
  CRM_NOTE_NOT_FOUND("CRM_809", "Note not found"),
  CRM_INVALID_STATUS_TRANSITION("CRM_810", "Invalid status transition"),
  CRM_DUPLICATE_EMAIL("CRM_811", "Email already exists in CRM"),
  CRM_DUPLICATE_PHONE("CRM_812", "Phone number already exists in CRM"),
  CRM_VALIDATION_FAILED("CRM_813", "CRM validation failed"),
  CRM_RELATIONSHIP_EXISTS("CRM_814", "Relationship already exists"),
  CRM_RELATIONSHIP_NOT_FOUND("CRM_815", "Relationship not found"),
  CRM_PIPELINE_NOT_FOUND("CRM_816", "Pipeline not found"),
  CRM_STAGE_NOT_FOUND("CRM_817", "Stage not found"),
  CRM_INVALID_STAGE_ORDER("CRM_818", "Invalid stage order"),
  CRM_CANNOT_DELETE("CRM_819", "Cannot delete resource with existing dependencies");

  private final String code;
  private final String defaultMessage;

  ErrorCode(String code, String defaultMessage) {
    this.code = code;
    this.defaultMessage = defaultMessage;
  }

  public String getCode() {
    return code;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }
}
