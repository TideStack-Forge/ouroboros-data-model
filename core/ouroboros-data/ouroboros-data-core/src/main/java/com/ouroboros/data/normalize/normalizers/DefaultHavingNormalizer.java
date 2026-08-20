package com.ouroboros.data.normalize.normalizers;

import java.util.Map;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;

/**
 * HAVING子句规范化器（上下文感知版本）
 * <p>
 * HAVING子句类似WHERE，但允许使用聚合函数
 */
public class DefaultHavingNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "HAVING".equalsIgnoreCase(clauseType);
  }

  @Override
  @SuppressWarnings("unchecked")
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    Object havingRaw = Keyword.HAVING.findIn(clauseData);

    if (havingRaw == null) {
      return builder;
    }

    // 如果已经是SExpression，直接使用
    if (havingRaw instanceof SExpression<?> havingExpr) {
      // 类型检查：必须是Boolean类型或Void类型
      if (!havingExpr.getDataType().isAssignableFrom(Boolean.class) &&
          !havingExpr.getDataType().isAssignableFrom(Void.class)) {
        throw new NormalizeException("HAVING 子句必须是 Boolean 类型的表达式，但得到: "
            + havingExpr.getDataType().getName());
      }
      return builder.having((SExpression<Boolean>) havingExpr);
    }

    SExpression<Boolean> havingCondition = context.normalizeCondition(havingRaw, "having").get();

    // 再次检查类型（防御性编程）
    if (!havingCondition.getDataType().isAssignableFrom(Boolean.class) &&
        !havingCondition.isEmpty()) {
      throw new NormalizeException("规范化后的 HAVING 子句不是 Boolean 类型: "
          + havingCondition.getDataType().getName());
    }

    return builder.having(havingCondition);
  }
}
