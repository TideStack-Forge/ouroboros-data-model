package com.ouroboros.data.orchestration;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.record.RecordList;

/**
 * 查询编排器
 *
 * <p>职责：
 * <ul>
 *   <li>接收 QueryStatement 和执行上下文</li>
 *   <li>编排查询执行流程</li>
 *   <li>返回查询结果</li>
 * </ul>
 *
 * <p>不负责：
 * <ul>
 *   <li>具体的查询执行（委托给 MainQueryExecutor）</li>
 *   <li>语句改写逻辑（委托给 StatementRewriter）</li>
 *   <li>填充策略选择（委托给 PopulateStrategy）</li>
 * </ul>
 *
 * @author Claude Code
 */
public interface QueryOrchestrator {

  /**
   * 编排并执行查询
   *
   * @param statement    查询语句
   * @param rootModel    根数据模型
   * @param mainExecutor 主查询执行器（由 DataModel 提供）
   * @param context      编排上下文
   * @return 查询结果（RecordList）
   */
  Try<RecordList> orchestrate(
      QueryStatement statement,
      DataModel rootModel,
      MainQueryExecutor mainExecutor,
      OrchestrationContext context
  );

  /**
   * 改写查询语句中的关联条件（由调用方提供 OrchestrationContext）
   *
   * @param statement 查询语句
   * @param rootModel 根数据模型
   * @param context   编排上下文
   * @return 改写后的查询语句
   */
  Try<QueryStatement> rewriteStatement(QueryStatement statement, DataModel rootModel, OrchestrationContext context);
}
