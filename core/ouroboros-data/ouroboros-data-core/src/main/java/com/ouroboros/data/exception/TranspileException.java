package com.ouroboros.data.exception;

/**
 * Transpile阶段异常
 * <p>
 * 当DSL语句转译过程中发生上下文错误时抛出。
 * 包括实体不存在、字段不存在、类型不匹配等。
 * 属于语句处理的第二阶段（QueryStatement → QueryDSL）。
 *
 * @author Song Mingxu
 */
public class TranspileException extends StatementException {
  public TranspileException() {
    super();
  }

  public TranspileException(String message) {
    super(message);
  }

  public TranspileException(String message, Throwable cause) {
    super(message, cause);
  }

  public TranspileException(Throwable cause) {
    super(cause);
  }

  protected TranspileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
