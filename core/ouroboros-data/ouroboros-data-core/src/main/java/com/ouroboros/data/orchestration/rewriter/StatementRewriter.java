package com.ouroboros.data.orchestration.rewriter;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;

/**
 * 语句改写器
 *
 * <p>职责：
 * <ul>
 *   <li>接收原始 QueryStatement</li>
 *   <li>根据特定规则改写语句</li>
 *   <li>返回改写后的 QueryStatement</li>
 * </ul>
 *
 * <p>不负责：
 * <ul>
 *   <li>执行查询</li>
 *   <li>决策改写策略</li>
 * </ul>
 *
 * @author Claude Code
 */
public interface StatementRewriter {

  /**
   * 改写查询语句
   *
   * @param statement 原始查询语句
   * @param context   编排上下文（可能包含改写所需的数据）
   * @return 改写后的查询语句
   */
  QueryStatement rewrite(QueryStatement statement, OrchestrationContext context);
}
