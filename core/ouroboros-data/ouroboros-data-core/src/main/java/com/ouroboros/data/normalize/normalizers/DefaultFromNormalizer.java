package com.ouroboros.data.normalize.normalizers;

import java.util.Map;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.query.QuerySource;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.exception.StatementError;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;
import com.ouroboros.data.util.NormalizeUtils;

/**
 * FROM子句规范化器（上下文感知版本）
 */
public class DefaultFromNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "FROM".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    Object from = Keyword.FROM.findIn(clauseData);

    if (from == null) {
      throw new StatementError("FROM 子句不能为空");
    }

    return builder.from(normalizeFrom(from, context));
  }

  private QueryStatement.TableSource normalizeFrom(Object from, ClauseNormalizeContext context) {
    if (from instanceof QuerySource source) {
      Object rawFrom = source.toRawFrom();
      if (rawFrom == from) {
        throw new NormalizeException("QuerySource 不能返回自身作为 raw FROM: "
            + from.getClass().getName());
      }
      if (rawFrom == null) {
        throw new NormalizeException("QuerySource 返回的 raw FROM 不能为空: "
            + from.getClass().getName());
      }
      return normalizeFrom(rawFrom, context);
    }

    // 已规范化的表源
    if (from instanceof QueryStatement.TableSource normalizedTableSource) {
      return normalizedTableSource;
    }
    // 表名字符串
    else if (from instanceof String fromString) {
      return NormalizeUtils.buildTableSource(fromString).get();
    }
    // 子查询或表Map（{alias: tableName} 或 {alias: subquery}）
    else if (from instanceof Map<?, ?> fromMap) {
      return NormalizeUtils.buildTableSource(fromMap, context).get();
    }
    // 不支持的类型
    else {
      throw new NormalizeException("FROM 子句类型不支持: " + from.getClass().getName()
          + ", 仅支持 QuerySource、TableSource、String 或 Map");
    }
  }
}
