package com.ouroboros.data.orchestration.step;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.RelationType;
import com.ouroboros.data.orchestration.strategy.PopulateField;
import com.ouroboros.data.orchestration.transformer.RecordListTransformer;
import com.ouroboros.data.record.RecordList;

/**
 * PopulateStep 单元测试
 *
 * @author Claude Code
 */
class PopulateStepTest {

  private DataModel mockDataModel;
  private OrchestrationContext context;
  private PopulateField populateField;
  private PopulateStep populateStep;

  @BeforeEach
  void setUp() {
    mockDataModel = mock(DataModel.class);
    context = mock(OrchestrationContext.class);

    populateField = new PopulateField(
        "department",
        mockDataModel,
        "departmentId",
        "id",
        null,
        null
    );

    populateStep = new PopulateStep("populate_department", populateField, "main");
  }

  @Test
  void testExtractForeignKeys() {
    // Given: 主查询结果包含外键
    Map<String, Object> record1 = new HashMap<>();
    record1.put("id", 1);
    record1.put("departmentId", 101);

    Map<String, Object> record2 = new HashMap<>();
    record2.put("id", 2);
    record2.put("departmentId", 102);

    Map<String, Object> record3 = new HashMap<>();
    record3.put("id", 3);
    record3.put("departmentId", 101);  // 重复

    RecordList sourceData = RecordList.of(Arrays.asList(record1, record2, record3));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    // When: 执行 PopulateStep
    populateStep.execute(context);

    // Then: 验证查询被调用（说明外键被提取）
    verify(mockDataModel, times(1)).query(anyMap());
  }

  @Test
  void testBuildBatchQueryMap() {
    // Given: 源数据包含外键
    Map<String, Object> record1 = new HashMap<>();
    record1.put("id", 1);
    record1.put("departmentId", 101);

    RecordList sourceData = RecordList.of(Collections.singletonList(record1));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    // When: 执行 PopulateStep
    populateStep.execute(context);

    // Then: 验证查询参数结构
    verify(mockDataModel).query((Map<String, Object>) argThat(query -> {
      Map<String, Object> queryMap = (Map<String, Object>) query;
      return queryMap.containsKey("WHERE");
    }));
  }

  @Test
  void testBatchProcessing() {
    // Given: 超过 1000 个外键
    List<Map<String, Object>> records = new ArrayList<>();
    for (int i = 1; i <= 1500; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      record.put("departmentId", i);
      records.add(record);
    }

    RecordList sourceData = RecordList.of(records);

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    // When: 执行批量查询
    populateStep.execute(context);

    // Then: 验证分批执行（1000 + 500）
    verify(mockDataModel, times(2)).query(anyMap());
  }

  @Test
  void testCreateTransformer() {
    // Given: 源数据和关联数据
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 101);

    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    Map<String, Object> relatedRecord = new HashMap<>();
    relatedRecord.put("id", 101);
    relatedRecord.put("name", "开发部");

