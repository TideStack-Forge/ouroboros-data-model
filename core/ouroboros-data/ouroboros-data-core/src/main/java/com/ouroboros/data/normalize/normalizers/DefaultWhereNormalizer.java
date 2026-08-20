package com.ouroboros.data.normalize.normalizers;

import java.util.Map;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;

/**
 * WHERE子句规范化器（上下文感知版本）
 * <p>
 * 对应旧代码的 WhereNormalizer，保持逻辑严密性并增加上下文感知
 */
public class DefaultWhereNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    // 支持QUERY级别的规范化（提取where子句）
    return "QUERY".equalsIgnoreCase(clauseType) || "WHERE".equalsIgnoreCase(clauseType);
  }

  @Override
  @SuppressWarnings("unchecked")
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    // 获取where子句数据
    Object where = Keyword.WHERE.findIn(clauseData);
    if (where == null) {
      // 没有where子句，直接返回
      return builder;
    }

    // 如果已经是SExpression，直接使用
    if (where instanceof SExpression<?> whereExpr) {
      // 类型检查：必须是Boolean类型或Void类型
      if (!whereExpr.getDataType().isAssignableFrom(Boolean.class) &&
          !whereExpr.getDataType().isAssignableFrom(Void.class)) {
        throw new NormalizeException("WHERE 子句必须是 Boolean 类型的表达式，但得到: "
            + whereExpr.getDataType().getName());
      }
      return builder.where((SExpression<Boolean>) whereExpr);
    }

    SExpression<Boolean> whereCondition = context.normalizeCondition(where, "where").get();

    // 再次检查类型（防御性编程）
    if (!whereCondition.getDataType().isAssignableFrom(Boolean.class) &&
        !whereCondition.isEmpty()) {
      throw new NormalizeException("规范化后的 WHERE 子句不是 Boolean 类型: "
          + whereCondition.getDataType().getName());
    }

    return builder.where(whereCondition);
  }
}
