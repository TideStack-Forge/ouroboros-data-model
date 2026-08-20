package com.ouroboros.data.normalize.normalizers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.QueryNormalizeContext;

class DefaultWithNormalizerTest {

  private final DefaultWithNormalizer normalizer = new DefaultWithNormalizer();

  @Test
  void normalizeTopLevelCteDefinitionPreservesCanonicalDefinition() {
    QueryStatement cteQuery = QueryStatement.builder()
        .from("users", "u")
        .build();
    QueryStatement.CTEDefinition canonicalDefinition =
        new QueryStatement.CTEDefinition(cteQuery, "active_users", false);
    Map<String, Object> clauseData = new LinkedHashMap<>();
    clauseData.put(Keyword.WITH.toString(), canonicalDefinition);

    QueryStatement statement = normalize(clauseData);

    assertEquals(1, statement.getWith().size());
    assertSame(canonicalDefinition, statement.getWith().get(0));
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
