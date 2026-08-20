package com.ouroboros.data.orchestration.step;

import java.util.Queue;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.MainQueryExecutor;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.OrchestrationException;
import com.ouroboros.data.orchestration.rewriter.StatementRewriter;
import com.ouroboros.data.record.RecordList;

/**
 * 主查询步骤
 *
 * <p>Round 1 实现：
 * <ul>
 *   <li>直接执行主查询，不应用任何改写器</li>
 *   <li>将结果存入 Context</li>
 * </ul>
 *
 * <p>Round 2 扩展：
 * <ul>
 *   <li>应用语句改写器队列</li>
 *   <li>支持多个改写器顺序应用</li>
 *   <li>向后兼容（无改写器时直接执行）</li>
 * </ul>
 *
 * @author Claude Code
 */
public class MainQueryStep extends AbstractQueryStep {

  private final MainQueryExecutor mainExecutor;

  public MainQueryStep(String name, MainQueryExecutor mainExecutor) {
    super(name);
    this.mainExecutor = mainExecutor;
  }

  @Override
  protected void doExecute(OrchestrationContext context) {
    // 1. 获取原始语句
    QueryStatement statement = context.getMainStatement();

    // 2. 应用所有改写器（Round 2 扩展）
    Queue<StatementRewriter> rewriters = context.getStatementRewriters();
    if (!rewriters.isEmpty()) {
      logger.debug("Applying {} statement rewriters", rewriters.size());
      for (StatementRewriter rewriter : rewriters) {
        statement = rewriter.rewrite(statement, context);
        logger.debug("Applied rewriter: {}", rewriter.getClass().getSimpleName());
      }
      // 3. 清空改写器队列
      context.clearStatementRewriters();
    } else {
      logger.debug("No rewriters to apply, executing original statement");
    }

    // 4. 执行改写后的查询
    RecordList result = mainExecutor.execute(statement)
        .getOrElseThrow(e -> new OrchestrationException("Main query execution failed", e));

    // 5. 存储结果
    context.setResult(getName(), result);
  }
}
