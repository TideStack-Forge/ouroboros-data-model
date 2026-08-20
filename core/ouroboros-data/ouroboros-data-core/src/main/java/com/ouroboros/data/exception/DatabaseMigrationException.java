package com.ouroboros.data.exception;

/**
 * 数据库迁移执行异常
 * <p>
 * 当数据库迁移执行过程中发生错误时抛出。
 *
 * @author Song Mingxu
 */
public class DatabaseMigrationException extends MigrationException {
  private final String dataStationName;

  public DatabaseMigrationException(String message) {
    super(message);
    this.dataStationName = null;
  }

  public DatabaseMigrationException(String message, String dataStationName) {
    super(message);
    this.dataStationName = dataStationName;
  }

  public DatabaseMigrationException(String message, String dataStationName, Throwable cause) {
    super(message, cause);
    this.dataStationName = dataStationName;
  }

  public DatabaseMigrationException(String message, Throwable cause) {
    super(message, cause);
    this.dataStationName = null;
  }

  public String getDataStationName() {
    return dataStationName;
  }
}
