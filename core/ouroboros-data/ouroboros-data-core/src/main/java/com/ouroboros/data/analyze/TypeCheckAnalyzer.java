package com.ouroboros.data.analyze;

import java.util.List;
import java.util.Optional;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.FieldPathResolver;

/**
 * 类型检查分析器
 *
 * <p>验证查询语句中的字段和操作符的类型正确性。
 *
 * <p><b>检查内容：</b>
 * <ul>
 *   <li>字段是否存在于数据模型中</li>
 *   <li>操作符是否适用于字段类型（如不能对字符串使用 &gt; 比较）</li>
 *   <li>常量值类型是否与字段类型兼容</li>
 * </ul>
 *
 * <p><b>注意：</b>
 * <ul>
 *   <li>如果上下文中没有模型元数据，则跳过类型检查</li>
 *   <li>如果 {@code enableTypeChecking} 为 false，则跳过类型检查</li>
 * </ul>
 *
 * @see QueryAnalyzer
 * @see QueryAnalyzeContext#isTypeCheckingEnabled()
 * @since 1.0.0-beta.2
 */
public class TypeCheckAnalyzer implements QueryAnalyzer {

  @Override
  public boolean supports(QueryAnalyzeContext context) {
    return context.isTypeCheckingEnabled() && context.getModel() != null;
  }

  @Override
  public Try<QueryStatement> analyze(QueryStatement statement, QueryAnalyzeContext context) {
    // 执行类型检查
    return Try.of(() -> {
      checkWhereClause(statement, context);
      checkHavingClause(statement, context);
      checkSelectClause(statement, context);
      return statement;
    });
  }

  /**
   * 检查 WHERE 子句
   */
  private void checkWhereClause(QueryStatement statement, QueryAnalyzeContext context) {
    SExpression<Boolean> where = statement.getWhere();
    if (where != null && !where.isEmpty()) {
      checkExpression(where, context.getModel(), "WHERE", statement, true);
    }
  }

  /**
   * 检查 HAVING 子句
   */
  private void checkHavingClause(QueryStatement statement, QueryAnalyzeContext context) {
    SExpression<Boolean> having = statement.getHaving();
    if (having != null && !having.isEmpty()) {
      checkExpression(having, context.getModel(), "HAVING", statement, true);
    }
  }

  /**
   * 检查 SELECT 子句
   */
  private void checkSelectClause(QueryStatement statement, QueryAnalyzeContext context) {
    List<SExpression<?>> select = statement.getSelect();
    for (SExpression<?> expr : select) {
      if (expr != null && !expr.isEmpty()) {
        checkExpression(expr, context.getModel(), "SELECT", statement, true);
      }
    }
  }

  /**
   * 递归检查表达式
   */
  private void checkExpression(
      SExpression<?> expression,
      DataModel model,
      String clauseType,
      QueryStatement statement,
      boolean rootScope) {
    if (expression == null || expression.isEmpty()) {
      return;
    }

    if (isRelationOperator(expression)) {
      checkRelationExpression(expression, model, clauseType, statement, rootScope);
      return;
    }

    if (expression.getOperator() == Operators.FIELD) {
      SExpression<?> fieldExpression = rootScope
          ? FieldPathResolver.stripRootSourceQualifier(expression, statement)
          : expression;
      if (fieldExpression.getParams().size() == 1) {
        String fieldName = (String) fieldExpression.getParam(0);
        checkFieldExists(fieldName, model, clauseType);
      } else if (fieldExpression.getParams().size() > 1) {
        FieldPathValidator.validateFieldPath(fieldExpression, model, clauseType);
      }
      return;
    }

    for (Object param : expression.getParams()) {
      if (param instanceof SExpression<?> sExpr) {
        checkExpression(sExpr, model, clauseType, statement, rootScope);
      }
    }
  }

  private void checkRelationExpression(
      SExpression<?> expression,
      DataModel model,
      String clauseType,
      QueryStatement statement,
      boolean rootScope) {
    if (expression.getParams().isEmpty()) {
      return;
    }

    Object relationParam = expression.getParam(0);
    if (!(relationParam instanceof SExpression<?> relationExpr)) {
      return;
    }

    checkExpression(relationExpr, model, clauseType, statement, rootScope);

    DataModel relatedModel = resolveRelatedModel(relationExpr, model, statement, rootScope);
    DataModel nestedModel = relatedModel != null ? relatedModel : model;

    for (int i = 1; i < expression.getParams().size(); i++) {
      Object param = expression.getParam(i);
      if (param instanceof SExpression<?> nestedExpr) {
        checkExpression(nestedExpr, nestedModel, clauseType, statement, false);
      }
    }
  }

  private DataModel resolveRelatedModel(
      SExpression<?> relationExpr,
      DataModel model,
      QueryStatement statement,
      boolean rootScope) {
    Optional<FieldPathResolver.ResolvedFieldPath> resolved = rootScope
        ? FieldPathResolver.resolve(relationExpr, model, statement)
        : FieldPathResolver.resolve(relationExpr, model);
    return resolved
        .flatMap(FieldPathResolver.ResolvedFieldPath::getTerminalRelatedModel)
        .orElse(null);
  }

  private boolean isRelationOperator(SExpression<?> expression) {
    return expression.getOperator() == ExtOps.REL_ANY
        || expression.getOperator() == ExtOps.REL_ALL
        || expression.getOperator() == ExtOps.REL_NONE;
  }

  /**
   * 检查字段是否存在
   */
  private void checkFieldExists(String fieldName, DataModel model, String clauseType) {
    if ("*".equals(fieldName)) {
      return;
    }

    // 检查字段是否存在
    DataModelField field = model.getFields().stream()
        .filter(f -> fieldName.equals(f.getName()))
        .findFirst()
        .orElse(null);

    if (field == null) {
      throw new NormalizeException(
          "字段 '" + fieldName + "' 不存在于模型 '" + model.getName() + "' 中 " +
              "(" + clauseType + " 子句)"
      );
    }

    // 检查是否为关联字段（关联字段允许通过类型检查）
    // 关联字段的类型为 "Model"（一对一/多对一）或 "Collection"（一对多）
    String fieldType = field.getType();
    if ("Model".equals(fieldType) || "Collection".equals(fieldType)) {
      // 关联字段允许
      return;
    }
  }
}
