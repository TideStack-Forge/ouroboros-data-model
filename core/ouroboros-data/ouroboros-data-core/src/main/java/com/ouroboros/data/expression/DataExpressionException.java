package com.ouroboros.data.expression;

public class DataExpressionException extends RuntimeException {
  public DataExpressionException(String message) {
    super(message);
  }

  public DataExpressionException(String message, Throwable cause) {
    super(message, cause);
  }
}