    RecordList relatedData = RecordList.of(Collections.singletonList(relatedRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    // When: 执行 PopulateStep
    populateStep.execute(context);

    // Then: 验证转换器被添加
    verify(context, times(1)).addTransformer(any(RecordListTransformer.class));
  }

  @Test
  void testEmptySourceData() {
    // Given: 空的源数据
    RecordList sourceData = RecordList.empty();

    when(context.getResult("main")).thenReturn(sourceData);

    // When: 执行 PopulateStep
    populateStep.execute(context);

    // Then: 不执行查询，不创建转换器
    verify(mockDataModel, never()).query(anyMap());
    verify(context, never()).addTransformer(any(RecordListTransformer.class));
  }

  // ========== Step 4.1: Empty/null handling tests ==========

  @Test
  void testExecuteWithEmptySourceData() {
    // Given: Empty source RecordList
    when(context.getResult("main")).thenReturn(RecordList.empty());

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: No query executed, no transformer added
    verify(mockDataModel, never()).query(anyMap());
    verify(context, never()).addTransformer(any(RecordListTransformer.class));
  }

  @Test
  void testExecuteWithNullSourceData() {
    // Given: Null source data from context
    when(context.getResult("main")).thenReturn(null);

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: No query executed, no transformer added
    verify(mockDataModel, never()).query(anyMap());
    verify(context, never()).addTransformer(any(RecordListTransformer.class));
  }

  @Test
  void testExecuteWithNullForeignKeys() {
    // Given: Source data with all null foreign keys
    Map<String, Object> record1 = new HashMap<>();
    record1.put("id", 1);
    record1.put("departmentId", null);

    Map<String, Object> record2 = new HashMap<>();
    record2.put("id", 2);
    record2.put("departmentId", null);

    RecordList sourceData = RecordList.of(Arrays.asList(record1, record2));

    when(context.getResult("main")).thenReturn(sourceData);

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: No query executed (all foreign keys are null)
    verify(mockDataModel, never()).query(anyMap());
    verify(context, never()).addTransformer(any(RecordListTransformer.class));
  }

  @Test
  void testExtractForeignKeysShouldIgnoreCaseOfJdbcResultKeys() {
    Map<String, Object> record = new HashMap<>();
    record.put("ID", 1);
    record.put("DEPARTMENTID", 101);

    when(context.getResult("main")).thenReturn(RecordList.of(Collections.singletonList(record)));
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    populateStep.execute(context);

    verify(mockDataModel, times(1)).query(anyMap());
  }

  @Test
  void testTransformerShouldIgnoreCaseOfSourceAndRelatedKeys() {
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("ID", 1);
    sourceRecord.put("DEPARTMENTID", 101);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    Map<String, Object> relatedRecord = new HashMap<>();
    relatedRecord.put("ID", 101);
    relatedRecord.put("NAME", "开发部");
    RecordList relatedData = RecordList.of(Collections.singletonList(relatedRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    populateStep.execute(context);

    ArgumentCaptor<RecordListTransformer> captor = ArgumentCaptor.forClass(RecordListTransformer.class);
    verify(context).addTransformer(captor.capture());

    RecordList transformed = captor.getValue().transform(sourceData, context);
    assertTrue(transformed.get(0).get("department") instanceof Map);
  }

  // ========== Step 4.2: Batch processing tests ==========

  @Test
  void testExecuteWithExactly1000ForeignKeys() {
    // Given: Exactly 1000 unique foreign keys
    List<Map<String, Object>> records = new ArrayList<>();
    for (int i = 1; i <= 1000; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      record.put("departmentId", i);
      records.add(record);
    }
    RecordList sourceData = RecordList.of(records);

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: Single batch query (exactly 1000)
    verify(mockDataModel, times(1)).query(anyMap());
  }

  @Test
  void testExecuteWith1001ForeignKeys() {
    // Given: 1001 unique foreign keys
    List<Map<String, Object>> records = new ArrayList<>();
    for (int i = 1; i <= 1001; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      record.put("departmentId", i);
      records.add(record);
    }
    RecordList sourceData = RecordList.of(records);

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: Two batch queries (1000 + 1)
    verify(mockDataModel, times(2)).query(anyMap());
  }

  @Test
  void testExecuteWith2500ForeignKeys() {
    // Given: 2500 unique foreign keys
    List<Map<String, Object>> records = new ArrayList<>();
    for (int i = 1; i <= 2500; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      record.put("departmentId", i);
      records.add(record);
    }
    RecordList sourceData = RecordList.of(records);

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: Three batch queries (1000 + 1000 + 500)
    verify(mockDataModel, times(3)).query(anyMap());
  }

  // ========== Step 4.3: Transformer and error handling tests ==========

  @Test
  void testTransformerWithToOneRelation() {
    // Given: Source data with foreign key, single related record
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 101);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    Map<String, Object> relatedRecord = new HashMap<>();
    relatedRecord.put("id", 101);
    relatedRecord.put("name", "Engineering");
    RecordList relatedData = RecordList.of(Collections.singletonList(relatedRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: Transformer created (single object fill for ToOne)
    verify(context).addTransformer(any(RecordListTransformer.class));
  }

  @Test
  void testTransformerWithToManyRelation() {
    // Given: Source data with foreign key, multiple related records
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 101);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    Map<String, Object> relatedRecord1 = new HashMap<>();
    relatedRecord1.put("id", 101);
    relatedRecord1.put("name", "Engineering");
    Map<String, Object> relatedRecord2 = new HashMap<>();
    relatedRecord2.put("id", 101);
    relatedRecord2.put("name", "Engineering-2");
    RecordList relatedData = RecordList.of(Arrays.asList(relatedRecord1, relatedRecord2));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: Transformer created (list fill for ToMany)
    verify(context).addTransformer(any(RecordListTransformer.class));
  }

  @Test
  void testTransformerWithMissingRelatedData() {
    // Given: Source data with foreign key, no matching related records
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 999);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: Transformer still created (handles missing data gracefully)
    verify(context).addTransformer(any(RecordListTransformer.class));
  }

  @Test
  void testTransformerWithNullForeignKeyValue() {
    // Given: Source data with mix of null and non-null foreign keys
    Map<String, Object> record1 = new HashMap<>();
    record1.put("id", 1);
    record1.put("departmentId", null);

    Map<String, Object> record2 = new HashMap<>();
    record2.put("id", 2);
    record2.put("departmentId", 101);

    RecordList sourceData = RecordList.of(Arrays.asList(record1, record2));

    Map<String, Object> relatedRecord = new HashMap<>();
    relatedRecord.put("id", 101);
    relatedRecord.put("name", "Engineering");
    RecordList relatedData = RecordList.of(Collections.singletonList(relatedRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    // When: Execute PopulateStep
    populateStep.execute(context);

    // Then: Query executed for non-null keys, transformer created
    verify(mockDataModel, times(1)).query(anyMap());
    verify(context).addTransformer(any(RecordListTransformer.class));
  }

  @Test
  void testExecuteWithQueryFailure() {
    // Given: Source data with foreign key, query returns failure
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 101);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(
        Try.failure(new RuntimeException("Query failed")));

    // When & Then: Should throw RuntimeException
    assertThrows(RuntimeException.class, () -> {
      populateStep.execute(context);
    });
  }

  @Test
  void testExecuteWithQueryException() {
    // Given: Source data with foreign key, query throws exception
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 101);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(
        Try.failure(new RuntimeException("Database error")));

    // When & Then: Should throw RuntimeException with message
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      populateStep.execute(context);
    });
    assertNotNull(exception.getMessage());
  }

