package com.ouroboros.data.orchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.IntegerValue;
import com.ouroboros.data.model.valuetypes.MapValue;
import com.ouroboros.data.model.valuetypes.ModelValue;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.dsl.JoinType;
import com.querydsl.core.types.Ops;

/**
 * JOIN Populate 集成测试
 *
 * <p>端到端验证完整链路：
 * DefaultQueryOrchestrator.orchestrate() → analyzeStatement → buildPlan
 * （含 PopulateStrategySelector → JoinPopulateStrategy → PopulateJoinRewriter）
 * → MainQueryStep → ResultTransformStep → ApplyTransformersStep
 *
 * @author Claude Code
 */
class JoinPopulateIntegrationTest {

  private DefaultQueryOrchestrator orchestrator;
  private DataModel rootModel;
  private DataModel departmentModel;
  private DataModelField departmentField;
  private DataStation<?> stationA;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    orchestrator = new DefaultQueryOrchestrator();

    // DataStation mock（同源）
    stationA = mock(DataStation.class);
    when(stationA.getName()).thenReturn("stationA");

    // departmentModel mock
    departmentModel = mock(DataModel.class);
    when(departmentModel.getName()).thenReturn("Department");
    doReturn(stationA).when(departmentModel).getDataStation();

    // departmentModel 字段列表
    DataModelField deptIdField = mockPhysicalField("id");
    doReturn(new IntegerValue()).when(deptIdField).getValueType();
    DataModelField deptNameField = mockPhysicalField("name");
    DataModelField deptSettingsField = mockPhysicalField("settings");
    doReturn(new MapValue()).when(deptSettingsField).getValueType();
    when(departmentModel.getFields()).thenReturn(Arrays.asList(deptIdField, deptNameField, deptSettingsField));

    // ModelValue mock
    ModelValue mockModelValue = mock(ModelValue.class);
    when(mockModelValue.getReferenceModel()).thenReturn(Optional.of(departmentModel));
    when(mockModelValue.isPhysical()).thenReturn(false);
    DataModelField localKeyField = mock(DataModelField.class);
    when(localKeyField.getName()).thenReturn("departmentId");
    DataModelField referenceKeyField = mock(DataModelField.class);
    when(referenceKeyField.getName()).thenReturn("id");
    when(mockModelValue.getKey()).thenReturn(Optional.of(localKeyField));
    when(mockModelValue.getReferenceKey()).thenReturn(Optional.of(referenceKeyField));

    // departmentField mock
    departmentField = mock(DataModelField.class);
    when(departmentField.getName()).thenReturn("department");
    doReturn(mockModelValue).when(departmentField).getValueType();

