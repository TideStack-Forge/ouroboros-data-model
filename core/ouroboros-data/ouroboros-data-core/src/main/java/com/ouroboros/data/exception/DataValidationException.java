package com.ouroboros.data.exception;

/**
 * 数据校验异常的基类
 * <p>
 * 用于处理用户提交数据的业务规则校验错误。
 * 包括字段级校验、记录级校验、约束检查等。
 *
 * @author Song Mingxu
 */
public class DataValidationException extends DataModelException {
  public DataValidationException() {
    super();
  }

  public DataValidationException(String message) {
    super(message);
  }

  public DataValidationException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataValidationException(Throwable cause) {
    super(cause);
  }

  protected DataValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
