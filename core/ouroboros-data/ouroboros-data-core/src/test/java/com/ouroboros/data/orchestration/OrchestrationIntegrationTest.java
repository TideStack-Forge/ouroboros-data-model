package com.ouroboros.data.orchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.record.RecordList;

/**
 * Orchestration 集成测试
 *
 * @author Claude Code
 */
class OrchestrationIntegrationTest {

  private QueryOrchestrator orchestrator;
  private DataModel model;

  @BeforeEach
  void setUp() {
    orchestrator = new DefaultQueryOrchestrator();
    model = mock(DataModel.class);
    when(model.getName()).thenReturn("TestModel");
  }

  @Test
  void testEndToEndOrchestration() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RecordList expectedResult = RecordList.empty();

    MainQueryExecutor executor = stmt -> Try.success(expectedResult);
    OrchestrationContext context = new OrchestrationContext();

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then
    assertTrue(result.isSuccess());
    assertSame(expectedResult, result.get());
    assertSame(statement, context.getMainStatement());
    assertTrue(context.hasResult("main"));
  }

  @Test
  void testWithRealQueryStatement() {
    // Given
    HashMap<String, Object> statementMap = new HashMap<>();
    statementMap.put("select", new ArrayList<>());
    QueryStatement statement = new QueryStatement(statementMap);

    RecordList expectedResult = RecordList.empty();

    MainQueryExecutor executor = stmt -> {
      assertNotNull(stmt);
      return Try.success(expectedResult);
    };

    OrchestrationContext context = new OrchestrationContext();

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then
    assertTrue(result.isSuccess());
    assertSame(expectedResult, result.get());
  }

  @Test
  void testContextStateTransition() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RecordList expectedResult = RecordList.empty();

    MainQueryExecutor executor = stmt -> Try.success(expectedResult);
    OrchestrationContext context = new OrchestrationContext();

    // Verify initial state
    assertNull(context.getMainStatement());
    assertFalse(context.hasResult("main"));

    // When
    orchestrator.orchestrate(statement, model, executor, context);

    // Then - verify state transition
    assertNotNull(context.getMainStatement());
    assertTrue(context.hasResult("main"));
    assertSame(expectedResult, context.getResult("main"));
  }

  @Test
  void testExecutorFailurePropagation() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RuntimeException cause = new RuntimeException("Executor failed");

    MainQueryExecutor executor = stmt -> Try.failure(cause);
    OrchestrationContext context = new OrchestrationContext();

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then
    assertTrue(result.isFailure());
    assertNotNull(result.getCause());
  }
}
