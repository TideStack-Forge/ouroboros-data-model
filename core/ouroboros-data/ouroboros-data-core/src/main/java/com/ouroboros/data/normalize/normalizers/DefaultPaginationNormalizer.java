package com.ouroboros.data.normalize.normalizers;

import java.util.Map;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;
import com.ouroboros.data.util.DataConverters;

/**
 * 分页子句规范化器（上下文感知版本）
 * <p>
 * 支持多种分页方式：
 * - page/pageSize (或 pageNum/perPage)
 * - offset/limit
 * - skip/limit
 */
public class DefaultPaginationNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "PAGINATION".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    // 优先处理page/pageSize分页参数
    Object pageRaw = Keyword.PAGE.findIn(clauseData);
    Long page = pageRaw != null ? DataConverters.toLong(pageRaw) : null;

    Object pageSizeRaw = Keyword.PAGESIZE.findIn(clauseData);
    Long pageSize = pageSizeRaw != null ? DataConverters.toLong(pageSizeRaw) : null;

    if (page != null && pageSize != null) {
      // 将page/pageSize转换为offset/limit
      builder.offset((page - 1) * pageSize);
      builder.limit(pageSize);
    }

    // offset/limit 或 skip/limit 会覆盖之前的分页参数
    Object offsetRaw = Keyword.OFFSET.findIn(clauseData);
    Object limitRaw = Keyword.LIMIT.findIn(clauseData);

    if (offsetRaw != null) {
      builder.offset(DataConverters.toLong(offsetRaw));
    }
    if (limitRaw != null) {
      builder.limit(DataConverters.toLong(limitRaw));
    }

    return builder;
  }
}
