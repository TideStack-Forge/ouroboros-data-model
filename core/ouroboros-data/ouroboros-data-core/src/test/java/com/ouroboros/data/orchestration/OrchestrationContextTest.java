package com.ouroboros.data.orchestration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.RecordList;

/**
 * OrchestrationContext 单元测试
 *
 * @author Claude Code
 */
class OrchestrationContextTest {

  private OrchestrationContext context;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
  }

  @Test
  void testSetAndGetMainStatement() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());

    // When
    context.setMainStatement(statement);

    // Then
    assertSame(statement, context.getMainStatement());
  }

  @Test
  void testSetAndGetResult() {
    // Given
    RecordList result = RecordList.empty();

    // When
    context.setResult("test", result);

    // Then
    assertSame(result, context.getResult("test"));
  }

  @Test
  void testHasResult() {
    // Given
    RecordList result = RecordList.empty();
    context.setResult("test", result);

    // Then
    assertTrue(context.hasResult("test"));
    assertFalse(context.hasResult("nonexistent"));
  }

  @Test
  void testGetNonExistentResult() {
    // When
    RecordList result = context.getResult("nonexistent");

    // Then
    assertNull(result);
  }

  @Test
  void testMultipleResults() {
    // Given
    RecordList result1 = RecordList.empty();
    RecordList result2 = RecordList.empty();

    // When
    context.setResult("step1", result1);
    context.setResult("step2", result2);

    // Then
    assertSame(result1, context.getResult("step1"));
    assertSame(result2, context.getResult("step2"));
    assertTrue(context.hasResult("step1"));
    assertTrue(context.hasResult("step2"));
  }
}
