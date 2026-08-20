package com.ouroboros.data.normalize.normalizers;

import java.util.*;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;

/**
 * WITH子句（CTE - Common Table Expression）规范化器（上下文感知版本）
 * <p>
 * 支持：WITH（普通CTE）和 WITH RECURSIVE（递归CTE）
 */
public class DefaultWithNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "WITH".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    List<QueryStatement.CTEDefinition> allCteDefinitions = new ArrayList<>();

    // 处理 WITH（普通CTE）
    Object withRaw = Keyword.WITH.findIn(clauseData);
    if (withRaw != null) {
      List<QueryStatement.CTEDefinition> withCtes = buildCteDefinitions("WITH", withRaw, false, context);
      allCteDefinitions.addAll(withCtes);
    }

    // 处理 WITH RECURSIVE（递归CTE）
    Object withRecursiveRaw = Keyword.WITH_RECURSIVE.findIn(clauseData);

    if (withRecursiveRaw != null) {
      List<QueryStatement.CTEDefinition> recursiveCtes = buildCteDefinitions("WITH RECURSIVE", withRecursiveRaw, true, context);
      allCteDefinitions.addAll(recursiveCtes);
    }

    if (!allCteDefinitions.isEmpty()) {
      builder.with(allCteDefinitions);
    }

    return builder;
  }

  /**
   * 构建CTE定义列表
   */
  private List<QueryStatement.CTEDefinition> buildCteDefinitions(String clauseName, Object raw, boolean recursive, ClauseNormalizeContext context) {
    if (raw == null) {
      return Collections.emptyList();
    }

    List<QueryStatement.CTEDefinition> definitions = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    // Map格式：{alias1: subQuery1, alias2: subQuery2, ...}
    if (raw instanceof QueryStatement.CTEDefinition definition) {
      definitions.add(definition);
    } else if (raw instanceof Map<?, ?> withMap) {
      for (Map.Entry<?, ?> entry : withMap.entrySet()) {
        try {
          QueryStatement.CTEDefinition cte = buildSingleCte(clauseName, entry.getKey(), entry.getValue(), recursive, context);
          definitions.add(cte);
        } catch (Exception e) {
          errors.add(String.format("%s 定义 '%s' 规范化失败: %s",
              clauseName, entry.getKey(), e.getMessage()));
        }
      }
    }
    // List格式：[{alias1: subQuery1}, {alias2: subQuery2}, ...]
    else if (raw instanceof Collection<?> withList) {
      int index = 0;
      for (Object item : withList) {
        if (item instanceof QueryStatement.CTEDefinition definition) {
          definitions.add(definition);
        } else if (item instanceof Map<?, ?> itemMap) {
          for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
            try {
              QueryStatement.CTEDefinition cte = buildSingleCte(clauseName, entry.getKey(), entry.getValue(), recursive, context);
              definitions.add(cte);
            } catch (Exception e) {
              errors.add(String.format("%s 第%d个定义 '%s' 规范化失败: %s",
                  clauseName, index + 1, entry.getKey(), e.getMessage()));
            }
          }
        } else {
          errors.add(String.format("%s 第%d个项必须是Map，但得到: %s",
              clauseName, index + 1, item.getClass().getName()));
        }
        index++;
      }
    } else {
      throw new NormalizeException(clauseName + " 子句格式错误，必须是Map或List");
    }

    // 如果有错误，抛出异常
    if (!errors.isEmpty()) {
      throw new NormalizeException(clauseName + " 子句存在错误:\n" + String.join("\n", errors));
    }

    return definitions;
  }

  /**
   * 构建单个CTE定义
   */
  @SuppressWarnings("unchecked")
  private QueryStatement.CTEDefinition buildSingleCte(String clauseName, Object aliasObj, Object subQueryObj,
                                                      boolean recursive, ClauseNormalizeContext context) {
    // 验证别名
    if (!(aliasObj instanceof String alias)) {
      throw new NormalizeException(clauseName + " 别名必须是String，但得到: " + aliasObj.getClass().getName());
    }

    if (alias.isEmpty()) {
      throw new NormalizeException(clauseName + " 别名不能为空");
    }

    if (alias.contains(".")) {
      throw new NormalizeException(clauseName + " 别名不能包含点号: " + alias);
    }

    // 验证并规范化子查询
    if (!(subQueryObj instanceof Map<?, ?>)) {
      throw new NormalizeException(clauseName + " 定义 '" + alias + "' 的值必须是Map（子查询），但得到: "
          + subQueryObj.getClass().getName());
    }

    Map<String, ?> subQueryMap = castToStringKeyMap((Map<?, ?>) subQueryObj, clauseName, alias);

    // 递归规范化子查询
    try {
      QueryStatement subQuery = context.getQueryContext().normalizeQuery(subQueryMap).get();
      return new QueryStatement.CTEDefinition(subQuery, alias, recursive);
    } catch (Exception e) {
      throw new NormalizeException(clauseName + " 定义 '" + alias + "' 的子查询规范化失败: "
          + e.getMessage(), e);
    }
  }

  /**
   * 将Map转换为String key的Map
   */
  @SuppressWarnings("unchecked")
  private Map<String, ?> castToStringKeyMap(Map<?, ?> map, String clauseName, String alias) {
    // 检查所有key是否为String
    for (Object key : map.keySet()) {
      if (!(key instanceof String)) {
        throw new NormalizeException(clauseName + " 定义 '" + alias + "' 的子查询key必须是String，但得到: "
            + key.getClass().getName());
      }
    }
    return (Map<String, ?>) map;
  }
}
