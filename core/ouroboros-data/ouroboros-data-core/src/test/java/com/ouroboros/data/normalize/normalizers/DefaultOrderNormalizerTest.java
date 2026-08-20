package com.ouroboros.data.normalize.normalizers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Order;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.QueryNormalizeContext;

class DefaultOrderNormalizerTest {

  private final DefaultOrderNormalizer normalizer = new DefaultOrderNormalizer();

  @Test
  void normalizeMixedOrderListPreservesEveryEntryInOrder() {
    QueryStatement.OrderEntry canonicalEntry =
        new QueryStatement.OrderEntry("updatedAt", Order.DESC);
    Map<String, Object> clauseData = new LinkedHashMap<>();
    clauseData.put(
        Keyword.ORDER.toString(),
        List.of(canonicalEntry, "createdAt desc", Map.of("name", "asc")));

    QueryStatement statement = normalize(clauseData);

    List<QueryStatement.OrderEntry> orders = statement.getOrders();
    assertEquals(3, orders.size());
    assertSame(canonicalEntry, orders.get(0));
    assertEquals("updatedAt", orders.get(0).getColumn());
    assertEquals(Order.DESC, orders.get(0).getOrder());
    assertEquals("createdAt", orders.get(1).getColumn());
    assertEquals(Order.DESC, orders.get(1).getOrder());
    assertEquals("name", orders.get(2).getColumn());
    assertEquals(Order.ASC, orders.get(2).getOrder());
  }

  @Test
  void normalizeTopLevelOrderEntryPreservesCanonicalEntry() {
    QueryStatement.OrderEntry canonicalEntry =
        new QueryStatement.OrderEntry("updatedAt", Order.DESC);
    Map<String, Object> clauseData = new LinkedHashMap<>();
    clauseData.put(Keyword.ORDER.toString(), canonicalEntry);

    QueryStatement statement = normalize(clauseData);

    List<QueryStatement.OrderEntry> orders = statement.getOrders();
    assertEquals(1, orders.size());
    assertSame(canonicalEntry, orders.get(0));
  }

  private QueryStatement normalize(Map<String, Object> clauseData) {
    QueryNormalizeContext queryContext = QueryNormalizeContext.builder().build();
    ClauseNormalizeContext clauseContext = new ClauseNormalizeContext(queryContext, "QUERY");
    return normalizer.normalize(
        clauseData,
        QueryStatement.builder().from("users"),
        clauseContext
    ).build();
  }
}
