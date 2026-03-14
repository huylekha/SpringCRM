package com.company.platform.shared.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.platform.shared.config.I18nConfig;
import com.company.platform.shared.exception.ErrorCode;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Test MessageService translation and fallback behavior */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {I18nConfig.class, MessageService.class})
class MessageServiceTest {

  @Autowired private MessageService messageService;

  @BeforeEach
  void setUp() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void shouldReturnEnglishMessage() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    String message = messageService.getMessage(ErrorCode.USER_NOT_FOUND);

    assertThat(message).isEqualTo("User not found");
  }

  @Test
  void shouldReturnVietnameseMessage() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));

    String message = messageService.getMessage(ErrorCode.USER_NOT_FOUND);

    assertThat(message).isEqualTo("Không tìm thấy người dùng");
  }

  @Test
  void shouldReturnJapaneseMessage() {
    LocaleContextHolder.setLocale(Locale.JAPANESE);

    String message = messageService.getMessage(ErrorCode.USER_NOT_FOUND);

    assertThat(message).isEqualTo("ユーザーが見つかりません");
  }

  @Test
  void shouldReturnChineseMessage() {
    LocaleContextHolder.setLocale(Locale.CHINESE);

    String message = messageService.getMessage(ErrorCode.USER_NOT_FOUND);

    assertThat(message).isEqualTo("未找到用户");
  }

  @Test
  void shouldFallbackToDefaultMessageForUnsupportedLocale() {
    LocaleContextHolder.setLocale(Locale.KOREAN); // unsupported

    String message = messageService.getMessage(ErrorCode.USER_NOT_FOUND);

    // Should fallback to default message from ErrorCode enum
    assertThat(message).isEqualTo("User not found");
  }

  @Test
  void shouldFallbackToDefaultMessageForMissingTranslation() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    // Use a code that exists but might not have translation
    String message = messageService.getMessage(ErrorCode.CRM_CUSTOMER_NOT_FOUND);

    assertThat(message).isNotBlank();
  }

  @Test
  void shouldReturnAuthErrorMessages() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    String message = messageService.getMessage(ErrorCode.AUTH_INVALID_CREDENTIALS);
    assertThat(message).isEqualTo("Invalid username or password");

    message = messageService.getMessage(ErrorCode.AUTH_TOKEN_EXPIRED);
    assertThat(message).isEqualTo("Token has expired");
  }

  @Test
  void shouldReturnValidationErrorMessages() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    String message = messageService.getMessage(ErrorCode.VALIDATION_EMAIL_REQUIRED);
    assertThat(message).isEqualTo("Email is required");

    message = messageService.getMessage(ErrorCode.VALIDATION_PASSWORD_MIN_LENGTH);
    assertThat(message).isEqualTo("Password must be at least 8 characters");
  }

  @Test
  void shouldSupportMessageWithArguments() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    Object[] args = {"100"};
    String message = messageService.getMessage(ErrorCode.VALIDATION_FAILED, args);

    assertThat(message).isNotBlank();
  }

  @Test
  void shouldHandleNullArguments() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    String message = messageService.getMessage(ErrorCode.USER_NOT_FOUND, null);

    assertThat(message).isEqualTo("User not found");
  }

  @Test
  void shouldReturnTrueForExistingMessage() {
    boolean exists = messageService.hasMessage("USER_100", Locale.ENGLISH);

    assertThat(exists).isTrue();
  }

  @Test
  void shouldReturnFalseForMissingMessage() {
    boolean exists = messageService.hasMessage("NONEXISTENT_CODE", Locale.ENGLISH);

    assertThat(exists).isFalse();
  }

  @Test
  void shouldReturnSystemErrorMessages() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    String message = messageService.getMessage(ErrorCode.SYSTEM_INTERNAL_ERROR);
    assertThat(message).isEqualTo("Internal server error");

    message = messageService.getMessage(ErrorCode.SYSTEM_RATE_LIMIT_EXCEEDED);
    assertThat(message).isEqualTo("Rate limit exceeded");
  }

  @Test
  void shouldReturnPaymentErrorMessages() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));

    String message = messageService.getMessage(ErrorCode.PAYMENT_DECLINED);
    assertThat(message).isEqualTo("Thanh toán bị từ chối");

    message = messageService.getMessage(ErrorCode.PAYMENT_CARD_EXPIRED);
    assertThat(message).isEqualTo("Thẻ đã hết hạn");
  }
}
