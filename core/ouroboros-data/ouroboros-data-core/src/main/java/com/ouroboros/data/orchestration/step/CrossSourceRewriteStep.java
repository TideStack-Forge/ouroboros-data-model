package com.ouroboros.data.orchestration.step;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.orchestration.MainQueryExecutor;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.OrchestrationException;
import com.ouroboros.data.orchestration.rewriter.CrossSourceConditionRewriter;
import com.ouroboros.data.record.RecordList;

/**
 * 跨源条件改写步骤
 *
 * <p>职责：
 * <ul>
 *   <li>执行预查询，获取关联数据的 ID 列表</li>
 *   <li>创建 CrossSourceConditionRewriter</li>
 *   <li>将改写器添加到 Context</li>
 * </ul>
 *
 * <p>执行顺序：
 * <ol>
 *   <li>创建改写器（快速）</li>
 *   <li>执行预查询（慢速）</li>
 *   <li>存储预查询结果</li>
 * </ol>
 *
 * @author Claude Code
 */
public class CrossSourceRewriteStep extends AbstractQueryStep {

  private static final int DEFAULT_MAX_IN_LIST_SIZE = 1000;

  private final QueryStatement preQueryStatement;
  private final DataModel relatedModel;
  private final String localFieldPath;
  private final String relationFieldPath;
  private final MainQueryExecutor preQueryExecutor;
  private final int maxInListSize;
  private final String referenceKeyName;

  public CrossSourceRewriteStep(
      String name,
      QueryStatement preQueryStatement,
      DataModel relatedModel,
      String localFieldPath,
      String relationFieldPath,
      MainQueryExecutor preQueryExecutor,
      int maxInListSize,
      String referenceKeyName) {
    super(name);
    this.preQueryStatement = preQueryStatement;
    this.relatedModel = relatedModel;
    this.localFieldPath = localFieldPath;
    this.relationFieldPath = relationFieldPath;
    this.preQueryExecutor = preQueryExecutor;
    this.maxInListSize = maxInListSize;
    this.referenceKeyName = referenceKeyName;
  }

  @Override
  protected void doExecute(OrchestrationContext context) {
    logger.debug("Executing cross-source rewrite step: {}", getName());

    // 1. 创建改写器并添加到 Context
    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter(getName(), localFieldPath, relationFieldPath, referenceKeyName, maxInListSize);
    context.addStatementRewriter(rewriter);
    logger.debug("Added CrossSourceConditionRewriter to context");

    // 2. 执行预查询
    logger.debug("Executing pre-query for related model: {}", relatedModel.getName());
    Try<RecordList> preQueryResult = preQueryExecutor.execute(preQueryStatement);

    if (preQueryResult.isFailure()) {
      throw new OrchestrationException(
          "Pre-query execution failed for step: " + getName(),
          preQueryResult.getCause());
    }

    // 3. 存储预查询结果
    RecordList result = preQueryResult.get();
    context.setResult(getName(), result);
    logger.debug("Pre-query completed, {} records retrieved", result.size());
  }

  public QueryStatement getPreQueryStatement() {
    return preQueryStatement;
  }

  public DataModel getRelatedModel() {
    return relatedModel;
  }

  public String getLocalFieldPath() {
    return localFieldPath;
  }

  public String getRelationFieldPath() {
    return relationFieldPath;
  }
}
