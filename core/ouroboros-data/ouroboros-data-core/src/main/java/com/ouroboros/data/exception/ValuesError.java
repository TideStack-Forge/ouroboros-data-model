package com.ouroboros.data.exception;

import java.util.Map;
import java.util.Optional;

public class ValuesError extends StatementError {
  private static final long serialVersionUID = 1L;
  private final Optional<Map<String, StatementCheckFailure>> detail;

  public ValuesError(String message, Optional<Map<String, StatementCheckFailure>> detail) {
    super(message);
    this.detail = detail;
  }

  public Optional<Map<String, StatementCheckFailure>> getDetail() {
    return detail;
  }
}
