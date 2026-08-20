package com.ouroboros.data.orchestration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.PopulateContext;
import com.ouroboros.data.orchestration.rewriter.ExistsStatementRewriter;
import com.ouroboros.data.orchestration.rewriter.JoinDeduplicator;
import com.ouroboros.data.orchestration.rewriter.JoinStatementRewriter;
import com.ouroboros.data.orchestration.step.ApplyTransformersStep;
import com.ouroboros.data.orchestration.step.MainQueryStep;
import com.ouroboros.data.orchestration.step.PopulateStep;
import com.ouroboros.data.orchestration.step.QueryStep;
import com.ouroboros.data.orchestration.step.StatementRewriteStep;
import com.ouroboros.data.orchestration.strategy.DefaultPopulateStrategySelector;
import com.ouroboros.data.orchestration.strategy.PopulateField;
import com.ouroboros.data.orchestration.strategy.PopulateStrategy;
import com.ouroboros.data.orchestration.strategy.PopulateStrategySelector;
import com.ouroboros.data.record.RecordList;

/**
 * 默认查询编排器
 *
 * <p>Round 1 实现：
 * <ul>
 *   <li>只执行主查询，不做任何改写和填充</li>
 *   <li>验证接口设计的可用性</li>
 * </ul>
 *
 * <p>Round 2 扩展：
 * <ul>
 *   <li>三阶段架构：分析 → 构建 → 执行</li>
 *   <li>支持跨源条件改写</li>
 *   <li>支持同源 ToOne/ToMany 条件改写</li>
 *   <li>向后兼容（无条件时直接执行主查询）</li>
 * </ul>
 *
 * <p>Round 4 扩展：
 * <ul>
 *   <li>支持 POPULATE 子句</li>
 *   <li>使用 SeparatePopulateStrategy 创建 PopulateStep</li>
 *   <li>添加 ApplyTransformersStep 应用转换器</li>
 * </ul>
 *
 * <p>Round 6 扩展：
 * <ul>
 *   <li>添加 JoinDeduplicator 优化重复 JOIN</li>
 *   <li>在所有 JOIN/EXISTS 改写后、MainQueryStep 前执行</li>
 * </ul>
 *
 * @author Claude Code
 */
public class DefaultQueryOrchestrator implements QueryOrchestrator {

  private static final Logger logger = LoggerFactory.getLogger(DefaultQueryOrchestrator.class);
  private final RelationConditionPlanner relationConditionPlanner = new RelationConditionPlanner();
  private final StatementRewriteCoordinator statementRewriteCoordinator =
      new StatementRewriteCoordinator(relationConditionPlanner);
  private final StatementPreparer statementPreparer = new StatementPreparer();
  private final PopulatePlanner populatePlanner = new PopulatePlanner();

  @Override
  public Try<RecordList> orchestrate(
      QueryStatement statement,
      DataModel rootModel,
      MainQueryExecutor mainExecutor,
      OrchestrationContext context
  ) {
    logger.debug("Orchestrating query for model: {}", rootModel.getName());

    return Try.of(() -> {
      // 1. 初始化 Context
      context.setMainStatement(statement);
      QueryStatement currentStatement = statement;

      // 2. 解析 POPULATE 上下文（从类型化 PopulateClause 构建）
      PopulateClause populateClause = context.getPopulateClause();
      if (populateClause != null) {
        String mainAlias = currentStatement.getFrom() != null
            ? currentStatement.getFrom().getName() : rootModel.getName();
        List<PopulateContext> populateContexts = populatePlanner.buildPopulateContexts(populateClause, rootModel, mainAlias);
        context.setPopulateContexts(populateContexts);
      }

      // 3. 准备语句（SELECT 展开、OMIT、别名前缀）
      currentStatement = statementPreparer.prepare(currentStatement, rootModel, context);
      context.setMainStatement(currentStatement);

      // 4. 分析阶段：识别跨源/同源条件
      AnalysisResult analysis = relationConditionPlanner.analyze(currentStatement, rootModel);

      // 5. 构建阶段：根据分析结果构建 QueryPlan
      QueryPlan plan = buildPlan(analysis, rootModel, mainExecutor, context);

      // 6. 执行阶段：执行 QueryPlan
      return plan.execute(context).get();
    });
  }

