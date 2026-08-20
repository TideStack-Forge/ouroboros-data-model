package com.ouroboros.data.exception;

import java.sql.SQLException;

/**
 * 数据库异常
 * <p>
 * 包装底层数据库驱动异常（如JDBC的SQLException）。
 * 保留完整的异常链，便于问题诊断。
 *
 * @author Song Mingxu
 */
public class DatabaseException extends DataAccessException {
  private final String sqlState;
  private final int errorCode;

  public DatabaseException(String message, Throwable cause) {
    super(message, cause);
    if (cause instanceof SQLException) {
      SQLException sqlException = (SQLException) cause;
      this.sqlState = sqlException.getSQLState();
      this.errorCode = sqlException.getErrorCode();
    } else {
      this.sqlState = null;
      this.errorCode = 0;
    }
  }

  public DatabaseException(Throwable cause) {
    super(cause);
    if (cause instanceof SQLException) {
      SQLException sqlException = (SQLException) cause;
      this.sqlState = sqlException.getSQLState();
      this.errorCode = sqlException.getErrorCode();
    } else {
      this.sqlState = null;
      this.errorCode = 0;
    }
  }

  public DatabaseException(String message, String sqlState, int errorCode, Throwable cause) {
    super(message, cause);
    this.sqlState = sqlState;
    this.errorCode = errorCode;
  }

  /**
   * 获取SQL状态码
   */
  public String getSqlState() {
    return sqlState;
  }

  /**
   * 获取数据库错误代码
   */
  public int getErrorCode() {
    return errorCode;
  }
}
