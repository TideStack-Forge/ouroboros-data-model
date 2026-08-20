package com.ouroboros.data.orchestration.step;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.MainQueryExecutor;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.OrchestrationException;
import com.ouroboros.data.record.RecordList;

/**
 * MainQueryStep 单元测试
 *
 * @author Claude Code
 */
class MainQueryStepTest {

  private OrchestrationContext context;
  private MainQueryExecutor executor;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
    executor = mock(MainQueryExecutor.class);
  }

  @Test
  void testExecuteSuccess() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RecordList expectedResult = RecordList.empty();
    context.setMainStatement(statement);

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    MainQueryStep step = new MainQueryStep("main", executor);

    // When
    step.execute(context);

    // Then
    assertTrue(context.hasResult("main"));
    assertSame(expectedResult, context.getResult("main"));
  }

  @Test
  void testExecuteStoresResult() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RecordList expectedResult = RecordList.empty();
    context.setMainStatement(statement);

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    MainQueryStep step = new MainQueryStep("test-main", executor);

    // When
    step.execute(context);

    // Then
    assertTrue(context.hasResult("test-main"));
    assertSame(expectedResult, context.getResult("test-main"));
  }

  @Test
  void testExecuteFailure() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    context.setMainStatement(statement);

    RuntimeException cause = new RuntimeException("Query failed");
    when(executor.execute(any())).thenReturn(Try.failure(cause));

    MainQueryStep step = new MainQueryStep("main", executor);

    // When & Then
    OrchestrationException exception = assertThrows(
        OrchestrationException.class,
        () -> step.execute(context)
    );

    assertTrue(exception.getMessage().contains("Step execution failed"));
    assertNotNull(exception.getCause());
    assertTrue(exception.getCause().getMessage().contains("Main query execution failed"));
  }

  @Test
  void testExecuteWithNullStatement() {
    // Given
    context.setMainStatement(null);
    MainQueryStep step = new MainQueryStep("main", executor);

    // When & Then
    assertThrows(OrchestrationException.class, () -> step.execute(context));
  }
}
