package com.ouroboros.data.exception;

/**
 * 数据访问异常的基类
 * <p>
 * 用于处理底层数据源访问过程中的错误。
 * 包括数据库异常、连接异常、查询执行异常等。
 *
 * @author Song Mingxu
 */
public class DataAccessException extends DataModelException {
  public DataAccessException() {
    super();
  }

  public DataAccessException(String message) {
    super(message);
  }

  public DataAccessException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataAccessException(Throwable cause) {
    super(cause);
  }

  protected DataAccessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
