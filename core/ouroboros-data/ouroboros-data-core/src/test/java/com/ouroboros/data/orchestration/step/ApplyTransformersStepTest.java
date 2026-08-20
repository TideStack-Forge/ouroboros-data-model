package com.ouroboros.data.orchestration.step;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.transformer.RecordListTransformer;
import com.ouroboros.data.record.RecordList;

/**
 * ApplyTransformersStep 单元测试
 *
 * @author Claude Code
 */
class ApplyTransformersStepTest {

  private OrchestrationContext context;
  private ApplyTransformersStep step;

  @BeforeEach
  void setUp() {
    context = mock(OrchestrationContext.class);
    step = new ApplyTransformersStep("apply", "main");
  }

  @Test
  void testApplySingleTransformer() {
    // Given: 一个转换器
    List<Map<String, Object>> records = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 1);
    records.add(record);

    RecordList sourceData = RecordList.of(records);

    // 使用一个标志来验证转换器被调用
    final boolean[] transformerCalled = {false};
    RecordListTransformer transformer = (recordList, ctx) -> {
      transformerCalled[0] = true;
      // 直接修改原始 Map（不通过 RecordList）
      records.get(0).put("transformed", true);
      return recordList;
    };

    Queue<RecordListTransformer> transformers = new LinkedList<>();
    transformers.add(transformer);

    when(context.getResult("main")).thenReturn(sourceData);
    when(context.getTransformers()).thenReturn(transformers);

    // When: 应用转换器
    step.execute(context);

    // Then: 转换器被调用
    assertTrue(transformerCalled[0]);
    verify(context).setResult(eq("apply"), any(RecordList.class));
  }

  @Test
  void testApplyMultipleTransformers() {
    // Given: 多个转换器（链式应用）
    List<Map<String, Object>> records = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 1);
    records.add(record);

    RecordList sourceData = RecordList.of(records);

    // Record 是不可变的，转换器需要创建新的 RecordList
    RecordListTransformer transformer1 = (recordList, ctx) -> {
      List<Map<String, Object>> modified = new ArrayList<>();
      for (Map<String, Object> r : recordList) {
        Map<String, Object> newR = new HashMap<>(r);
        newR.put("step1", true);
        modified.add(newR);
      }
      return RecordList.of(modified);
    };
    RecordListTransformer transformer2 = (recordList, ctx) -> {
      List<Map<String, Object>> modified = new ArrayList<>();
      for (Map<String, Object> r : recordList) {
        Map<String, Object> newR = new HashMap<>(r);
        newR.put("step2", true);
        modified.add(newR);
      }
      return RecordList.of(modified);
    };

    Queue<RecordListTransformer> transformers = new LinkedList<>();
    transformers.add(transformer1);
    transformers.add(transformer2);

    when(context.getResult("main")).thenReturn(sourceData);
    when(context.getTransformers()).thenReturn(transformers);

    // When: 应用转换器
    step.execute(context);

    // Then: 两个转换器都被应用
    verify(context).setResult(eq("apply"), argThat(result -> {
      Map<String, Object> r = result.get(0);
      return Boolean.TRUE.equals(r.get("step1")) && Boolean.TRUE.equals(r.get("step2"));
    }));
  }

  @Test
  void testEmptyTransformerQueue() {
    // Given: 空的转换器队列
    List<Map<String, Object>> records = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 1);
    records.add(record);

    RecordList sourceData = RecordList.of(records);

    Queue<RecordListTransformer> transformers = new LinkedList<>();

    when(context.getResult("main")).thenReturn(sourceData);
    when(context.getTransformers()).thenReturn(transformers);

    // When: 应用转换器
    step.execute(context);

    // Then: 直接返回源数据
    verify(context).setResult(eq("apply"), eq(sourceData));
  }

  @Test
  void testTransformerQueueCleared() {
    // Given: 转换器队列
    List<Map<String, Object>> records = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 1);
    records.add(record);

    RecordList sourceData = RecordList.of(records);

    RecordListTransformer transformer = (recordList, ctx) -> recordList;

    Queue<RecordListTransformer> transformers = new LinkedList<>();
    transformers.add(transformer);

    when(context.getResult("main")).thenReturn(sourceData);
    when(context.getTransformers()).thenReturn(transformers);

    // When: 应用转换器
    step.execute(context);

    // Then: 转换器队列被清空
    assertTrue(transformers.isEmpty());
  }
}
