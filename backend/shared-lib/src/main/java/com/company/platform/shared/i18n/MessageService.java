package com.company.platform.shared.i18n;

import com.company.platform.shared.exception.ErrorCode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

  private final MessageSource messageSource;

  /** Get translated message for an error code using the current locale */
  @Nonnull
  public String getMessage(@Nonnull ErrorCode errorCode) {
    return getMessage(errorCode, null);
  }

  /** Get translated message for an error code with arguments using the current locale */
  @Nonnull
  public String getMessage(@Nonnull ErrorCode errorCode, @Nullable Object[] args) {
    Locale locale = LocaleContextHolder.getLocale();
    return getMessage(errorCode, args, locale);
  }

  /** Get translated message for an error code with specific locale */
  @Nonnull
  public String getMessage(
      @Nonnull ErrorCode errorCode, @Nullable Object[] args, @Nonnull Locale locale) {
    try {
      String message = messageSource.getMessage(errorCode.getCode(), args, locale);
      return message != null ? message : errorCode.getDefaultMessage();
    } catch (NoSuchMessageException e) {
      log.warn(
          "No message found for code: {} in locale: {}, falling back to default message",
          errorCode.getCode(),
          locale);
      return errorCode.getDefaultMessage();
    }
  }

  /** Get translated message for a message code (for Bean Validation) */
  @Nonnull
  public String getMessage(@Nonnull String code) {
    return getMessage(code, null, LocaleContextHolder.getLocale());
  }

  /** Get translated message for a message code with arguments */
  @Nonnull
  public String getMessage(@Nonnull String code, @Nullable Object[] args, @Nonnull Locale locale) {
    try {
      String message = messageSource.getMessage(code, args, locale);
      return message != null ? message : code;
    } catch (NoSuchMessageException e) {
      log.warn(
          "No message found for code: {} in locale: {}, returning code as fallback", code, locale);
      return code;
    }
  }

  /** Check if a message exists for the given code and locale */
  public boolean hasMessage(@Nonnull String code, @Nonnull Locale locale) {
    try {
      messageSource.getMessage(code, null, locale);
      return true;
    } catch (NoSuchMessageException e) {
      return false;
    }
  }
}
