package com.ouroboros.data.orchestration.step;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.transformer.RecordListTransformer;
import com.ouroboros.data.record.RecordList;

/**
 * ResultTransformStep 测试
 */
class ResultTransformStepTest {

  private OrchestrationContext context;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
  }

  @Test
  void testTransformExtractsFlatFieldsToNestedMap() {
    // Given: 含扁平字段的记录
    ResultTransformStep step = new ResultTransformStep(
        "result_transform_department",
        "department",
        "department__",
        Arrays.asList("id", "name")
    );

    step.execute(context);

    // 验证转换器已添加到 context
    Queue<RecordListTransformer> transformers = context.getTransformers();
    assertEquals(1, transformers.size());

    // 应用转换器
    RecordListTransformer transformer = transformers.poll();
    Map<String, Object> record = new LinkedHashMap<>();
    record.put("id", 1);
    record.put("name", "Alice");
    record.put("department__id", 10);
    record.put("department__name", "IT");
    RecordList input = RecordList.of(Collections.singletonList(record));

    RecordList result = transformer.transform(input, context);

    // Then: 嵌套对象正确
    assertEquals(1, result.size());
    Map<String, Object> transformed = result.get(0);
    assertEquals(1, transformed.get("id"));
    assertEquals("Alice", transformed.get("name"));

    @SuppressWarnings("unchecked")
    Map<String, Object> nested = (Map<String, Object>) transformed.get("department");
    assertNotNull(nested);
    assertEquals(10, nested.get("id"));
    assertEquals("IT", nested.get("name"));
  }

  @Test
  void testTransformRemovesFlatFields() {
    // Given
    ResultTransformStep step = new ResultTransformStep(
        "result_transform_department",
        "department",
        "department__",
        Arrays.asList("id", "name")
    );

    step.execute(context);
    RecordListTransformer transformer = context.getTransformers().poll();

    Map<String, Object> record = new LinkedHashMap<>();
    record.put("id", 1);
    record.put("department__id", 10);
    record.put("department__name", "IT");
    RecordList input = RecordList.of(Collections.singletonList(record));

    // When
    RecordList result = transformer.transform(input, context);

    // Then: 扁平字段已移除
    Map<String, Object> transformed = result.get(0);
    assertFalse(transformed.containsKey("department__id"));
    assertFalse(transformed.containsKey("department__name"));
    assertTrue(transformed.containsKey("department"));
  }

  @Test
  void testTransformSetsNullWhenAllFieldsNull() {
    // Given: LEFT JOIN 无匹配，所有扁平字段为 null
    ResultTransformStep step = new ResultTransformStep(
        "result_transform_department",
        "department",
        "department__",
        Arrays.asList("id", "name")
    );

    step.execute(context);
    RecordListTransformer transformer = context.getTransformers().poll();

    Map<String, Object> record = new LinkedHashMap<>();
    record.put("id", 1);
    record.put("name", "Bob");
    record.put("department__id", null);
    record.put("department__name", null);
    RecordList input = RecordList.of(Collections.singletonList(record));

    // When
    RecordList result = transformer.transform(input, context);

    // Then: 嵌套对象为 null
    Map<String, Object> transformed = result.get(0);
    assertTrue(transformed.containsKey("department"));
    assertNull(transformed.get("department"));
  }

  @Test
  void testTransformShouldIgnoreCaseOfJdbcFlatKeys() {
    ResultTransformStep step = new ResultTransformStep(
        "result_transform_department",
        "department",
        "department__",
        Arrays.asList("id", "name")
    );

    step.execute(context);
    RecordListTransformer transformer = context.getTransformers().poll();

    Map<String, Object> record = new LinkedHashMap<>();
    record.put("ID", 1);
    record.put("DEPARTMENT__ID", 10);
    record.put("DEPARTMENT__NAME", "IT");
    RecordList input = RecordList.of(Collections.singletonList(record));

    RecordList result = transformer.transform(input, context);

    @SuppressWarnings("unchecked")
    Map<String, Object> nested = (Map<String, Object>) result.get(0).get("department");
    assertNotNull(nested);
    assertEquals(10, nested.get("id"));
    assertEquals("IT", nested.get("name"));
    assertFalse(result.get(0).containsKey("DEPARTMENT__ID"));
    assertFalse(result.get(0).containsKey("DEPARTMENT__NAME"));
  }

  @Test
  void testTransformWithEmptySelectFields() {
    // Given: selectFields 为空
    ResultTransformStep step = new ResultTransformStep(
        "result_transform_department",
        "department",
        "department__",
        Collections.emptyList()
    );

    step.execute(context);

    // Then: 不创建转换器
    assertTrue(context.getTransformers().isEmpty());
  }
}
