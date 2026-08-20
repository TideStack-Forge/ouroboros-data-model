package com.ouroboros.data.exception;

/**
 * 字段不存在异常
 * <p>
 * 当引用的字段在实体中不存在时抛出。
 * 属于Transpile阶段的上下文错误。
 *
 * @author Song Mingxu
 */
public class FieldNotFoundException extends TranspileException {
  private final String fieldName;
  private final String entityName;

  public FieldNotFoundException(String fieldName, String entityName) {
    super(String.format("Field '%s' not found in entity '%s'", fieldName, entityName));
    this.fieldName = fieldName;
    this.entityName = entityName;
  }

  public FieldNotFoundException(String message, String fieldName, String entityName) {
    super(message);
    this.fieldName = fieldName;
    this.entityName = entityName;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getEntityName() {
    return entityName;
  }
}
