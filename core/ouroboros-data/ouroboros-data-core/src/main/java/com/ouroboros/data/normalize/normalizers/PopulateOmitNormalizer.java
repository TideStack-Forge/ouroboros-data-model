package com.ouroboros.data.normalize.normalizers;

import java.util.Map;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.statement.ModelQueryStatementBuilder;
import com.ouroboros.data.dsl.statement.OmitClause;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;

/**
 * POPULATE/OMIT子句规范化器
 * <p>
 * 从原始查询Map中提取POPULATE和OMIT子句，设置到ModelQueryStatementBuilder的类型化字段。
 * 当builder不是ModelQueryStatementBuilder时静默忽略（DataAdapter等低级API不处理POPULATE/OMIT）。
 */
public class PopulateOmitNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    Object populate = Keyword.POPULATE.findIn(clauseData);
    if (populate != null && builder instanceof ModelQueryStatementBuilder mqb) {
      mqb.populateClause(PopulateClause.fromRaw(populate));
    }
    Object omit = Keyword.OMIT.findIn(clauseData);
    if (omit != null && builder instanceof ModelQueryStatementBuilder mqb) {
      mqb.omitClause(OmitClause.fromRaw(omit));
    }
    return builder;
  }
}
