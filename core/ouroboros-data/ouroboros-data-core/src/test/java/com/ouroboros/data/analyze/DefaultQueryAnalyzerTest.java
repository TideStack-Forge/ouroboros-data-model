package com.ouroboros.data.analyze;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;

class DefaultQueryAnalyzerTest {

  @Test
  void analyzeShouldNotRewriteStatement() {
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(SExpression.create(
            Operators.AND,
            SExpression.constant(Boolean.TRUE),
            SExpression.create(
                Operators.EQ,
                SExpression.create(Operators.FIELD, "status"),
                SExpression.constant("active")
            )
        ))
        .build();

    QueryAnalyzeContext context = QueryAnalyzeContext.builder().build();

    Try<QueryStatement> result = new DefaultQueryAnalyzer().analyze(statement, context);

    assertTrue(result.isSuccess());
    assertSame(statement, result.get(),
        "validator-only analyzer 不应重写 statement");
  }

  @Test
  void analyzeShouldRespectDisabledTypeCheckingFlag() {
    DataModel model = mock(DataModel.class);
    when(model.getName()).thenReturn("user");
    when(model.getFields()).thenReturn(Arrays.asList());

    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "missingField"),
            SExpression.constant("active")
        ))
        .build();

    QueryAnalyzeContext context = QueryAnalyzeContext.builder()
        .model(model)
        .enableTypeChecking(false)
        .build();

    Try<QueryStatement> result = new DefaultQueryAnalyzer().analyze(statement, context);

    assertTrue(result.isSuccess());
    assertSame(statement, result.get(),
        "关闭 enableTypeChecking 后不应执行 TypeCheckAnalyzer");
  }

  @Test
  void customAnalyzerConstructorShouldUseProvidedChain() {
    QueryAnalyzer first = mock(QueryAnalyzer.class);
    QueryAnalyzer second = mock(QueryAnalyzer.class);
    QueryStatement statement = QueryStatement.builder().from("user").build();
    QueryAnalyzeContext context = QueryAnalyzeContext.builder().build();

    when(first.supports(context)).thenReturn(true);
    when(second.supports(context)).thenReturn(true);
    when(first.analyze(statement, context)).thenReturn(Try.success(statement));
    when(second.analyze(statement, context)).thenReturn(Try.success(statement));

    DefaultQueryAnalyzer analyzer = new DefaultQueryAnalyzer(Arrays.asList(first, second));
    Try<QueryStatement> result = analyzer.analyze(statement, context);

    assertTrue(result.isSuccess());
    verify(first).analyze(statement, context);
    verify(second).analyze(statement, context);
    assertEquals(2, analyzer.getAnalyzers().size());
    assertNotSame(analyzer.getAnalyzers(), analyzer.getAnalyzers(),
        "getAnalyzers 应返回防御性拷贝");
  }
}
