package com.ouroboros.data.transpile.clausebuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.DefaultOuroborosQueryMetadata;
import com.ouroboros.data.transpile.DummyTranspileContext;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.QueryTranspiler;
import com.ouroboros.data.transpile.TranspileContext;

@DisplayName("UnionBuilder 测试")
class UnionBuilderTest {

  @Test
  @DisplayName("UNION 应写入 unions，UNION ALL 应写入 unionAlls")
  void shouldClassifyUnionAndUnionAllIntoDifferentBuckets() {
    QueryStatement unionQuery = QueryStatement.builder().from("users").build();
    QueryStatement unionAllQuery = QueryStatement.builder().from("departments").build();
    QueryStatement statement = QueryStatement.builder()
        .from("users")
        .union(unionQuery)
        .unionAll(unionAllQuery)
        .build();

    DefaultOuroborosQueryMetadata distinctMetadata = new DefaultOuroborosQueryMetadata();
    DefaultOuroborosQueryMetadata allMetadata = new DefaultOuroborosQueryMetadata();
    QueryTranspiler queryTranspiler = (query, context) -> Try.success(
        query == unionQuery ? distinctMetadata : allMetadata);
    TranspileContext context = new DummyTranspileContext() {
      @Override
      public QueryTranspiler getQueryTranspiler() {
        return queryTranspiler;
      }
    };
    DefaultOuroborosQueryMetadata metadata = new DefaultOuroborosQueryMetadata();

    Try<?> result = new UnionBuilder().applyWithContext(statement, metadata, context);

    assertTrue(result.isSuccess(), () -> "UNION 子句转译应成功: "
        + (result.isFailure() ? result.getCause().getMessage() : ""));
    assertEquals(1, metadata.getUnions().size(), "UNION 应进入 unions");
    assertEquals(1, metadata.getUnionAlls().size(), "UNION ALL 应进入 unionAlls");
    assertTrue(metadata.getUnions().contains(distinctMetadata), "UNION 子查询不应被错误放入 unionAlls");
    assertTrue(metadata.getUnionAlls().contains(allMetadata), "UNION ALL 子查询不应被错误放入 unions");
  }
}
