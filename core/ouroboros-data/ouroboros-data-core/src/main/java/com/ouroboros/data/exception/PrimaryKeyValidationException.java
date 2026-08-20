package com.ouroboros.data.exception;

import java.util.List;

/**
 * 主键校验异常
 * <p>
 * 当主键值缺失、为空或不符合要求时抛出。
 *
 * @author Song Mingxu
 */
public class PrimaryKeyValidationException extends DataValidationException {
  private final List<String> primaryKeyFields;

  public PrimaryKeyValidationException(String message) {
    super(message);
    this.primaryKeyFields = null;
  }

  public PrimaryKeyValidationException(String message, List<String> primaryKeyFields) {
    super(message);
    this.primaryKeyFields = primaryKeyFields;
  }

  public List<String> getPrimaryKeyFields() {
    return primaryKeyFields;
  }
}
