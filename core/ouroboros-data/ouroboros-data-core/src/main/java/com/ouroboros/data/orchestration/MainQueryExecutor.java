package com.ouroboros.data.orchestration;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.RecordList;

/**
 * 主查询执行器（函数式接口）
 *
 * <p>由 DataModel 提供，用于执行改写后的查询语句。
 * 通常实现为 lambda，可以访问 DataModel 的私有方法。
 *
 * @author Claude Code
 */
@FunctionalInterface
public interface MainQueryExecutor {
  /**
   * 执行查询语句
   *
   * @param statement 查询语句
   * @return 查询结果
   */
  Try<RecordList> execute(QueryStatement statement);
}
