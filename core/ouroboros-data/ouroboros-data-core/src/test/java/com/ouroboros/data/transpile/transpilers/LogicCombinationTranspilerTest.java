package com.ouroboros.data.transpile.transpilers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.DummyTranspileContext;
import com.ouroboros.data.transpile.FieldSource;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.Expressions;

class LogicCombinationTranspilerTest {

  private TranspileContext context;

  @BeforeEach
  void setUp() {
    FieldSource source = mock(FieldSource.class);
    Path<?> idPath = Expressions.numberPath(Long.class, "id");
    when(source.getField("id")).thenReturn(Optional.of(idPath));
    context = new DummyTranspileContext().withTable("test", source);
  }

  @Test
  @DisplayName("NOT 应保留一元逻辑操作符，而不是退化成内部条件")
  void apply_notShouldWrapInnerPredicate() {
    SExpression<Boolean> expr = SExpression.create(
        Operators.NOT,
        SExpression.create(
            Operators.EQ,
            SExpression.field("id"),
            SExpression.constant(1L)
        )
    );

    Try<Expression<?>> result = new LogicCombinationTranspiler().apply(expr, context);

    assertTrue(result.isSuccess(), () -> "NOT 转译应成功: "
        + (result.isFailure() ? result.getCause().getMessage() : ""));
    Operation<?> operation = (Operation<?>) result.get();
    assertEquals(Ops.NOT, operation.getOperator());
  }
}
