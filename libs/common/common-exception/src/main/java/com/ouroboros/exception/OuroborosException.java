package com.ouroboros.exception;

import java.text.MessageFormat;
import java.util.Arrays;

import com.ouroboros.errorcode.ErrorCodeReference;

/**
 * 所有异常的基类
 */
public abstract class OuroborosException extends RuntimeException {

  private static final Integer DEFAULT_ERROR_CODE = -1;
  private final Integer errorCode;
  private final ErrorCodeReference errorCodeReference;
  private final Object[] messageArgs;

  public OuroborosException(Integer errorCode, String message) {
    this(errorCode, message, null, null, null);
  }

  public OuroborosException(Integer errorCode, String message, Throwable cause) {
    this(errorCode, message, cause, null, null);
  }

  public OuroborosException(String message, Throwable cause) {
    this(DEFAULT_ERROR_CODE, message, cause);
  }

  public OuroborosException(String message) {
    this(DEFAULT_ERROR_CODE, message);
  }

  protected OuroborosException(ErrorCodeReference errorCodeReference, Object... messageArgs) {
    this(errorCodeReference, null, messageArgs);
  }

  protected OuroborosException(ErrorCodeReference errorCodeReference, Throwable cause, Object... messageArgs) {
    this(resolveErrorCode(errorCodeReference), buildMessage(errorCodeReference, messageArgs, cause), cause, errorCodeReference, messageArgs);
  }

  private OuroborosException(Integer errorCode,
                             String message,
                             Throwable cause,
                             ErrorCodeReference errorCodeReference,
                             Object[] messageArgs) {
    super(message, cause);
    this.errorCode = errorCode == null ? DEFAULT_ERROR_CODE : errorCode;
    this.errorCodeReference = errorCodeReference;
    this.messageArgs = cloneMessageArgs(messageArgs);
  }

  public Integer getErrorCode() {
    return errorCode;
  }

  public ErrorCodeReference getErrorCodeReference() {
    return errorCodeReference;
  }

  public Object[] getMessageArgs() {
    return cloneMessageArgs(messageArgs);
  }

  private static Integer resolveErrorCode(ErrorCodeReference errorCodeReference) {
    if (errorCodeReference == null || errorCodeReference.getCode() == null) {
      return DEFAULT_ERROR_CODE;
    }
    return errorCodeReference.getCode();
  }

  private static String buildMessage(ErrorCodeReference errorCodeReference, Object[] messageArgs, Throwable cause) {
    if (errorCodeReference != null) {
      var template = errorCodeReference.getFallbackMessageTemplate();
      if (template != null) {
        return formatMessage(template, messageArgs);
      }
      if (errorCodeReference.getBizCode() != null) {
        return errorCodeReference.getBizCode();
      }
      if (errorCodeReference.getCode() != null) {
        return String.valueOf(errorCodeReference.getCode());
      }
    }
    return cause == null ? null : cause.getMessage();
  }

  private static String formatMessage(String template, Object[] messageArgs) {
    if (messageArgs == null || messageArgs.length == 0) {
      return template;
    }
    try {
      return MessageFormat.format(template, messageArgs);
    } catch (IllegalArgumentException exception) {
      return template;
    }
  }

  private static Object[] cloneMessageArgs(Object[] messageArgs) {
    return messageArgs == null ? null : Arrays.copyOf(messageArgs, messageArgs.length);
  }
}
