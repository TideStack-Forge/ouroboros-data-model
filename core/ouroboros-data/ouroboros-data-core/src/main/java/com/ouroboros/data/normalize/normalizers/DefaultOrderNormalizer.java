package com.ouroboros.data.normalize.normalizers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Order;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;

/**
 * ORDER BY子句规范化器（上下文感知版本）
 */
public class DefaultOrderNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "ORDER".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    Object orderRaw = Keyword.ORDER.findIn(clauseData);

    Stream<QueryStatement.OrderEntry> orderStream;
    if (orderRaw instanceof QueryStatement.OrderEntry orderEntry) {
      orderStream = Stream.of(orderEntry);
    } else if (orderRaw instanceof String orderStr) {
      orderStream = parseOrderEntry(orderStr);
    } else if (orderRaw instanceof List<?> orderList) {
      orderStream = parseOrderEntry(orderList);
    } else if (orderRaw instanceof Map<?, ?> orderMap) {
      orderStream = parseOrderEntry(orderMap);
    } else {
      orderStream = Stream.empty();
    }

    List<QueryStatement.OrderEntry> orderEntries = orderStream.collect(Collectors.toList());
    builder.order(orderEntries);
    return builder;
  }

  /**
   * 解析字符串形式的排序：field1 asc, field2 desc, field3
   */
  private Stream<QueryStatement.OrderEntry> parseOrderEntry(String orderStr) {
    return Arrays.stream(orderStr.split(","))
        .map(String::trim)
        .map(s -> {
          String[] pair = s.split(" ");
          if (pair.length == 1) {
            return new QueryStatement.OrderEntry(pair[0], Order.ASC);
          } else if (pair.length == 2) {
            return new QueryStatement.OrderEntry(pair[0], pair[1]);
          }
          return null;
        })
        .filter(Objects::nonNull);
  }

  /**
   * 解析Map形式的排序：{field1: "asc", field2: "desc"}
   */
  private Stream<QueryStatement.OrderEntry> parseOrderEntry(Map<?, ?> orderMap) {
    return orderMap.entrySet().stream()
        .map(entry -> {
          Object key = entry.getKey();
          Object value = entry.getValue();
          if (key instanceof String fieldName) {
            if (value instanceof String orderStr) {
              return new QueryStatement.OrderEntry(fieldName, orderStr);
            } else if (value instanceof Order order) {
              return new QueryStatement.OrderEntry(fieldName, order);
            }
            return new QueryStatement.OrderEntry(fieldName, Order.ASC);
          }
          return null;
        })
        .filter(Objects::nonNull);
  }

  /**
   * 解析List形式的排序：[{field1: "asc"}, "field2 desc", {field3: "asc"}]
   */
  private Stream<QueryStatement.OrderEntry> parseOrderEntry(List<?> orderList) {
    return orderList.stream()
        .flatMap(item -> {
          if (item instanceof QueryStatement.OrderEntry orderEntry) {
            return Stream.of(orderEntry);
          } else if (item instanceof String orderStr) {
            return parseOrderEntry(orderStr);
          } else if (item instanceof Map<?, ?> orderMap) {
            return parseOrderEntry(orderMap);
          }
          return Stream.empty();
        });
  }
}
