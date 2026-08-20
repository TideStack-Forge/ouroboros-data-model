package com.ouroboros.data.orchestration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.FieldPathResolver;
import com.ouroboros.data.model.valuetypes.RelatedValue;
import com.ouroboros.data.orchestration.rewriter.BareFieldCrossSourceConditionRewriter;
import com.ouroboros.data.orchestration.rewriter.ExistsStatementRewriter;
import com.ouroboros.data.orchestration.rewriter.JoinDeduplicator;
import com.ouroboros.data.orchestration.rewriter.JoinStatementRewriter;
import com.ouroboros.data.orchestration.rewriter.StatementRewriter;
import com.ouroboros.data.orchestration.step.CrossSourceRewriteStep;
import com.ouroboros.data.orchestration.step.QueryStep;
import com.ouroboros.data.orchestration.step.StatementRewriteStep;
import com.ouroboros.data.record.RecordList;
import com.querydsl.core.types.Operator;

/**
 * statement rewrite 协调器。
 *
 * <p>负责 orchestration 阶段的 relation rewrite 闭环，包括：
 * 当前 statement rewrite、嵌套 subquery rewrite、固定点递归，以及跨源 rewrite step 构建。
 */
class StatementRewriteCoordinator {

  private static final int DEFAULT_MAX_IN_LIST_SIZE = 1000;
  private static final int MAX_REWRITE_DEPTH = 8;

  private final RelationConditionPlanner relationConditionPlanner;

  StatementRewriteCoordinator(RelationConditionPlanner relationConditionPlanner) {
    this.relationConditionPlanner = relationConditionPlanner;
  }

  Try<QueryStatement> rewriteStatement(QueryStatement statement, DataModel rootModel, OrchestrationContext context) {
    return rewriteStatementInternal(statement, rootModel, context, 0);
  }

  Try<QueryStatement> rewriteStatement(QueryStatement statement, DataModel rootModel) {
    return rewriteStatement(statement, rootModel, new OrchestrationContext());
  }

  QueryStep createCrossSourceRewriteStep(
      CrossSourceCondition condition,
      QueryStatement preQuery,
      MainQueryExecutor preQueryExecutor) {
    if (!condition.implicitFieldPath()) {
      return new CrossSourceRewriteStep(
          "cross_" + condition.fieldPath(),
          preQuery,
          condition.relatedModel(),
          buildSourceLocalFieldPath(condition.sourceFieldPath(), condition.localKeyName()),
          condition.fieldPath(),
          preQueryExecutor,
          DEFAULT_MAX_IN_LIST_SIZE,
          condition.referenceKeyName()
      );
    }

    return new CrossSourceRewriteStep(
        "cross_" + condition.fieldPath(),
        preQuery,
        condition.relatedModel(),
        buildSourceLocalFieldPath(condition.sourceFieldPath(), condition.localKeyName()),
        condition.fieldPath(),
        preQueryExecutor,
        DEFAULT_MAX_IN_LIST_SIZE,
        condition.referenceKeyName()) {
      @Override
      protected void doExecute(OrchestrationContext context) {
        context.addStatementRewriter(new BareFieldCrossSourceConditionRewriter(
            getName(),
            buildSourceLocalFieldPath(condition.sourceFieldPath(), condition.localKeyName()),
            condition.fieldPath(),
            condition.referenceKeyName(),
            DEFAULT_MAX_IN_LIST_SIZE
        ));
        Try<RecordList> preQueryResult = preQueryExecutor.execute(preQuery);
        if (preQueryResult.isFailure()) {
          throw new OrchestrationException(
              "Pre-query execution failed for step: " + getName(),
              preQueryResult.getCause());
        }
        context.setResult(getName(), preQueryResult.get());
      }
    };
  }

