package com.ouroboros.data.exception;

/**
 * 查询执行异常
 * <p>
 * 当查询执行过程中发生错误时抛出。
 * 包括SQL执行错误、结果集处理错误等。
 *
 * @author Song Mingxu
 */
public class QueryExecutionException extends DataAccessException {
  private final String query;

  public QueryExecutionException(String message) {
    super(message);
    this.query = null;
  }

  public QueryExecutionException(String message, Throwable cause) {
    super(message, cause);
    this.query = null;
  }

  public QueryExecutionException(String message, String query, Throwable cause) {
    super(message, cause);
    this.query = query;
  }

  /**
   * 获取执行失败的查询语句
   */
  public String getQuery() {
    return query;
  }
}
