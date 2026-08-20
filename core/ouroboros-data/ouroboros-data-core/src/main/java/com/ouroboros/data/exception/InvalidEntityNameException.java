package com.ouroboros.data.exception;

/**
 * 无效实体名异常
 * <p>
 * 当实体名不符合命名规范时抛出。
 * 属于Normalize阶段的形式化错误。
 *
 * @author Song Mingxu
 */
public class InvalidEntityNameException extends NormalizeException {
  private final String entityName;

  public InvalidEntityNameException(String message, String entityName) {
    super(message);
    this.entityName = entityName;
  }

  public InvalidEntityNameException(String message, String entityName, Throwable cause) {
    super(message, cause);
    this.entityName = entityName;
  }

  public String getEntityName() {
    return entityName;
  }
}
