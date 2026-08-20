package com.ouroboros.data.transpile.transpilers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.DummyTranspileContext;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.QueryTranspiler;
import com.ouroboros.data.transpile.SubqueryTranspileContext;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Operation;

class ExistsTranspilerTest {

  @Test
  @DisplayName("EXISTS 应接受 QueryStatement 参数并委托子查询转译器")
  void apply_shouldTranspileQueryStatementParam() {
    QueryStatement subQuery = QueryStatement.builder()
        .from("t_order_item")
        .select(SExpression.field("id"))
        .build();
    QueryTranspiler queryTranspiler = mock(QueryTranspiler.class);
    OuroborosQueryMetadata metadata = mock(OuroborosQueryMetadata.class);
    TranspileContext context = new DummyTranspileContext() {
      @Override
      public QueryTranspiler getQueryTranspiler() {
        return queryTranspiler;
      }
    };
    when(queryTranspiler.applyWithContext(eq(subQuery), any(SubqueryTranspileContext.class)))
        .thenReturn(Try.success(metadata));

    SExpression<?> exists = SExpression.create(Operators.EXISTS, subQuery);

    Try<Expression<?>> result = new ExistsTranspiler().apply(exists, context);

    assertTrue(result.isSuccess(), () -> "EXISTS 转译应成功: "
        + (result.isFailure() ? result.getCause().getMessage() : ""));
    Operation<?> operation = (Operation<?>) result.get();
    assertEquals(Ops.EXISTS, operation.getOperator());
    verify(queryTranspiler).applyWithContext(eq(subQuery), any(SubqueryTranspileContext.class));
  }
}