  private Try<QueryStatement> rewriteStatementInternal(
      QueryStatement statement, DataModel rootModel, OrchestrationContext context, int depth) {
    return Try.of(() -> {
      QueryStatement currentStatement = statement;
      AnalysisResult analysis = relationConditionPlanner.analyze(currentStatement, rootModel);
      QueryStatement aggregateRewritten = rewriteRelationAggregates(currentStatement, rootModel);

      if (analysis.crossSourceConditions().isEmpty()
          && analysis.sameSourceToOneConditions().isEmpty()
          && analysis.sameSourceToManyConditions().isEmpty()) {
        return rewriteNestedSubQueries(aggregateRewritten, rootModel);
      }

      context.setMainStatement(currentStatement);

      List<QueryStep> steps = new ArrayList<>();

      for (CrossSourceCondition condition : analysis.crossSourceConditions()) {
        QueryStatement preQuery = QueryStatement.builder()
            .from(condition.relatedModel().getName())
            .select(SExpression.field(condition.referenceKeyName()))
            .where(condition.condition())
            .build();
        MainQueryExecutor preQueryExecutor = (stmt) -> condition.relatedModel().query(stmt);
        steps.add(createCrossSourceRewriteStep(condition, preQuery, preQueryExecutor));
      }

      for (SameSourceCondition condition : analysis.sameSourceToOneConditions()) {
        JoinStatementRewriter rewriter = new JoinStatementRewriter(
            condition.fieldPath(), condition.sourceFieldPath(), condition.requiresLeftJoin(),
            condition.localKeyName(), condition.referenceKeyName(), condition.relatedModel().getName());
        steps.add(new StatementRewriteStep("join_" + condition.fieldPath(), rewriter));
      }

      for (SameSourceCondition condition : analysis.sameSourceToManyConditions()) {
        ExistsStatementRewriter rewriter = new ExistsStatementRewriter(
            condition.fieldPath(), condition.sourceFieldPath(),
            condition.localKeyName(), condition.referenceKeyName());
        steps.add(new StatementRewriteStep("exists_" + condition.fieldPath(), rewriter));
      }

      if (!analysis.sameSourceToOneConditions().isEmpty()) {
        steps.add(new StatementRewriteStep("join_deduplicate", new JoinDeduplicator()));
      }

      for (QueryStep step : steps) {
        step.execute(context);
      }

      QueryStatement rewritten = context.getMainStatement();
      Queue<StatementRewriter> rewriters = context.getStatementRewriters();
      for (StatementRewriter rewriter : rewriters) {
        rewritten = rewriter.rewrite(rewritten, context);
      }
      context.clearStatementRewriters();

      rewritten = rewriteRelationAggregates(rewritten, rootModel);
      rewritten = rewriteNestedSubQueries(rewritten, rootModel);
      if (depth < MAX_REWRITE_DEPTH && hasRelationCondition(rewritten) && !rewritten.equals(currentStatement)) {
        return rewriteStatementInternal(rewritten, rootModel, new OrchestrationContext(), depth + 1).get();
      }
      return rewritten;
    });
  }

  private boolean hasRelationCondition(QueryStatement statement) {
    return containsRelationOperator(statement.getWhere()) || containsRelationOperator(statement.getHaving());
  }

  @SuppressWarnings("unchecked")
  private QueryStatement rewriteRelationAggregates(QueryStatement statement, DataModel rootModel) {
    QueryStatement current = statement;

    SExpression<Boolean> where = current.getWhere();
    if (where != null && !where.isEmpty()) {
      SExpression<Boolean> rewrittenWhere = (SExpression<Boolean>) rewriteRelationAggregates(where, statement, rootModel);
      if (!rewrittenWhere.equals(where)) {
        current = current.getBuilder().where(rewrittenWhere).build();
      }
    }

    SExpression<Boolean> having = current.getHaving();
    if (having != null && !having.isEmpty()) {
      SExpression<Boolean> rewrittenHaving = (SExpression<Boolean>) rewriteRelationAggregates(having, statement, rootModel);
      if (!rewrittenHaving.equals(having)) {
        current = current.getBuilder().having(rewrittenHaving).build();
      }
    }

    return current;
  }

  private SExpression<?> rewriteRelationAggregates(
      SExpression<?> expr, QueryStatement statement, DataModel rootModel) {
    if (expr == null || expr.isEmpty()) {
      return expr;
    }
    return expr.transform((node, context) -> rewriteRelationCountAggregate(node, statement, rootModel));
  }

  private SExpression<?> rewriteRelationCountAggregate(
      SExpression<?> expr, QueryStatement statement, DataModel rootModel) {
    if (expr.getOperator() != Operators.COUNT || expr.getParams().size() != 1) {
      return expr;
    }

    Object param = expr.getParam(0);
    if (!(param instanceof SExpression<?> fieldExpr) || fieldExpr.getOperator() != Operators.FIELD) {
      return expr;
    }

    return buildRelationCountSubQuery(statement, fieldExpr, rootModel)
        .<SExpression<?>>map(subQuery -> SExpression.create(Operators.SUB_QUERY, subQuery))
        .orElse(expr);
  }

