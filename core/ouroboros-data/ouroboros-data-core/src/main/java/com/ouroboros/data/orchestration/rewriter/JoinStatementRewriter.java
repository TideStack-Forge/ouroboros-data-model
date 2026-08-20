package com.ouroboros.data.orchestration.rewriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.querydsl.core.types.Operator;
/**
 * JOIN 语句改写器
 *
 * <p>职责：
 * <ul>
 *   <li>将同源 ToOne 关联条件改写为 JOIN</li>
 *   <li>支持 INNER JOIN / LEFT JOIN 选择</li>
 *   <li>移动条件到 WHERE 子句</li>
 * </ul>
 *
 * @author Claude Code
 */
public record JoinStatementRewriter(String relationFieldPath, String sourceFieldPath, boolean useLeftJoin,
                                    String localKeyName, String referenceKeyName,
                                    String relationTargetName) implements StatementRewriter {
  private static final Logger logger = LoggerFactory.getLogger(JoinStatementRewriter.class);
  public JoinStatementRewriter(String relationFieldPath, String sourceFieldPath, boolean useLeftJoin,
                               String localKeyName, String referenceKeyName) {
    this(relationFieldPath, sourceFieldPath, useLeftJoin, localKeyName, referenceKeyName,
        defaultRelationTargetName(relationFieldPath));
  }
  @Override
  public QueryStatement rewrite(QueryStatement statement, OrchestrationContext context) {
    String joinType = useLeftJoin ? "LEFT JOIN" : "INNER JOIN";
    logger.debug("Rewriting ToOne relation to {}: {}", joinType, relationFieldPath);
    // 1. 生成 JOIN 别名
    String joinAlias = generateJoinAlias(relationFieldPath);
    logger.debug("Generated JOIN alias: {}", joinAlias);
    // 2. 添加 JOIN 子句
    QueryStatement withJoin = addJoin(statement, relationFieldPath, joinAlias);
    // 3. 替换条件中的字段引用
    QueryStatement rewritten = replaceFieldReferences(withJoin, relationFieldPath, joinAlias);
    logger.debug("JOIN rewrite completed");
    return rewritten;
  }
  /**
   * 生成 JOIN 别名
   * <p>
   * 将字段路径转换为别名，例如：user.department → user_department
   */
  private String generateJoinAlias(String fieldPath) {
    return fieldPath.replace('.', '_');
  }
  /**
   * 添加 JOIN 子句
   * <p>
   * 根据 useLeftJoin 选择 INNER JOIN 或 LEFT JOIN。
   * 使用调用方传入的 localKeyName/referenceKeyName 作为键名。
   */
  private QueryStatement addJoin(QueryStatement statement, String fieldPath, String alias) {
    String tableName = relationTargetName == null || relationTargetName.isEmpty()
        ? defaultRelationTargetName(fieldPath)
        : relationTargetName;
    SExpression<Boolean> onCondition = SExpression.create(
        Operators.EQ,
        buildSourceFieldExpression(alias),
        SExpression.field(alias, referenceKeyName)
    );
    if (useLeftJoin) {
      return statement.getBuilder()
          .leftJoin(tableName, alias, onCondition)
          .build();
    } else {
      return statement.getBuilder()
          .innerJoin(tableName, alias, onCondition)
          .build();
    }
  }
  /**
   * 替换字段引用
   * <p>
   * Round 3 实现：将 fieldPath.xxx 替换为 alias.xxx
   */
  @SuppressWarnings("unchecked")
  private QueryStatement replaceFieldReferences(QueryStatement statement, String fieldPath, String alias) {
    SExpression<Boolean> where = statement.getWhere();
    if (where.isEmpty()) {
      return statement;
    }
    List<String> relationSegments = splitFieldPath(fieldPath);
    SExpression<?> transformedWhere = where.transform((expr, context) -> {
      if (isMatchingRelationAny(expr, fieldPath)) {
        return qualifyInnerCondition((SExpression<?>) expr.getParamAsSExpression(1), alias);
      }
      if (expr.getOperator() == Operators.FIELD) {
        List<String> fieldSegments = extractFieldSegments(expr);
        if (matchesPrefix(fieldSegments, relationSegments)) {
          List<String> rewrittenSegments = new ArrayList<>();
          rewrittenSegments.add(alias);
          rewrittenSegments.addAll(fieldSegments.subList(relationSegments.size(), fieldSegments.size()));
          return SExpression.field(rewrittenSegments);
        }
      }
      return expr;
    });
    return statement.getBuilder()
        .where((SExpression<Boolean>) transformedWhere)
        .build();
  }
  private boolean isMatchingRelationAny(SExpression<?> expr, String fieldPath) {
    Operator op = expr.getOperator();
    if (op != ExtOps.REL_ANY) {
      return false;
    }
    SExpression<?> fieldExpr = expr.getParamAsSExpression(0);
    if (fieldExpr.getOperator() != Operators.FIELD) {
      return false;
    }
    return fieldPath.equals(String.join(".",
        fieldExpr.getParams().stream().map(String::valueOf).toArray(String[]::new)));
  }
  private SExpression<?> qualifyInnerCondition(SExpression<?> condition, String alias) {
    return qualifyInnerCondition(condition, alias, splitFieldPath(relationFieldPath), false);
  }
  private SExpression<?> qualifyInnerCondition(
      SExpression<?> expr, String joinAlias, List<String> relationPathSegments, boolean insideNestedRelationBody) {
    if (expr == null || expr.isEmpty()) {
      return expr;
    }
    Operator op = expr.getOperator();
    if (op == ExtOps.REL_ANY || op == ExtOps.REL_ALL || op == ExtOps.REL_NONE) {
      SExpression<?> fieldExpr = expr.getParamAsSExpression(0);
      SExpression<?> qualifiedRelationField = prependRelationPath(fieldExpr, relationPathSegments);
      SExpression<?> nestedCondition = qualifyInnerCondition(
          expr.getParamAsSExpression(1), joinAlias, extractFieldSegments(qualifiedRelationField), true);
      return SExpression.create(op, qualifiedRelationField, nestedCondition);
    }
    List<Object> params = new ArrayList<>();
    for (Object param : expr.getParams()) {
      if (param instanceof SExpression<?> childExpr) {
        params.add(qualifyInnerCondition(childExpr, joinAlias, relationPathSegments, insideNestedRelationBody));
      } else {
        params.add(param);
      }
    }
    SExpression<?> rewritten = SExpression.create(op, params);
    if (!insideNestedRelationBody && op == Operators.FIELD && rewritten.getParams().size() == 1) {
      Object param = rewritten.getParam(0);
      if (param instanceof String fieldName) {
        return SExpression.field(joinAlias, fieldName);
      }
    }
    return rewritten;
  }
  private SExpression<?> prependRelationPath(SExpression<?> fieldExpr, List<String> relationPathSegments) {
    if (fieldExpr.getOperator() != Operators.FIELD) {
      return fieldExpr;
    }
    List<String> params = new ArrayList<>(relationPathSegments);
    fieldExpr.getParams().forEach(param -> params.add(String.valueOf(param)));
    return SExpression.field(params);
  }
  private SExpression<?> buildSourceFieldExpression(String alias) {
    if (sourceFieldPath == null || sourceFieldPath.isEmpty()) {
      return SExpression.field(localKeyName);
    }
    return SExpression.field(aliasFor(sourceFieldPath), localKeyName);
  }
  private String aliasFor(String fieldPath) {
    return fieldPath.replace('.', '_');
  }
  private static String defaultRelationTargetName(String fieldPath) {
    int lastDot = fieldPath.lastIndexOf('.');
    return lastDot >= 0 ? fieldPath.substring(lastDot + 1) : fieldPath;
  }
  private boolean matchesPrefix(List<String> fieldSegments, List<String> relationSegments) {
    if (fieldSegments.size() <= relationSegments.size()) {
      return false;
    }
    for (int i = 0; i < relationSegments.size(); i++) {
      if (!relationSegments.get(i).equals(fieldSegments.get(i))) {
        return false;
      }
    }
    return true;
  }
  private List<String> extractFieldSegments(SExpression<?> fieldExpr) {
    List<String> result = new ArrayList<>();
    fieldExpr.getParams().forEach(param -> {
      if (param instanceof String) {
        result.addAll(splitFieldPath((String) param));
      }
    });
    return result;
  }
  private List<String> splitFieldPath(String fieldPath) {
    return Arrays.asList(fieldPath.split("\\."));
  }
}
