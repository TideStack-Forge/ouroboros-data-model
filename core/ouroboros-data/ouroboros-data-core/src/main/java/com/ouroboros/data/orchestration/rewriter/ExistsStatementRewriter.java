package com.ouroboros.data.orchestration.rewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.RelationRewriteMetadata;
import com.querydsl.core.types.Operator;
/**
 * EXISTS 语句改写器
 *
 * <p>职责：
 * <ul>
 *   <li>将同源 ToMany 关联条件改写为 EXISTS 子查询</li>
 *   <li>生成 EXISTS 子查询</li>
 *   <li>替换原条件</li>
 * </ul>
 *
 * <p>Round 2 实现：
 * <ul>
 *   <li>EXISTS 子查询生成框架</li>
 * </ul>
 *
 * <p>Round 3 实现：
 * <ul>
 *   <li>buildExistsSubQuery 核心逻辑</li>
 *   <li>replaceWithExists 核心逻辑</li>
 *   <li>辅助方法：extractRelatedCondition, isRelationCondition, getMainTableAlias</li>
 * </ul>
 *
 * <p>不包含：
 * <ul>
 *   <li>NOT EXISTS 支持（Round 4）</li>
 * </ul>
 *
 * @author Claude Code
 */
public record ExistsStatementRewriter(String relationFieldPath, String sourceFieldPath,
                                      String localKeyName, String referenceKeyName,
                                      String relationTargetName) implements StatementRewriter {
  private static final Logger logger = LoggerFactory.getLogger(ExistsStatementRewriter.class);
  public ExistsStatementRewriter(String relationFieldPath, String sourceFieldPath,
                                 String localKeyName, String referenceKeyName) {
    this(relationFieldPath, sourceFieldPath, localKeyName, referenceKeyName,
        defaultRelationTargetName(relationFieldPath));
  }
  @Override
  public QueryStatement rewrite(QueryStatement statement, OrchestrationContext context) {
    logger.debug("Rewriting ToMany relation to EXISTS subquery: {}", relationFieldPath);
    QueryStatement rewritten = replaceWithExists(statement, relationFieldPath);
    logger.debug("EXISTS rewrite completed");
    return rewritten;
  }
  /**
   * 构建 EXISTS 子查询
   * <p>
   * Round 3 实现：SELECT 1 FROM relatedTable WHERE joinCondition AND relatedCondition
   */
  private QueryStatement buildExistsSubQuery(QueryStatement statement, SExpression<?> relationExpr, String fieldPath) {
    String relatedTable = relationTargetName == null || relationTargetName.isEmpty()
        ? defaultRelationTargetName(fieldPath)
        : relationTargetName;
    SExpression<Boolean> relatedCondition = extractRelatedCondition(relationExpr);
    String mainAlias = getMainTableAlias(statement);
    SExpression<Boolean> joinCondition = SExpression.create(
        Operators.EQ,
        buildFieldExpression(relatedTable, referenceKeyName),
        buildSourceFieldExpression(mainAlias)
    );
    SExpression<Boolean> subQueryWhere;
    if (relatedCondition != null && !relatedCondition.isEmpty()) {
      subQueryWhere = SExpression.create(Operators.AND, joinCondition, relatedCondition);
    } else {
      subQueryWhere = joinCondition;
    }
    QueryStatement subQuery = QueryStatement.builder()
        .select(SExpression.constant(1))
        .from(relatedTable)
        .where(subQueryWhere)
        .build();
    return RelationRewriteMetadata.attachRelationFieldPath(subQuery, fieldPath);
  }
  @SuppressWarnings("unchecked")
  private SExpression<Boolean> extractRelatedCondition(SExpression<?> relationExpr) {
    SExpression<Boolean> relatedCondition = (SExpression<Boolean>) relationExpr.getParamAsSExpression(1);
    if (relationExpr.getOperator() == ExtOps.REL_ALL) {
      return SExpression.create(Operators.NOT, relatedCondition);
    }
    return relatedCondition;
  }
  @SuppressWarnings("unchecked")
  private SExpression<Boolean> buildExistsCondition(QueryStatement statement, SExpression<?> relationExpr, String fieldPath) {
    QueryStatement subQuery = buildExistsSubQuery(statement, relationExpr, fieldPath);
    SExpression<Boolean> existsCondition = SExpression.create(Operators.EXISTS, subQuery);
    Operator op = relationExpr.getOperator();
    if (op == ExtOps.REL_NONE || op == ExtOps.REL_ALL) {
      return SExpression.create(Operators.NOT, existsCondition);
    }
    return existsCondition;
  }
  private SExpression<?> buildFieldExpression(String path, String leaf) {
    String[] segments = path.split("\\.");
    String[] params = new String[segments.length + 1];
    System.arraycopy(segments, 0, params, 0, segments.length);
    params[segments.length] = leaf;
    return SExpression.field(params);
  }
  private SExpression<?> buildSourceFieldExpression(String mainAlias) {
    String sourcePath = sourceFieldPath == null || sourceFieldPath.isEmpty()
        ? mainAlias
        : aliasFor(sourceFieldPath);
    return buildFieldExpression(sourcePath, localKeyName);
  }
  private String aliasFor(String fieldPath) {
    return fieldPath.replace('.', '_');
  }
  private static String defaultRelationTargetName(String fieldPath) {
    int lastDot = fieldPath.lastIndexOf('.');
    return lastDot >= 0 ? fieldPath.substring(lastDot + 1) : fieldPath;
  }
  /**
   * 替换为 EXISTS 条件
   * <p>
   * Round 3 实现：将关联条件替换为 EXISTS(subQuery)
   */
  @SuppressWarnings("unchecked")
  private QueryStatement replaceWithExists(QueryStatement statement, String fieldPath) {
    SExpression<Boolean> where = statement.getWhere();
    if (where.isEmpty()) {
      return statement;
    }
    SExpression<?> transformedWhere = where.transform((expr, context) -> {
      if (isRelationCondition(expr, fieldPath)) {
        return buildExistsCondition(statement, expr, fieldPath);
      }
      return expr;
    });
    return statement.getBuilder()
        .where((SExpression<Boolean>) transformedWhere)
        .build();
  }
  /**
   * 判断表达式是否为指定字段的关联条件
   * <p>
   * Round 3 新增辅助方法
   */
  private boolean isRelationCondition(SExpression<?> expr, String fieldPath) {
    Operator op = expr.getOperator();
    if (op == ExtOps.REL_ANY || op == ExtOps.REL_ALL || op == ExtOps.REL_NONE) {
      return matchesRelationField(expr, fieldPath);
    }
    return false;
  }
  private boolean matchesRelationField(SExpression<?> expr, String fieldPath) {
    SExpression<?> fieldExpr = expr.getParamAsSExpression(0);
    if (fieldExpr.getOperator() != Operators.FIELD) {
      return false;
    }
    return fieldPath.equals(String.join(".",
        fieldExpr.getParams().stream().map(String::valueOf).toArray(String[]::new)));
  }
  /**
   * 获取主表别名
   * <p>
   * Round 3 新增辅助方法
   */
  private String getMainTableAlias(QueryStatement statement) {
    QueryStatement.TableSource from = statement.getFrom();
    if (from != null) {
      String alias = from.getAlias();
      if (alias != null) {
        return alias;
      }
      return from.getTableName();
    }
    return "t";
  }
}
