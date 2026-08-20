package com.ouroboros.data.exception;

import java.util.Collections;
import java.util.List;

public class FieldValidationException extends DataModelException {
  private static final long serialVersionUID = 1L;
  private final String fieldName;
  private final List<String> errors;

  public FieldValidationException(String fieldName, String errorMessage) {
    this.fieldName = fieldName;
    this.errors = Collections.singletonList(errorMessage);
  }

  public FieldValidationException(String fieldName, List<String> errors) {
    this.fieldName = fieldName;
    this.errors = Collections.unmodifiableList(errors);
  }

  public String getFieldName() {
    return fieldName;
  }

  public List<String> getErrors() {
    return errors;
  }
}
