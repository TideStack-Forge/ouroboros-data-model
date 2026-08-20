package com.ouroboros.data.exception;

/**
 * 数据库连接异常
 * <p>
 * 当数据库连接获取或操作失败时抛出。
 *
 * @author Song Mingxu
 */
public class ConnectionException extends DataAccessException {
  public ConnectionException(String message) {
    super(message);
  }

  public ConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
