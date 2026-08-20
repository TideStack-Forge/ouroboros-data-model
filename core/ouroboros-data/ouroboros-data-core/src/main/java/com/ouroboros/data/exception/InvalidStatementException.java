package com.ouroboros.data.exception;

/**
 * 无效语句异常
 * <p>
 * 当语句结构不符合DSL规范时抛出。
 * 属于Normalize阶段的形式化错误。
 *
 * @author Song Mingxu
 */
public class InvalidStatementException extends NormalizeException {
  public InvalidStatementException() {
    super();
  }

  public InvalidStatementException(String message) {
    super(message);
  }

  public InvalidStatementException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidStatementException(Throwable cause) {
    super(cause);
  }
}
