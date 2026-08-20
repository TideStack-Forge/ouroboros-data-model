package com.ouroboros.data.exception;

public class ExecuteException extends DataModelException {
  public ExecuteException() {
  }

  public ExecuteException(String message) {
    super(message);
  }

  public ExecuteException(String message, Throwable cause) {
    super(message, cause);
  }

  public ExecuteException(Throwable cause) {
    super(cause);
  }

  public ExecuteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
