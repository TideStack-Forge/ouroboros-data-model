package com.ouroboros.data.exception;

/**
 * 数据库迁移异常基类
 * <p>
 * 用于处理数据库迁移过程中的错误。
 * 包括迁移执行、快照生成、变更日志生成等错误。
 *
 * @author Song Mingxu
 */
public class MigrationException extends DataModelException {
  public MigrationException() {
    super();
  }

  public MigrationException(String message) {
    super(message);
  }

  public MigrationException(String message, Throwable cause) {
    super(message, cause);
  }

  public MigrationException(Throwable cause) {
    super(cause);
  }

  protected MigrationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