  // ========== R13: Transformer invocation tests (GAP-01 closure) ==========

  @Test
  void testTransformerInvocationToOneMatch() {
    // Given: Source record with FK=101, one related record with id=101
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 101);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    Map<String, Object> relatedRecord = new HashMap<>();
    relatedRecord.put("id", 101);
    relatedRecord.put("name", "Engineering");
    RecordList relatedData = RecordList.of(Collections.singletonList(relatedRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    // When: Execute to create transformer
    populateStep.execute(context);

    // Capture the transformer
    ArgumentCaptor<RecordListTransformer> captor = ArgumentCaptor.forClass(RecordListTransformer.class);
    verify(context).addTransformer(captor.capture());
    RecordListTransformer transformer = captor.getValue();

    // Invoke transformer with test data
    Map<String, Object> inputRecord = new HashMap<>();
    inputRecord.put("id", 1);
    inputRecord.put("departmentId", 101);
    RecordList input = RecordList.of(Collections.singletonList(inputRecord));
    RecordList result = transformer.transform(input, context);

    // Then: Record should have "department" field with single object (ToOne)
    Map<String, Object> resultRecord = result.iterator().next();
    assertNotNull(resultRecord.get("department"));
    assertTrue(resultRecord.get("department") instanceof Map);
    assertEquals("Engineering", ((Map<String, Object>) resultRecord.get("department")).get("name"));
  }

  @Test
  void testTransformerInvocationToManyMatch() {
    // Given: Source record with FK=101, two related records with id=101
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 101);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    Map<String, Object> related1 = new HashMap<>();
    related1.put("id", 101);
    related1.put("name", "Eng-A");
    Map<String, Object> related2 = new HashMap<>();
    related2.put("id", 101);
    related2.put("name", "Eng-B");
    RecordList relatedData = RecordList.of(Arrays.asList(related1, related2));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    // When: Execute to create transformer
    populateStep.execute(context);

    // Capture and invoke transformer
    ArgumentCaptor<RecordListTransformer> captor = ArgumentCaptor.forClass(RecordListTransformer.class);
    verify(context).addTransformer(captor.capture());
    RecordListTransformer transformer = captor.getValue();

    Map<String, Object> inputRecord = new HashMap<>();
    inputRecord.put("id", 1);
    inputRecord.put("departmentId", 101);
    RecordList input = RecordList.of(Collections.singletonList(inputRecord));
    RecordList result = transformer.transform(input, context);

    // Then: Record should have "department" field with list (ToMany)
    Map<String, Object> resultRecord = result.iterator().next();
    assertNotNull(resultRecord.get("department"));
    assertTrue(resultRecord.get("department") instanceof List);
    assertEquals(2, ((List<?>) resultRecord.get("department")).size());
  }

