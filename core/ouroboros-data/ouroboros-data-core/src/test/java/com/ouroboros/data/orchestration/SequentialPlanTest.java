package com.ouroboros.data.orchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.orchestration.step.QueryStep;
import com.ouroboros.data.record.RecordList;

/**
 * SequentialPlan 测试
 */
class SequentialPlanTest {

  private OrchestrationContext context;
  private RecordList mockResult;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
    mockResult = RecordList.empty();
  }

  @Test
  void testSequentialExecution() {
    // Given: 3 个步骤
    List<QueryStep> steps = new ArrayList<>();

    QueryStep step1 = mock(QueryStep.class);
    when(step1.getName()).thenReturn("step1");
    doAnswer(invocation -> {
      OrchestrationContext ctx = invocation.getArgument(0);
      ctx.setResult("step1", mockResult);
      return null;
    }).when(step1).execute(any());

    QueryStep step2 = mock(QueryStep.class);
    when(step2.getName()).thenReturn("step2");
    doAnswer(invocation -> {
      OrchestrationContext ctx = invocation.getArgument(0);
      ctx.setResult("step2", mockResult);
      return null;
    }).when(step2).execute(any());

    QueryStep step3 = mock(QueryStep.class);
    when(step3.getName()).thenReturn("step3");
    doAnswer(invocation -> {
      OrchestrationContext ctx = invocation.getArgument(0);
      ctx.setResult("step3", mockResult);
      return null;
    }).when(step3).execute(any());

    steps.add(step1);
    steps.add(step2);
    steps.add(step3);

    SequentialPlan plan = new SequentialPlan(steps, "step3");

    // When: 执行计划
    Try<RecordList> result = plan.execute(context);

    // Then: 所有步骤按顺序执行
    assertTrue(result.isSuccess());
    verify(step1).execute(context);
    verify(step2).execute(context);
    verify(step3).execute(context);
  }

  @Test
  void testFastFail() {
    // Given: 第二个步骤会失败
    List<QueryStep> steps = new ArrayList<>();

    QueryStep step1 = mock(QueryStep.class);
    when(step1.getName()).thenReturn("step1");
    doNothing().when(step1).execute(any());

    QueryStep step2 = mock(QueryStep.class);
    when(step2.getName()).thenReturn("step2");
    doThrow(new RuntimeException("Step 2 failed")).when(step2).execute(any());

    QueryStep step3 = mock(QueryStep.class);
    when(step3.getName()).thenReturn("step3");

    steps.add(step1);
    steps.add(step2);
    steps.add(step3);

    SequentialPlan plan = new SequentialPlan(steps, "step3");

    // When: 执行计划
    Try<RecordList> result = plan.execute(context);

    // Then: 执行失败，第三个步骤不会执行
    assertTrue(result.isFailure());
    verify(step1).execute(context);
    verify(step2).execute(context);
    verify(step3, never()).execute(any());
  }

  @Test
  void testFinalResultRetrieval() {
    // Given: 最终步骤有结果
    List<QueryStep> steps = new ArrayList<>();

    QueryStep finalStep = mock(QueryStep.class);
    when(finalStep.getName()).thenReturn("final");
    doAnswer(invocation -> {
      OrchestrationContext ctx = invocation.getArgument(0);
      ctx.setResult("final", mockResult);
      return null;
    }).when(finalStep).execute(any());

    steps.add(finalStep);

    SequentialPlan plan = new SequentialPlan(steps, "final");

    // When: 执行计划
    Try<RecordList> result = plan.execute(context);

    // Then: 返回最终结果
    assertTrue(result.isSuccess());
    assertEquals(mockResult, result.get());
  }

  @Test
  void testEmptyStepList() {
    // Given: 空步骤列表
    List<QueryStep> steps = new ArrayList<>();
    SequentialPlan plan = new SequentialPlan(steps, "final");

    // When: 执行计划
    Try<RecordList> result = plan.execute(context);

    // Then: 执行失败（找不到最终结果）
    assertTrue(result.isFailure());
    assertInstanceOf(OrchestrationException.class, result.getCause());
  }
}
