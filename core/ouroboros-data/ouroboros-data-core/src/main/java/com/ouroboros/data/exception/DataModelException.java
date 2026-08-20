package com.ouroboros.data.exception;

public class DataModelException extends RuntimeException {
  public DataModelException() {
    super();
  }

  public DataModelException(String message) {
    super(message);
  }

  public DataModelException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataModelException(Throwable cause) {
    super(cause);
  }

  protected DataModelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
