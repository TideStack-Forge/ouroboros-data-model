package com.ouroboros.data.analyze;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;

/**
 * 查询语句转换器接口。
 *
 * <p>Transform 阶段负责在 Analyze 之前对语句做结构改写，例如：
 * 通配符展开、表达式简化、常量折叠等。
 */
public interface QueryTransformer {

  default boolean supports(QueryAnalyzeContext context) {
    return true;
  }

  /**
   * 对查询语句执行结构改写。
   *
   * @param statement 输入语句
   * @param context   分析上下文
   * @return 改写后的语句
   */
  Try<QueryStatement> transform(QueryStatement statement, QueryAnalyzeContext context);
}
