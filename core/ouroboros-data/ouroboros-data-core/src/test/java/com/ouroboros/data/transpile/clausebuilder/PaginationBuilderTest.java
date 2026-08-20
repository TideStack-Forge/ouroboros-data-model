package com.ouroboros.data.transpile.clausebuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.DefaultOuroborosQueryMetadata;
import com.ouroboros.data.transpile.DummyTranspileContext;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.dsl.Expressions;

@DisplayName("PaginationBuilder 测试")
class PaginationBuilderTest {

  @Test
  @DisplayName("limit=0 应被视为空结果快捷路径，而不是传给 QueryDSL")
  void limitZeroShouldNotSetQuerydslLimit() {
    PaginationBuilder builder = new PaginationBuilder();
    QueryStatement query = QueryStatement.builder()
        .from("users")
        .limit(0)
        .build();
    OuroborosQueryMetadata metadata = new DefaultOuroborosQueryMetadata();
    TranspileContext context = new DummyTranspileContext();

    Try<?> result = builder.applyWithContext(query, metadata, context);

    assertTrue(result.isSuccess(), () -> "limit=0 不应在转译阶段失败: "
        + (result.isFailure() ? result.getCause().getMessage() : ""));
    assertNull(metadata.getModifiers().getLimit(), "limit=0 不应调用 QueryDSL setLimit(0)");
    assertEquals(Expressions.ONE.eq(Expressions.ZERO), metadata.getWhere(),
        "limit=0 应改写为空结果条件，避免退化成无界查询");
  }
}
