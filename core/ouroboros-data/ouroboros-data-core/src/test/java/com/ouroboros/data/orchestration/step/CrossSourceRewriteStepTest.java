package com.ouroboros.data.orchestration.step;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.orchestration.MainQueryExecutor;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.OrchestrationException;
import com.ouroboros.data.record.RecordList;

/**
 * CrossSourceRewriteStep 测试
 */
class CrossSourceRewriteStepTest {

  private OrchestrationContext context;
  private QueryStatement mockPreQuery;
  private DataModel mockModel;
  private MainQueryExecutor mockExecutor;
  private RecordList mockResult;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
    mockPreQuery = mock(QueryStatement.class);
    mockModel = mock(DataModel.class);
    mockExecutor = mock(MainQueryExecutor.class);
    mockResult = RecordList.empty();

    when(mockModel.getName()).thenReturn("User");
  }

  @Test
  void testExecutePreQuery() {
    // Given: 预查询执行器返回成功
    when(mockExecutor.execute(mockPreQuery)).thenReturn(Try.success(mockResult));

    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When: 执行步骤
    step.execute(context);

    // Then: 预查询被执行，结果被存储
    verify(mockExecutor).execute(mockPreQuery);
    assertTrue(context.hasResult("preQuery"));
    assertEquals(mockResult, context.getResult("preQuery"));
  }

  @Test
  void testCreateRewriter() {
    // Given: 预查询执行器返回成功
    when(mockExecutor.execute(mockPreQuery)).thenReturn(Try.success(mockResult));

    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When: 执行步骤
    step.execute(context);

    // Then: 改写器被添加到 Context
    assertFalse(context.getStatementRewriters().isEmpty());
  }

  @Test
  void testStorePreQueryResult() {
    // Given: 预查询执行器返回成功
    when(mockExecutor.execute(mockPreQuery)).thenReturn(Try.success(mockResult));

    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When: 执行步骤
    step.execute(context);

    // Then: 预查询结果被存储
    assertTrue(context.hasResult("preQuery"));
    assertEquals(mockResult, context.getResult("preQuery"));
  }

  @Test
  void testPreQueryFailure() {
    // Given: 预查询执行器返回失败
    RuntimeException error = new RuntimeException("Query failed");
    when(mockExecutor.execute(mockPreQuery)).thenReturn(Try.failure(error));

    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When & Then: 执行步骤应该抛出异常
    assertThrows(OrchestrationException.class, () -> {
      step.execute(context);
    });
  }

  // ========== Step 3.1: Error handling and edge case tests ==========

  @Test
  void testExecutePreQueryFailure() {
    // Given: Pre-query executor returns failure
    RuntimeException error = new RuntimeException("Pre-query execution failed");
    when(mockExecutor.execute(mockPreQuery)).thenReturn(Try.failure(error));

    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When & Then: Should throw OrchestrationException
    OrchestrationException exception = assertThrows(OrchestrationException.class, () -> {
      step.execute(context);
    });
    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains("preQuery"));
  }

  @Test
  void testExecuteWithEmptyResult() {
    // Given: Pre-query returns empty RecordList
    RecordList emptyResult = RecordList.empty();
    when(mockExecutor.execute(mockPreQuery)).thenReturn(Try.success(emptyResult));

    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When: Execute step
    step.execute(context);

    // Then: Empty result stored in context
    assertTrue(context.hasResult("preQuery"));
    assertEquals(0, context.getResult("preQuery").size());
  }

  @Test
  void testExecuteWithLargeResult() {
    // Given: Pre-query returns large RecordList (1000+ records)
    List<Map<String, Object>> records = new ArrayList<>();
    for (int i = 0; i < 1500; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      records.add(record);
    }
    RecordList largeResult = RecordList.of(records);
    when(mockExecutor.execute(mockPreQuery)).thenReturn(Try.success(largeResult));

    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When: Execute step
    step.execute(context);

    // Then: All records stored in context
    assertTrue(context.hasResult("preQuery"));
    assertEquals(1500, context.getResult("preQuery").size());
  }

  @Test
  void testExecutePreQueryThrowsException() {
    // Given: Pre-query executor throws exception
    RuntimeException cause = new RuntimeException("Database connection failed");
    when(mockExecutor.execute(mockPreQuery)).thenReturn(Try.failure(cause));

    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When & Then: Should throw OrchestrationException with cause
    OrchestrationException exception = assertThrows(OrchestrationException.class, () -> {
      step.execute(context);
    });
    assertNotNull(exception.getCause());
  }

  // ========== Step 3.2: Getter method tests ==========

  @Test
  void testGetPreQueryStatement() {
    // Given: Step created with pre-query statement
    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When: Get pre-query statement
    QueryStatement result = step.getPreQueryStatement();

    // Then: Returns correct pre-query statement
    assertSame(mockPreQuery, result);
  }

  @Test
  void testGetRelatedModel() {
    // Given: Step created with related model
    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When: Get related model
    DataModel result = step.getRelatedModel();

    // Then: Returns correct related model
    assertSame(mockModel, result);
  }

  @Test
  void testGetLocalFieldPath() {
    // Given: Step created with local field path
    CrossSourceRewriteStep step = new CrossSourceRewriteStep(
            "preQuery",
            mockPreQuery,
            mockModel,
            "userId",
            "department",
            mockExecutor,
            1000,
            "id"
        );

    // When: Get local field path
    String result = step.getLocalFieldPath();

    // Then: Returns correct local field path
    assertEquals("userId", result);
  }
}
