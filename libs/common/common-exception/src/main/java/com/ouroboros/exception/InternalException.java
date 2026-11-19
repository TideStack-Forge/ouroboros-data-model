package com.ouroboros.exception;

import com.ouroboros.errorcode.ErrorCodeReference;

/**
 * 平台内部错误，产生原因可能是平台BUG或者来自于底层的无法处理的错误。
 */
public class InternalException extends OuroborosException {
  private static final Integer DEFAULT_ERROR_CODE = -5000;

  public InternalException(Integer errorCode, String message) {
    super(errorCode, message);
  }

  public InternalException(Integer errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  public InternalException(String message, Throwable cause) {
    super(DEFAULT_ERROR_CODE, message, cause);
  }

  public InternalException(String message) {
    super(DEFAULT_ERROR_CODE, message);
  }

  public InternalException(Throwable cause) {
    super(DEFAULT_ERROR_CODE, cause.getMessage(), cause);
  }

  public InternalException(ErrorCodeReference errorCodeReference, Object... messageArgs) {
    super(errorCodeReference, messageArgs);
  }

  public InternalException(ErrorCodeReference errorCodeReference, Throwable cause, Object... messageArgs) {
    super(errorCodeReference, cause, messageArgs);
  }
}
