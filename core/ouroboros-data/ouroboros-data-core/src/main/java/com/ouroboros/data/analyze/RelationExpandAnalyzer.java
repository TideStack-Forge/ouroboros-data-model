package com.ouroboros.data.analyze;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;

/**
 * 关联展开验证分析器
 *
 * <p>验证多参数 FIELD 表达式的关联路径是否有效。
 *
 * <p>示例：
 * <pre>{@code
 * FIELD("user", "name")                        // 验证: user 是关联字段
 * FIELD("user", "department", "company", "name") // 验证: user, department, company 都是关联字段
 * }</pre>
 *
 * @since 1.0.0-beta.2
 */
public class RelationExpandAnalyzer implements QueryAnalyzer {

  @Override
  public boolean supports(QueryAnalyzeContext context) {
    return context.getModel() != null;
  }

  @Override
  public Try<QueryStatement> analyze(QueryStatement statement, QueryAnalyzeContext context) {
    return Try.of(() -> {
      // 验证 WHERE 子句
      if (statement.getWhere() != null && !statement.getWhere().isEmpty()) {
        validateExpression(statement.getWhere(), context.getModel(), statement);
      }

      // 验证 HAVING 子句
      if (statement.getHaving() != null && !statement.getHaving().isEmpty()) {
        validateExpression(statement.getHaving(), context.getModel(), statement);
      }

      // 验证 SELECT 子句
      for (SExpression<?> selectExpr : statement.getSelect()) {
        if (selectExpr != null && !selectExpr.isEmpty()) {
          validateExpression(selectExpr, context.getModel(), statement);
        }
      }

      return statement;
    });
  }

  /**
   * 递归验证表达式中的多参数 FIELD
   */
  private void validateExpression(SExpression<?> expr, DataModel rootModel, QueryStatement statement) {
    if (expr == null || expr.isEmpty()) {
      return;
    }

    expr.walk((e, context) -> {
      if (e.getOperator() == Operators.FIELD && e.getParams().size() > 1) {
        FieldPathValidator.validateFieldPath(
            e,
            rootModel,
            null,
            statement);
      }
    });
  }
}
