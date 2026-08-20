package com.ouroboros.data.exception;

/**
 * 语句相关异常的基类
 * <p>
 * 用于处理DSL语句解析、规范化、转译阶段的错误。
 * 包括Normalize阶段的形式化错误和Transpile阶段的上下文错误。
 *
 * @author Song Mingxu
 */
public class StatementException extends DataModelException {
  public StatementException() {
    super();
  }

  public StatementException(String message) {
    super(message);
  }

  public StatementException(String message, Throwable cause) {
    super(message, cause);
  }

  public StatementException(Throwable cause) {
    super(cause);
  }

  protected StatementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