  /**
   * 根据分析结果构建 QueryPlan
   * <p>
   * Round 2 实现：基本计划构建
   */
  private QueryPlan buildPlan(
      AnalysisResult analysis,
      DataModel rootModel,
      MainQueryExecutor mainExecutor,
      OrchestrationContext context) {
    logger.debug("Building query plan");

    List<QueryStep> steps = new ArrayList<>();

    // 1. 为每个跨源条件创建 CrossSourceRewriteStep
    for (CrossSourceCondition condition : analysis.crossSourceConditions()) {
      QueryStatement preQuery = QueryStatement.builder()
          .from(condition.relatedModel().getName())
          .select(SExpression.field(condition.referenceKeyName()))
          .where(condition.condition())
          .build();

      MainQueryExecutor preQueryExecutor = (stmt) -> condition.relatedModel().query(stmt);
      steps.add(statementRewriteCoordinator.createCrossSourceRewriteStep(condition, preQuery, preQueryExecutor));
      logger.debug("Added cross-source rewrite step for: {}", condition.fieldPath());
    }

    // 2. 为每个同源 ToOne 条件创建 StatementRewriteStep + JoinStatementRewriter
    for (SameSourceCondition condition : analysis.sameSourceToOneConditions()) {
      JoinStatementRewriter rewriter = new JoinStatementRewriter(
          condition.fieldPath(), condition.sourceFieldPath(), condition.requiresLeftJoin(),
          condition.localKeyName(), condition.referenceKeyName(), condition.relatedModel().getName());
      StatementRewriteStep step = new StatementRewriteStep(
          "join_" + condition.fieldPath(),
          rewriter
      );
      steps.add(step);
      logger.debug("Added JOIN rewrite step for: {}", condition.fieldPath());
    }

    // 3. 为每个同源 ToMany 条件创建 StatementRewriteStep + ExistsStatementRewriter
    for (SameSourceCondition condition : analysis.sameSourceToManyConditions()) {
      ExistsStatementRewriter rewriter = new ExistsStatementRewriter(
          condition.fieldPath(), condition.sourceFieldPath(),
          condition.localKeyName(), condition.referenceKeyName(),
          condition.relatedModel().getName());
      StatementRewriteStep step = new StatementRewriteStep(
          "exists_" + condition.fieldPath(),
          rewriter
      );
      steps.add(step);
      logger.debug("Added EXISTS rewrite step for: {}", condition.fieldPath());
    }

    // 4. 为每个 POPULATE 字段创建步骤，分离 rewrite steps 和 populate steps
    List<PopulateField> populateFields = populatePlanner.extractPopulateFields(context, rootModel);
    PopulateStrategySelector selector = new DefaultPopulateStrategySelector();
    List<QueryStep> populatePostSteps = new ArrayList<>();
    boolean hasPopulateRewriteSteps = false;
    for (PopulateField field : populateFields) {
      PopulateStrategy strategy = selector.select(field);
      List<QueryStep> strategySteps = strategy.createSteps(field, "main");
      for (QueryStep step : strategySteps) {
        if (step instanceof StatementRewriteStep) {
          steps.add(step);
          hasPopulateRewriteSteps = true;
        } else {
          populatePostSteps.add(step);
        }
      }
      logger.debug("Added Populate steps for field: {} (strategy: {})", field.fieldName(), strategy.getClass().getSimpleName());
    }

    // 5. 添加 JoinDeduplicator Step
    if (!analysis.sameSourceToOneConditions().isEmpty() || hasPopulateRewriteSteps) {
      JoinDeduplicator deduplicator = new JoinDeduplicator();
      StatementRewriteStep deduplicateStep = new StatementRewriteStep(
          "join_deduplicate",
          deduplicator
      );
      steps.add(deduplicateStep);
      logger.debug("Added JOIN deduplication step");
    }

    StatementRewriteStep nestedRewriteStep = new StatementRewriteStep(
        "nested_relation_rewrite",
        (statement, ignored) -> statementRewriteCoordinator.rewriteStatement(statement, rootModel, new OrchestrationContext())
            .getOrElseThrow(cause -> new OrchestrationException("Nested relation rewrite failed", cause))
    );
    steps.add(nestedRewriteStep);
    logger.debug("Added nested relation rewrite step");

    StatementRewriteStep prepareAfterRewriteStep = new StatementRewriteStep(
        "prepare_after_rewrite",
        (statement, ignored) -> statementPreparer.prepare(statement, rootModel, context)
    );
    steps.add(prepareAfterRewriteStep);
    logger.debug("Added post-rewrite statement preparation step");

    // 6. 创建 MainQueryStep
    MainQueryStep mainStep = new MainQueryStep("main", mainExecutor);
    steps.add(mainStep);

    // 7. 添加 Populate 后处理步骤（MainQueryStep 之后）
    steps.addAll(populatePostSteps);

    // 8. 添加 ApplyTransformersStep
    if (!populateFields.isEmpty()) {
      ApplyTransformersStep applyStep = new ApplyTransformersStep("apply_transformers", "main");
      steps.add(applyStep);
      logger.debug("Added ApplyTransformersStep");
    }

    // 9. 创建 SequentialPlan
    String finalStepName = populateFields.isEmpty() ? "main" : "apply_transformers";
    logger.debug("Created sequential plan with {} steps", steps.size());
    return new SequentialPlan(steps, finalStepName);
  }

  /**
   * 只执行语句改写（不执行查询），用于 count() 等只需要改写后 statement 的场景
   *
   * @param statement 原始查询语句
   * @param rootModel 根数据模型
   * @return 改写后的查询语句
   */
  public Try<QueryStatement> rewriteStatement(QueryStatement statement, DataModel rootModel, OrchestrationContext context) {
    return statementRewriteCoordinator.rewriteStatement(statement, rootModel, context);
  }

  public Try<QueryStatement> rewriteStatement(QueryStatement statement, DataModel rootModel) {
    return statementRewriteCoordinator.rewriteStatement(statement, rootModel);
  }

}
