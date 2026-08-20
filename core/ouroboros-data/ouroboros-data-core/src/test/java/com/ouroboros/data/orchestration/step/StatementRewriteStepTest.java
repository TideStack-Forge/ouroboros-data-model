package com.ouroboros.data.orchestration.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.rewriter.StatementRewriter;

/**
 * StatementRewriteStep 测试
 */
class StatementRewriteStepTest {

  private OrchestrationContext context;
  private StatementRewriter mockRewriter;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
    mockRewriter = mock(StatementRewriter.class);
  }

  @Test
  void testAddRewriterToQueue() {
    // Given: 改写步骤
    StatementRewriteStep step = new StatementRewriteStep("rewrite", mockRewriter);

    // When: 执行步骤
    step.execute(context);

    // Then: 改写器被添加到队列
    assertFalse(context.getStatementRewriters().isEmpty());
    assertEquals(mockRewriter, context.getStatementRewriters().peek());
  }
}
