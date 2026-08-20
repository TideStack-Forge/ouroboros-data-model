package com.ouroboros.data.orchestration.strategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.orchestration.rewriter.PopulateJoinRewriter;
import com.ouroboros.data.orchestration.step.QueryStep;
import com.ouroboros.data.orchestration.step.ResultTransformStep;
import com.ouroboros.data.orchestration.step.StatementRewriteStep;

/**
 * JoinPopulateStrategy 单元测试
 */
class JoinPopulateStrategyTest {

  @Test
  void testCreateStepsReturnsTwoSteps() {
    // Given
    JoinPopulateStrategy strategy = new JoinPopulateStrategy();
    DataModel model = mock(DataModel.class);
    PopulateField field = new PopulateField(
        "department", model, "departmentId", "id",
        Arrays.asList("id", "name"), null);

    // When
    List<QueryStep> steps = strategy.createSteps(field, "main");

    // Then: Returns 2 steps
    assertEquals(2, steps.size());
  }

  @Test
  void testFirstStepIsStatementRewriteStep() {
    // Given
    JoinPopulateStrategy strategy = new JoinPopulateStrategy();
    DataModel model = mock(DataModel.class);
    PopulateField field = new PopulateField(
        "department", model, "departmentId", "id",
        Arrays.asList("id", "name"), null);

    // When
    List<QueryStep> steps = strategy.createSteps(field, "main");

    // Then: First step is StatementRewriteStep containing PopulateJoinRewriter
    assertInstanceOf(StatementRewriteStep.class, steps.get(0));
    StatementRewriteStep rewriteStep = (StatementRewriteStep) steps.get(0);
    assertEquals("populate_rewrite_department", rewriteStep.getName());
    assertInstanceOf(PopulateJoinRewriter.class, rewriteStep.getRewriter());
  }

  @Test
  void testSecondStepIsResultTransformStep() {
    // Given
    JoinPopulateStrategy strategy = new JoinPopulateStrategy();
    DataModel model = mock(DataModel.class);
    PopulateField field = new PopulateField(
        "department", model, "departmentId", "id",
        Arrays.asList("id", "name"), null);

    // When
    List<QueryStep> steps = strategy.createSteps(field, "main");

    // Then: Second step is ResultTransformStep
    assertInstanceOf(ResultTransformStep.class, steps.get(1));
    assertEquals("result_transform_department", steps.get(1).getName());
  }

  @Test
  void testUsesRelatedModelFullNameForSelfPopulateJoinTarget() {
    JoinPopulateStrategy strategy = new JoinPopulateStrategy();
    DataModel relatedModel = mock(DataModel.class);
    when(relatedModel.getName()).thenReturn("User");
    when(relatedModel.getFullName()).thenReturn("a.b.c.User");
    PopulateField field = new PopulateField(
        "parent", relatedModel, "parent", "id",
        Arrays.asList("id", "name"), null);

    List<QueryStep> steps = strategy.createSteps(field, "main");

    StatementRewriteStep rewriteStep = (StatementRewriteStep) steps.get(0);
    PopulateJoinRewriter rewriter = (PopulateJoinRewriter) rewriteStep.getRewriter();
    assertEquals("a.b.c.User", rewriter.relationTargetName(),
        "自关联 populate 应保留完整模型名，避免 JoinBuilder 无法解析简单名");
  }
}
