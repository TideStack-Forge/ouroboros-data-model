package com.ouroboros.data.exception;

import java.util.Map;
import java.util.Optional;

public class ValuesListError extends StatementError {

  private static final long serialVersionUID = 1L;
  private final Map<Long, Optional<Map<String, StatementCheckFailure>>> errorLines;

  public ValuesListError(String message, Map<Long, Optional<Map<String, StatementCheckFailure>>> errorLines) {
    super(message);
    this.errorLines = errorLines;
  }

  public Map<Long, Optional<Map<String, StatementCheckFailure>>> getErrorLines() {
    return errorLines;
  }
}
