package com.ouroboros.data.transpile;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.querydsl.core.types.dsl.Expressions;

import io.vavr.control.Try;

@DisplayName("SubqueryTranspileContext Test")
class SubqueryTranspileContextTest {

  @Test
  @DisplayName("应透传父上下文的 QueryTranspiler")
  void shouldDelegateQueryTranspilerToParent() {
    FieldSource mainTable = mock(FieldSource.class);
    doReturn("users").when(mainTable).getName();
    doReturn(Expressions.path(Object.class, "users")).when(mainTable).getSelfPath();

    QueryTranspiler customTranspiler = (query, context) ->
        Try.success(mock(OuroborosQueryMetadata.class));
    TranspileContext parent = new BaseTranspileContext(mainTable, "u", customTranspiler);

    SubqueryTranspileContext subqueryContext = new SubqueryTranspileContext(parent);

    assertSame(customTranspiler, subqueryContext.getQueryTranspiler());
  }
}
