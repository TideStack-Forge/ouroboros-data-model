package com.ouroboros.exception;

import com.ouroboros.errorcode.ErrorCodeReference;

/**
 * 用户输入与业务逻辑不符的异常，如缺少必填字段、格式校验失败、关联业务实体不存在或状态不正确等
 */
public class BusinessException extends OuroborosException {
  private static final Integer DEFAULT_ERROR_CODE = -4000;

  public BusinessException(Integer errorCode, String message) {
    super(errorCode, message);
  }

  public BusinessException(Integer errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  public BusinessException(String message, Throwable cause) {
    super(DEFAULT_ERROR_CODE, message, cause);
  }

  public BusinessException(String message) {
    super(DEFAULT_ERROR_CODE, message);
  }

  public BusinessException(ErrorCodeReference errorCodeReference, Object... messageArgs) {
    super(errorCodeReference, messageArgs);
  }

  public BusinessException(ErrorCodeReference errorCodeReference, Throwable cause, Object... messageArgs) {
    super(errorCodeReference, cause, messageArgs);
  }
}
