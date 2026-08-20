package com.ouroboros.data.exception;

/**
 * 语句语法异常
 * <p>
 * 当语句语法错误时抛出，如表达式格式不正确、参数类型错误等。
 * 属于Normalize阶段的形式化错误。
 *
 * @author Song Mingxu
 */
public class StatementSyntaxException extends NormalizeException {
  public StatementSyntaxException() {
    super();
  }

  public StatementSyntaxException(String message) {
    super(message);
  }

  public StatementSyntaxException(String message, Throwable cause) {
    super(message, cause);
  }

  public StatementSyntaxException(Throwable cause) {
    super(cause);
  }
}
