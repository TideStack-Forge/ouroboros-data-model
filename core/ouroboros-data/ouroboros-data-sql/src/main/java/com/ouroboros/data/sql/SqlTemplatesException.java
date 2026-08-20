package com.ouroboros.data.sql;

import com.ouroboros.data.exception.DataAccessException;

public class SqlTemplatesException extends DataAccessException {
  public SqlTemplatesException(String message) {
    super(message);
  }

  public SqlTemplatesException(String message, Throwable cause) {
    super(message, cause);
  }
}
