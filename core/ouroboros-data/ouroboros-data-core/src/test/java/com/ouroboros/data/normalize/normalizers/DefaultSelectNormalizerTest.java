package com.ouroboros.data.normalize.normalizers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.QueryNormalizeContext;

class DefaultSelectNormalizerTest {

  private final DefaultSelectNormalizer normalizer = new DefaultSelectNormalizer();

  @Test
  void normalizeTopLevelSExpressionSelectPreservesCanonicalExpression() {
    SExpression<?> canonicalSelect = SExpression.field("users", "name");
    Map<String, Object> clauseData = new LinkedHashMap<>();
    clauseData.put(Keyword.SELECT.toString(), canonicalSelect);

    QueryStatement statement = normalize(clauseData);

    List<SExpression<?>> select = statement.getSelect();
    assertEquals(1, select.size());
    assertSame(canonicalSelect, select.get(0));
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
