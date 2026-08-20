package com.ouroboros.data.exception;

import java.util.List;

/**
 * 唯一约束冲突异常
 * <p>
 * 当数据违反唯一约束时抛出。
 *
 * @author Song Mingxu
 */
public class UniqueConstraintViolationException extends DataValidationException {
  private final List<String> conflictFields;

  public UniqueConstraintViolationException(String message) {
    super(message);
    this.conflictFields = null;
  }

  public UniqueConstraintViolationException(String message, List<String> conflictFields) {
    super(message);
    this.conflictFields = conflictFields;
  }

  public List<String> getConflictFields() {
    return conflictFields;
  }
}
