package com.ouroboros.data.normalize.normalizers;

import java.util.*;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;

/**
 * UNION子句规范化器（上下文感知版本）
 * <p>
 * 支持：UNION（去重）和 UNION ALL（保留重复）
 */
public class DefaultUnionNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "UNION".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    // 处理 UNION
    Object unionRaw = Keyword.UNION.findIn(clauseData);
    if (unionRaw != null) {
      appendUnionEntries(builder, "UNION", unionRaw, false, context);
    }

    // 处理 UNION ALL
    Object unionAllRaw = Keyword.UNION_ALL.findIn(clauseData);

    if (unionAllRaw != null) {
      appendUnionEntries(builder, "UNION ALL", unionAllRaw, true, context);
    }

    return builder;
  }

  private void appendUnionEntries(QueryStatement.QueryStatementBuilder builder,
                                  String clauseName,
                                  Object raw,
                                  boolean all,
                                  ClauseNormalizeContext context) {
    if (raw instanceof QueryStatement.UnionEntry entry) {
      appendUnionEntry(builder, entry);
      return;
    }
    if (raw instanceof Collection<?> collection) {
      for (Object item : collection) {
        if (item instanceof QueryStatement.UnionEntry entry) {
          appendUnionEntry(builder, entry);
        } else {
          appendRawUnionEntry(builder, clauseName, item, all, context);
        }
      }
      return;
    }
    appendRawUnionEntry(builder, clauseName, raw, all, context);
  }

  private void appendRawUnionEntry(QueryStatement.QueryStatementBuilder builder,
                                   String clauseName,
                                   Object raw,
                                   boolean all,
                                   ClauseNormalizeContext context) {
    List<QueryStatement> queries = normalizeUnionQueries(clauseName, raw, context);
    queries.forEach(query -> {
      if (all) {
        builder.unionAll(query);
      } else {
        builder.union(query);
      }
    });
  }

  private void appendUnionEntry(QueryStatement.QueryStatementBuilder builder,
                                QueryStatement.UnionEntry entry) {
    if (Boolean.TRUE.equals(entry.isAll())) {
      builder.unionAll(entry.getQuery());
    } else {
      builder.union(entry.getQuery());
    }
  }

  /**
   * 规范化UNION查询列表
   */
  private List<QueryStatement> normalizeUnionQueries(String clauseName, Object raw, ClauseNormalizeContext context) {
    if (raw == null) {
      return Collections.emptyList();
    }

    List<Map<String, ?>> subQueryMaps = new ArrayList<>();

    // 单个子查询
    if (raw instanceof Map<?, ?> subQueryMap) {
      subQueryMaps.add(castToStringKeyMap(subQueryMap, clauseName));
    }
    // 多个子查询
    else if (raw instanceof List<?> subQueryList) {
      for (Object item : subQueryList) {
        if (item instanceof Map<?, ?> itemMap) {
          subQueryMaps.add(castToStringKeyMap(itemMap, clauseName));
        } else {
          throw new NormalizeException(clauseName + " 子句中的项必须是Map（子查询），但得到: "
              + item.getClass().getName());
        }
      }
    } else {
      throw new NormalizeException(clauseName + " 子句格式错误，必须是Map或List");
    }

    // 规范化每个子查询
    List<QueryStatement> results = new ArrayList<>();

    for (int i = 0; i < subQueryMaps.size(); i++) {
      Map<String, ?> subQueryMap = subQueryMaps.get(i);
      // 递归规范化子查询
      QueryStatement subQuery = context.getQueryContext().normalizeQuery(subQueryMap).get();
      results.add(subQuery);
    }

    return results;
  }

  /**
   * 将Map转换为String key的Map
   */
  @SuppressWarnings("unchecked")
  private Map<String, ?> castToStringKeyMap(Map<?, ?> map, String clauseName) {
    // 检查所有key是否为String
    for (Object key : map.keySet()) {
      if (!(key instanceof String)) {
        throw new NormalizeException(clauseName + " 子查询的key必须是String，但得到: "
            + key.getClass().getName());
      }
    }
    return (Map<String, ?>) map;
  }
}
