package com.ouroboros.data.exception;

/**
 * 引用约束冲突异常
 * <p>
 * 当数据违反引用约束时抛出（例如删除被引用的数据）。
 *
 * @author Song Mingxu
 */
public class ReferenceConstraintViolationException extends DataValidationException {
  private final String referenceModelName;
  private final long referenceCount;

  public ReferenceConstraintViolationException(String message) {
    super(message);
    this.referenceModelName = null;
    this.referenceCount = 0;
  }

  public ReferenceConstraintViolationException(String message, String referenceModelName, long referenceCount) {
    super(message);
    this.referenceModelName = referenceModelName;
    this.referenceCount = referenceCount;
  }

  public String getReferenceModelName() {
    return referenceModelName;
  }

  public long getReferenceCount() {
    return referenceCount;
  }
}
