package com.ouroboros.data.util;

import java.util.Map;
import java.util.TreeMap;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.StatementPredicates;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ClauseNormalizeContext;

public final class NormalizeUtils {

  private static Try<QueryStatement.TableSource> buildSimpleTableSource(String name, String alias) {
    return Try.of(() -> new QueryStatement.TableSource(name, alias));
  }

  public static Try<QueryStatement.TableSource> buildTableSource(String entryString) {
    var pair = entryString.split("(?i)( as | )");
    return Try.of(() -> {
      if (pair.length > 2) {
        throw new NormalizeException("表名格式错误");
      }
      var name = pair[0].trim();
      if (pair.length == 2) {
        if (pair[1].contains(".")) {
          throw new NormalizeException("表别名不能包含点号");
        }
        return buildSimpleTableSource(pair[0], pair[1]).getOrElseThrow(e -> e);
      }
      var alias = name.replaceAll("\\.", "_");
      return buildSimpleTableSource(pair[0], alias).getOrElseThrow(e -> e);
    });
  }

  /**
   * 将 Map 转换为 Entry
   * <p>
   * Map有三种格式：
   * 1. Map为子查询，含有AS字段代表别名
   * 2. 以字符串为键，字符串为值的 键为别名，值为表名
   * 3. 以字符串为键，Map为值的 键为别名，值为子查询
   *
   * @param entryMap
   * @param context
   * @return
   */
  public static Try<QueryStatement.TableSource> buildTableSource(Map<?, ?> entryMap, ClauseNormalizeContext context) {
    Map<String, Object> entryStrKeyMap = new TreeMap<>(String::compareToIgnoreCase);
    entryMap.forEach((key, value) -> entryStrKeyMap.put(key.toString(), value));

    // 格式1: Map为子查询，含有AS字段代表别名
    if (StatementPredicates.isQueryMap.test(entryStrKeyMap) && entryStrKeyMap.containsKey(Keyword.AS.toString())) {
      var subQuery = context.getQueryContext().normalizeQuery(entryStrKeyMap);
      if (subQuery.isFailure()) {
        return (Try) subQuery;
      }
      var as = entryStrKeyMap.get(Keyword.AS.toString());
      if (as == null) {
        return Try.failure(new NormalizeException("子查询必须要有别名"));
      } else if (!(as instanceof String alias)) {
        return Try.failure(new NormalizeException("子查询别名必须是字符串"));
      } else if (alias.contains(".")) {
        return Try.failure(new NormalizeException("表别名不能包含点号"));
      } else {
        return Try.success(new QueryStatement.TableSource(subQuery.get(), alias));
      }
    }

    // 格式2、3
    if (entryStrKeyMap.size() == 1) {
      var alias = entryStrKeyMap.keySet().iterator().next();
      var origin = entryStrKeyMap.get(alias);
      // 格式2：以字符串为键，字符串为值的 键为别名，值为表名
      if (origin instanceof String originName) {
        return buildSimpleTableSource(originName, alias);
      }
      // 格式3：以字符串为键，Map为值的 键为别名，值为子查询
      else if (origin instanceof Map originMap && StatementPredicates.isQueryMap.test(originMap)) {
        var subQuery = context.getQueryContext().normalizeQuery((Map<String, ?>) originMap);
        return subQuery.map(query -> new QueryStatement.TableSource(query, alias));
      }
    }

    return Try.failure(new NormalizeException("From/Join 子句错误，不兼容的类型"));
  }

}
