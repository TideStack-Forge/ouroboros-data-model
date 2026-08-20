package com.ouroboros.data.normalize.normalizers;

import java.util.Map;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;

/**
 * DISTINCT子句规范化器（上下文感知版本）
 */
public class DefaultDistinctNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "DISTINCT".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    Object distinct = Keyword.DISTINCT.findIn(clauseData);

    boolean isDistinct;
    if (distinct instanceof Boolean b) {
      isDistinct = b;
    } else if (distinct instanceof Number n) {
      isDistinct = !n.equals(0);
    } else if (distinct instanceof String s) {
      isDistinct = "true".equalsIgnoreCase(s) || "1".equals(s);
    } else {
      isDistinct = false;
    }

    return builder.distinct(isDistinct);
  }
}
