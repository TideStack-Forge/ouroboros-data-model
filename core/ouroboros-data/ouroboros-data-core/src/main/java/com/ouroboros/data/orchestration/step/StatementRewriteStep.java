package com.ouroboros.data.orchestration.step;

import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.rewriter.StatementRewriter;

/**
 * 语句改写步骤
 *
 * <p>职责：
 * <ul>
 *   <li>将 StatementRewriter 添加到 Context 的改写器队列</li>
 * </ul>
 *
 * <p>设计要点：
 * <ul>
 *   <li>通用步骤，支持任意 StatementRewriter 实现</li>
 *   <li>不执行实际改写，只是注册改写器</li>
 *   <li>实际改写由 MainQueryStep 执行</li>
 * </ul>
 *
 * @author Claude Code
 */
public class StatementRewriteStep extends AbstractQueryStep {

  private final StatementRewriter rewriter;

  public StatementRewriteStep(String name, StatementRewriter rewriter) {
    super(name);
    this.rewriter = rewriter;
  }

  @Override
  protected void doExecute(OrchestrationContext context) {
    logger.debug("Adding statement rewriter to queue: {}", getName());
    context.addStatementRewriter(rewriter);
  }

  public StatementRewriter getRewriter() {
    return rewriter;
  }
}
