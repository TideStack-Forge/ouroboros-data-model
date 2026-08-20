package com.ouroboros.data.exception;

/**
 * 类型不匹配异常
 * <p>
 * 当字段类型与操作不兼容时抛出。
 * 属于Transpile阶段的上下文错误。
 *
 * @author Song Mingxu
 */
public class TypeMismatchException extends TranspileException {
  private final String fieldName;
  private final String expectedType;
  private final String actualType;

  public TypeMismatchException(String fieldName, String expectedType, String actualType) {
    super(String.format("Type mismatch for field '%s': expected %s but got %s", fieldName, expectedType, actualType));
    this.fieldName = fieldName;
    this.expectedType = expectedType;
    this.actualType = actualType;
  }

  public TypeMismatchException(String message, String fieldName, String expectedType, String actualType) {
    super(message);
    this.fieldName = fieldName;
    this.expectedType = expectedType;
    this.actualType = actualType;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getExpectedType() {
    return expectedType;
  }

  public String getActualType() {
    return actualType;
  }
}
