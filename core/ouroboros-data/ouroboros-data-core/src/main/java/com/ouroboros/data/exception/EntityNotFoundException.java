package com.ouroboros.data.exception;

/**
 * 实体不存在异常
 * <p>
 * 当引用的实体（数据模型）不存在时抛出。
 * 属于Transpile阶段的上下文错误。
 *
 * @author Song Mingxu
 */
public class EntityNotFoundException extends TranspileException {
  private final String entityName;

  public EntityNotFoundException(String entityName) {
    super("Entity not found: " + entityName);
    this.entityName = entityName;
  }

  public EntityNotFoundException(String message, String entityName) {
    super(message);
    this.entityName = entityName;
  }

  public String getEntityName() {
    return entityName;
  }
}