  private Optional<QueryStatement> buildRelationCountSubQuery(
      QueryStatement statement, SExpression<?> fieldExpr, DataModel rootModel) {
    Optional<FieldPathResolver.ResolvedFieldPath> resolvedPathOpt = FieldPathResolver.resolve(fieldExpr, rootModel);
    if (!resolvedPathOpt.isPresent()) {
      return Optional.empty();
    }

    DataModelField terminalField = resolvedPathOpt.get().getTerminalField();
    if (!(terminalField.getValueType() instanceof RelatedValue<?> relatedValue)) {
      return Optional.empty();
    }

    Optional<DataModel> relatedModelOpt = relatedValue.getReferenceModel();
    Optional<DataModelField> localKeyOpt = relatedValue.getKey();
    Optional<DataModelField> referenceKeyOpt = relatedValue.getReferenceKey();
    if (!relatedModelOpt.isPresent() || !localKeyOpt.isPresent() || !referenceKeyOpt.isPresent()) {
      return Optional.empty();
    }

    String sourceAlias = resolveSourceAlias(statement, resolvedPathOpt.get());
    SExpression<Boolean> joinCondition = SExpression.create(
        Operators.EQ,
        buildFieldExpression(relatedModelOpt.get().getName(), referenceKeyOpt.get().getName()),
        buildFieldExpression(sourceAlias, localKeyOpt.get().getName())
    );

    return Optional.of(QueryStatement.builder()
        .from(relatedModelOpt.get().getName())
        .select(SExpression.create(Operators.COUNT, SExpression.columns()))
        .where(joinCondition)
        .build());
  }

  private String resolveSourceAlias(QueryStatement statement, FieldPathResolver.ResolvedFieldPath resolvedPath) {
    List<String> segments = resolvedPath.getPathSegments();
    if (segments.size() <= 1) {
      QueryStatement.TableSource from = statement.getFrom();
      if (from == null) {
        return "t";
      }
      return from.getAlias() != null ? from.getAlias() : from.getTableName();
    }
    return String.join("_", segments.subList(0, segments.size() - 1));
  }

  private SExpression<?> buildFieldExpression(String source, String field) {
    return SExpression.field(source, field);
  }

  private boolean containsRelationOperator(SExpression<?> expr) {
    if (expr == null || expr.isEmpty()) {
      return false;
    }
    AtomicBoolean found = new AtomicBoolean(false);
    expr.walk((node, context) -> {
      if (isRelationOperator(node.getOperator())) {
        found.set(true);
      }
    });
    return found.get();
  }

  @SuppressWarnings("unchecked")
  private QueryStatement rewriteNestedSubQueries(QueryStatement statement, DataModel rootModel) {
    QueryStatement current = statement;

    SExpression<Boolean> where = current.getWhere();
    if (where != null && !where.isEmpty()) {
      SExpression<Boolean> rewrittenWhere = (SExpression<Boolean>) rewriteNestedSubQueries(where, rootModel);
      if (!rewrittenWhere.equals(where)) {
        current = current.getBuilder().where(rewrittenWhere).build();
      }
    }

    SExpression<Boolean> having = current.getHaving();
    if (having != null && !having.isEmpty()) {
      SExpression<Boolean> rewrittenHaving = (SExpression<Boolean>) rewriteNestedSubQueries(having, rootModel);
      if (!rewrittenHaving.equals(having)) {
        current = current.getBuilder().having(rewrittenHaving).build();
      }
    }

    return current;
  }