    // rootModel mock
    rootModel = mock(DataModel.class);
    when(rootModel.getName()).thenReturn("Employee");
    doReturn(stationA).when(rootModel).getDataStation();
    when(rootModel.getField("department")).thenReturn(Optional.of(departmentField));
    DataModelField rootIdField = mockPhysicalField("id");
    DataModelField rootNameField = mockPhysicalField("name");
    DataModelField rootDepartmentIdField = mockPhysicalField("departmentId");
    when(rootModel.getFields()).thenReturn(Arrays.asList(
        rootIdField,
        rootNameField,
        rootDepartmentIdField,
        departmentField));
    when(departmentField.getDataModel()).thenReturn(rootModel);
  }

  @Test
  void testJoinPopulateConvertsRelatedMapFieldValues() {
    // Given
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .populate("department")
        .build();

    MainQueryExecutor executor = stmt -> {
      Map<String, Object> record = new LinkedHashMap<>();
      record.put("id", 1);
      record.put("name", "张三");
      record.put("departmentId", 10);
      record.put("department__id", 10);
      record.put("department__name", "开发部");
      record.put("department__settings", "{\"schema\":{\"type\":\"page\"}}");
      return Try.success(RecordList.of(Collections.singletonList(record)));
    };

    OrchestrationContext context = new OrchestrationContext();

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, rootModel, executor, context);

    // Then
    assertTrue(result.isSuccess(), "orchestrate() should return Success");

    Map<String, Object> resultRecord = result.get().get(0);
    Object deptObj = resultRecord.get("department");
    assertNotNull(deptObj, "department should not be null");
    assertTrue(deptObj instanceof Map, "department should be a Map");

    @SuppressWarnings("unchecked")
    Map<String, Object> deptMap = (Map<String, Object>) deptObj;
    Object settings = deptMap.get("settings");
    assertTrue(settings instanceof Map, "JOIN populate should convert related Map fields");

    @SuppressWarnings("unchecked")
    Map<String, Object> settingsMap = (Map<String, Object>) settings;
    assertEquals("page", ((Map<?, ?>) settingsMap.get("schema")).get("type"));
    assertFalse(resultRecord.containsKey("department__settings"), "Flat field department__settings should be removed");
  }

  private DataModelField mockPhysicalField(String name) {
    DataModelField field = mock(DataModelField.class);
    when(field.getName()).thenReturn(name);
    doReturn(new StringValue()).when(field).getValueType();
    return field;
  }

  @Test
  void testBasicJoinPopulate() {
    // Given
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .populate("department")
        .build();

    AtomicReference<QueryStatement> capturedStatement = new AtomicReference<>();
    MainQueryExecutor executor = stmt -> {
      capturedStatement.set(stmt);
      // 返回扁平化结果
      Map<String, Object> record = new LinkedHashMap<>();
      record.put("id", 1);
      record.put("name", "张三");
      record.put("departmentId", 10);
      record.put("department__id", 10);
      record.put("department__name", "开发部");
      return Try.success(RecordList.of(Collections.singletonList(record)));
    };

    OrchestrationContext context = new OrchestrationContext();

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, rootModel, executor, context);

    // Then
    assertTrue(result.isSuccess(), "orchestrate() should return Success");

    // 验证捕获的 QueryStatement 包含 JOIN
    QueryStatement captured = capturedStatement.get();
    assertNotNull(captured, "Captured statement should not be null");
    assertFalse(captured.getJoins().isEmpty(), "Captured statement should have JOIN clauses");

    // 验证 JOIN 类型为 LEFT JOIN
    QueryStatement.JoinEntry joinEntry = captured.getJoins().get(0);
    assertEquals(JoinType.LEFTJOIN, joinEntry.getType(), "JOIN type should be LEFTJOIN");

    // 验证 SELECT 包含前缀字段
    assertFalse(captured.getSelect().isEmpty(), "Captured statement should have SELECT fields");

    // 验证最终结果包含嵌套 department 对象
    RecordList finalResult = result.get();
    assertEquals(1, finalResult.size(), "Result should have 1 record");
    Map<String, Object> resultRecord = finalResult.get(0);

    // 验证嵌套 department 对象
    Object deptObj = resultRecord.get("department");
    assertNotNull(deptObj, "department should not be null");
    assertTrue(deptObj instanceof Map, "department should be a Map");
    @SuppressWarnings("unchecked")
    Map<String, Object> deptMap = (Map<String, Object>) deptObj;
    assertEquals(10, deptMap.get("id"), "department.id should be 10");
    assertEquals("开发部", deptMap.get("name"), "department.name should be '开发部'");

    // 验证扁平字段已移除
    assertFalse(resultRecord.containsKey("department__id"), "Flat field department__id should be removed");
    assertFalse(resultRecord.containsKey("department__name"), "Flat field department__name should be removed");
  }

  @Test
  void testJoinPopulateNullSemantics() {
    // Given
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .populate("department")
        .build();

    MainQueryExecutor executor = stmt -> {
      // 返回扁平化结果，所有 department 字段为 null（LEFT JOIN 无匹配）
      Map<String, Object> record = new LinkedHashMap<>();
      record.put("id", 1);
      record.put("name", "张三");
      record.put("departmentId", null);
      record.put("department__id", null);
      record.put("department__name", null);
      return Try.success(RecordList.of(Collections.singletonList(record)));
    };

    OrchestrationContext context = new OrchestrationContext();

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, rootModel, executor, context);

    // Then
    assertTrue(result.isSuccess(), "orchestrate() should return Success");

    RecordList finalResult = result.get();
    assertEquals(1, finalResult.size(), "Result should have 1 record");
    Map<String, Object> resultRecord = finalResult.get(0);

    // 验证 department 字段为 null（非空 Map）
    assertNull(resultRecord.get("department"), "department should be null when all JOIN fields are null");

    // 验证扁平字段已移除
    assertFalse(resultRecord.containsKey("department__id"), "Flat field department__id should be removed");
    assertFalse(resultRecord.containsKey("department__name"), "Flat field department__name should be removed");
  }

  @Test
  void testWhereInnerJoinWithPopulate() {
    // Given: WHERE REL_ANY(department, EQ(FIELD(name), "开发部")) + POPULATE department
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        SExpression.create(Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("开发部"))
    );

    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .populate("department")
        .build();

    AtomicReference<QueryStatement> capturedStatement = new AtomicReference<>();
    MainQueryExecutor executor = stmt -> {
      capturedStatement.set(stmt);
      Map<String, Object> record = new LinkedHashMap<>();
      record.put("id", 1);
      record.put("name", "张三");
      record.put("departmentId", 10);
      record.put("department__id", 10);
      record.put("department__name", "开发部");
      return Try.success(RecordList.of(Collections.singletonList(record)));
    };

    OrchestrationContext context = new OrchestrationContext();

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, rootModel, executor, context);

    // Then
    assertTrue(result.isSuccess(), "orchestrate() should return Success");

    // 验证 JOIN 子句
    QueryStatement captured = capturedStatement.get();
    assertNotNull(captured);
    assertFalse(captured.getJoins().isEmpty(), "Should have JOIN clauses");

    // JoinDeduplicator 应合并 WHERE JOIN 和 Populate JOIN，只保留 1 个
    assertEquals(1, captured.getJoins().size(),
        "JoinDeduplicator should merge duplicate department JOINs into 1");

    // 验证结果包含嵌套 department 对象
    RecordList finalResult = result.get();
    assertEquals(1, finalResult.size());
    Map<String, Object> resultRecord = finalResult.get(0);
    Object deptObj = resultRecord.get("department");
    assertNotNull(deptObj, "department should not be null");
    assertTrue(deptObj instanceof Map, "department should be a Map");
  }

  @Test
  void testWhereLeftJoinWithPopulate() {
    // Given: WHERE OR(REL_ANY(department, EQ(FIELD(name), "开发部")), IS_NULL(FIELD(department)))
    //        + POPULATE department
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        SExpression.create(Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("开发部"))
    );
    SExpression<Boolean> nullCheck = SExpression.create(
        Ops.IS_NULL,
        SExpression.create(Operators.FIELD, "department")
    );
    SExpression<Boolean> orCondition = SExpression.create(
        Operators.OR, relCondition, nullCheck
    );

    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(orCondition)
        .populate("department")
        .build();

    AtomicReference<QueryStatement> capturedStatement = new AtomicReference<>();
    MainQueryExecutor executor = stmt -> {
      capturedStatement.set(stmt);
      Map<String, Object> record = new LinkedHashMap<>();
      record.put("id", 1);
      record.put("name", "张三");
      record.put("departmentId", 10);
      record.put("department__id", 10);
      record.put("department__name", "开发部");
      return Try.success(RecordList.of(Collections.singletonList(record)));
    };

    OrchestrationContext context = new OrchestrationContext();

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, rootModel, executor, context);

    // Then
    assertTrue(result.isSuccess(), "orchestrate() should return Success");

    // 验证 JOIN 子句使用 LEFT JOIN
    QueryStatement captured = capturedStatement.get();
    assertNotNull(captured);
    assertFalse(captured.getJoins().isEmpty(), "Should have JOIN clauses");

    // 验证 JOIN 类型为 LEFT JOIN（NULL 语义要求 LEFT JOIN）
    QueryStatement.JoinEntry joinEntry = captured.getJoins().get(0);
    assertEquals(JoinType.LEFTJOIN, joinEntry.getType(),
        "JOIN type should be LEFTJOIN due to NULL semantics");

    // 验证结果包含嵌套 department 对象
    RecordList finalResult = result.get();
    assertEquals(1, finalResult.size());
    Map<String, Object> resultRecord = finalResult.get(0);
    Object deptObj = resultRecord.get("department");
    assertNotNull(deptObj, "department should not be null");
    assertTrue(deptObj instanceof Map, "department should be a Map");
  }
}
