package com.ouroboros.data.orchestration.step;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.OrchestrationException;

/**
 * AbstractQueryStep 单元测试
 *
 * @author Claude Code
 */
class AbstractQueryStepTest {

  @Test
  void testGetName() {
    // Given
    TestQueryStep step = new TestQueryStep("test-step");

    // Then
    assertEquals("test-step", step.getName());
  }

  @Test
  void testExecuteCallsDoExecute() {
    // Given
    TestQueryStep step = new TestQueryStep("test-step");
    OrchestrationContext context = new OrchestrationContext();

    // When
    step.execute(context);

    // Then
    assertTrue(step.wasExecuted);
  }

  @Test
  void testExecuteWrapsException() {
    // Given
    FailingQueryStep step = new FailingQueryStep("failing-step");
    OrchestrationContext context = new OrchestrationContext();

    // When & Then
    OrchestrationException exception = assertThrows(
        OrchestrationException.class,
        () -> step.execute(context)
    );

    assertTrue(exception.getMessage().contains("Step execution failed: failing-step"));
    assertNotNull(exception.getCause());
    assertEquals("Test failure", exception.getCause().getMessage());
  }

  // Test implementation
  private static class TestQueryStep extends AbstractQueryStep {
    boolean wasExecuted = false;

    TestQueryStep(String name) {
      super(name);
    }

    @Override
    protected void doExecute(OrchestrationContext context) {
      wasExecuted = true;
    }
  }

  // Failing implementation
  private static class FailingQueryStep extends AbstractQueryStep {
    FailingQueryStep(String name) {
      super(name);
    }

    @Override
    protected void doExecute(OrchestrationContext context) {
      throw new RuntimeException("Test failure");
    }
  }
}
