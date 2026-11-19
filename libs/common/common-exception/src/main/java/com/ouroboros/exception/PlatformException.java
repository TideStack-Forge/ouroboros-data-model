package com.ouroboros.exception;

import com.ouroboros.errorcode.ErrorCodeReference;

/**
 * 因为开发者不当使用如参数不合规、脚本语法错误、访问的模型不存在等原因产生的异常
 */
public class PlatformException extends OuroborosException {
  final private static Integer DEFAULT_ERROR_CODE = -6000;

  public PlatformException(Integer errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  public PlatformException(Integer errorCode, String message) {
    super(errorCode, message);
  }

  public PlatformException(String message) {
    super(DEFAULT_ERROR_CODE, message);
  }

  public PlatformException(String message, Throwable cause) {
    super(DEFAULT_ERROR_CODE, message, cause);
  }

  public PlatformException(ErrorCodeReference errorCodeReference, Object... messageArgs) {
    super(errorCodeReference, messageArgs);
  }

  public PlatformException(ErrorCodeReference errorCodeReference, Throwable cause, Object... messageArgs) {
    super(errorCodeReference, cause, messageArgs);
  }
}
