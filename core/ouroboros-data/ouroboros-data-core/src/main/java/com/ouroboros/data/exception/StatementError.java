package com.ouroboros.data.exception;

public class StatementError extends NormalizeException {

  public StatementError() {
    super();
  }

  public StatementError(String message) {
    super(message);
  }

  public StatementError(Throwable cause) {
    super(cause);
  }
}
