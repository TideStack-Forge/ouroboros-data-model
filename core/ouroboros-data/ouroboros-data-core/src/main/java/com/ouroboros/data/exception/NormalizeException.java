package com.ouroboros.data.exception;

/**
 * Normalize阶段异常
 * <p>
 * 当DSL语句规范化过程中发生形式化错误时抛出。
 * 包括语法错误、格式错误、参数类型错误等。
 * 属于语句处理的第一阶段（原始Map → QueryStatement）。
 *
 * @author Song Mingxu
 */
public class NormalizeException extends StatementException {
  public NormalizeException() {
    super();
  }

  public NormalizeException(String message) {
    super(message);
  }

  public NormalizeException(String message, Throwable cause) {
    super(message, cause);
  }

  public NormalizeException(Throwable cause) {
    super(cause);
  }

  protected NormalizeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