  private SExpression<?> rewriteNestedSubQueries(SExpression<?> expr, DataModel rootModel) {
    if (expr == null || expr.isEmpty()) {
      return expr;
    }

    List<Object> rewrittenParams = new ArrayList<>();
    boolean changed = false;

    for (Object param : expr.getParams()) {
      Object rewrittenParam = param;
      if (param instanceof SExpression<?> childExpr) {
        rewrittenParam = rewriteNestedSubQueries(childExpr, rootModel);
      } else if (param instanceof QueryStatement subQuery) {
        Optional<DataModel> subQueryModelOpt = resolveSubQueryModel(rootModel, subQuery);
        if (subQueryModelOpt.isPresent()) {
          QueryStatement rewrittenSubQuery = rewriteStatement(subQuery, subQueryModelOpt.get(), new OrchestrationContext())
              .getOrElseThrow(cause -> new OrchestrationException("Nested sub-query rewrite failed", cause));
          rewrittenParam = rewrittenSubQuery;
        }
      }

      rewrittenParams.add(rewrittenParam);
      if (!Objects.equals(rewrittenParam, param)) {
        changed = true;
      }
    }

    if (!changed) {
      return expr;
    }
    return SExpression.create(expr.getOperator(), rewrittenParams);
  }

  private Optional<DataModel> resolveSubQueryModel(DataModel rootModel, QueryStatement subQuery) {
    Optional<String> relationFieldPathOpt = RelationRewriteMetadata.getRelationFieldPath(subQuery);
    if (relationFieldPathOpt.isPresent()) {
      SExpression<?> fieldExpr = SExpression.field(relationFieldPathOpt.get().split("\\."));
      return resolveRelationField(rootModel, fieldExpr).map(resolved -> resolved.relatedModel);
    }

    QueryStatement.TableSource from = subQuery.getFrom();
    if (from == null || from.isSubQuery()) {
      return Optional.empty();
    }

    String tableName = from.getTableName();
    if (tableName == null || tableName.isEmpty()) {
      return Optional.empty();
    }
    if (rootModel.getName().equals(tableName)) {
      return Optional.of(rootModel);
    }

    SExpression<?> fieldExpr = SExpression.field(tableName.split("\\."));
    return resolveRelationField(rootModel, fieldExpr).map(resolved -> resolved.relatedModel);
  }

  private Optional<ResolvedRelationField> resolveRelationField(DataModel rootModel, SExpression<?> fieldExpr) {
    if (fieldExpr == null || fieldExpr.isEmpty() || fieldExpr.getOperator() != Operators.FIELD) {
      return Optional.empty();
    }

    List<String> pathSegments = new ArrayList<>();
    DataModel currentModel = rootModel;
    DataModel sourceModel = rootModel;
    DataModelField currentField = null;
    RelatedValue<?> currentRelatedValue = null;

    for (Object param : fieldExpr.getParams()) {
      if (!(param instanceof String)) {
        return Optional.empty();
      }

      String segment = (String) param;
      pathSegments.add(segment);
      sourceModel = currentModel;
      currentField = currentModel.getField(segment).orElse(null);
      if (currentField == null) {
        return Optional.empty();
      }
      if (!(currentField.getValueType() instanceof RelatedValue<?> relatedValue)) {
        return Optional.empty();
      }
      currentRelatedValue = relatedValue;
      currentModel = relatedValue.getReferenceModel().orElse(null);
      if (currentModel == null) {
        return Optional.empty();
      }
    }

    return Optional.of(new ResolvedRelationField(
        String.join(".", pathSegments),
        sourceModel,
        currentField,
        currentModel,
        currentRelatedValue));
  }

  private String buildSourceLocalFieldPath(String sourceFieldPath, String localKeyName) {
    return sourceFieldPath == null || sourceFieldPath.isEmpty()
        ? localKeyName
        : sourceFieldPath + "." + localKeyName;
  }

  private boolean isRelationOperator(Operator operator) {
    return operator == ExtOps.REL_ANY || operator == ExtOps.REL_ALL || operator == ExtOps.REL_NONE;
  }

  private static class ResolvedRelationField {
    private final String fieldPath;
    @SuppressWarnings("unused")
    private final DataModel sourceModel;
    @SuppressWarnings("unused")
    private final DataModelField field;
    private final DataModel relatedModel;
    @SuppressWarnings("unused")
    private final RelatedValue<?> relatedValue;

    private ResolvedRelationField(String fieldPath, DataModel sourceModel, DataModelField field,
                                  DataModel relatedModel, RelatedValue<?> relatedValue) {
      this.fieldPath = fieldPath;
      this.sourceModel = sourceModel;
      this.field = field;
      this.relatedModel = relatedModel;
      this.relatedValue = relatedValue;
    }
  }
}
