package com.ouroboros.data.exception;

import java.util.Optional;

public class InvalidEntityName extends StatementError {
  private final Optional<String> entityName;

  public InvalidEntityName(String message, Optional<String> entityName) {
    super(message);
    this.entityName = entityName;
  }

  public Optional<String> getEntityName() {
    return entityName;
  }
}
