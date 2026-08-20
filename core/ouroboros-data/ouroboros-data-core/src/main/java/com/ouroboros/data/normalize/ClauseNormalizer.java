package com.ouroboros.data.normalize;

import java.util.Map;

import com.ouroboros.data.dsl.statement.QueryStatement;

/**
 * 子句规范化器接口
 * <p>
 * 负责规范化特定类型的查询子句（如WHERE、SELECT、ORDER等）
 */
public interface ClauseNormalizer {

  /**
   * 检查是否支持指定的子句类型
   *
   * @param clauseType 子句类型（如"WHERE"、"SELECT"等）
   * @return true如果支持，false否则
   */
  boolean supports(String clauseType);

  /**
   * 规范化子句数据
   *
   * @param clauseData 原始子句数据
   * @param builder    查询语句构建器
   * @param context    子句规范化上下文
   * @return 更新后的查询语句构建器
   */
  QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                 QueryStatement.QueryStatementBuilder builder,
                                                 ClauseNormalizeContext context);
}