  @Test
  void testTransformerInvocationToManySingleMatchShouldStillReturnList() {
    PopulateField toManyField = new PopulateField(
        "orderItems",
        mockDataModel,
        "id",
        "orderId",
        null,
        null,
        mockDataModel,
        RelationType.TO_MANY
    );
    PopulateStep toManyStep = new PopulateStep("populate_orderItems", toManyField, "main");

    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    Map<String, Object> relatedRecord = new HashMap<>();
    relatedRecord.put("orderId", 1);
    relatedRecord.put("productName", "Cable");
    RecordList relatedData = RecordList.of(Collections.singletonList(relatedRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    toManyStep.execute(context);

    ArgumentCaptor<RecordListTransformer> captor = ArgumentCaptor.forClass(RecordListTransformer.class);
    verify(context).addTransformer(captor.capture());
    RecordListTransformer transformer = captor.getValue();

    RecordList result = transformer.transform(sourceData, context);
    Map<String, Object> resultRecord = result.iterator().next();

    assertTrue(resultRecord.get("orderItems") instanceof List);
    assertEquals(1, ((List<?>) resultRecord.get("orderItems")).size());
  }

  @Test
  void testTransformerInvocationNoMatch() {
    // Given: Source record with FK=101, related data queried
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 101);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    Map<String, Object> relatedRecord = new HashMap<>();
    relatedRecord.put("id", 101);
    relatedRecord.put("name", "Engineering");
    RecordList relatedData = RecordList.of(Collections.singletonList(relatedRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    populateStep.execute(context);

    ArgumentCaptor<RecordListTransformer> captor = ArgumentCaptor.forClass(RecordListTransformer.class);
    verify(context).addTransformer(captor.capture());
    RecordListTransformer transformer = captor.getValue();

    // When: Invoke transformer with record that has FK=999 (no match in grouped data)
    Map<String, Object> inputRecord = new HashMap<>();
    inputRecord.put("id", 2);
    inputRecord.put("departmentId", 999);
    RecordList input = RecordList.of(Collections.singletonList(inputRecord));
    RecordList result = transformer.transform(input, context);

    // Then: Record unchanged (no "department" field added)
    Map<String, Object> resultRecord = result.iterator().next();
    assertNull(resultRecord.get("department"));
    assertEquals(999, resultRecord.get("departmentId"));
  }

  @Test
  void testTransformerInvocationNullFK() {
    // Given: Source record with FK=101, related data queried
    Map<String, Object> sourceRecord = new HashMap<>();
    sourceRecord.put("id", 1);
    sourceRecord.put("departmentId", 101);
    RecordList sourceData = RecordList.of(Collections.singletonList(sourceRecord));

    Map<String, Object> relatedRecord = new HashMap<>();
    relatedRecord.put("id", 101);
    relatedRecord.put("name", "Engineering");
    RecordList relatedData = RecordList.of(Collections.singletonList(relatedRecord));

    when(context.getResult("main")).thenReturn(sourceData);
    when(mockDataModel.query(anyMap())).thenReturn(Try.success(relatedData));

    populateStep.execute(context);

    ArgumentCaptor<RecordListTransformer> captor = ArgumentCaptor.forClass(RecordListTransformer.class);
    verify(context).addTransformer(captor.capture());
    RecordListTransformer transformer = captor.getValue();

    // When: Invoke transformer with record that has null FK
    Map<String, Object> inputRecord = new HashMap<>();
    inputRecord.put("id", 3);
    inputRecord.put("departmentId", null);
    RecordList input = RecordList.of(Collections.singletonList(inputRecord));
    RecordList result = transformer.transform(input, context);

    // Then: Record unchanged (null FK skipped)
    Map<String, Object> resultRecord = result.iterator().next();
    assertNull(resultRecord.get("department"));
  }
}
