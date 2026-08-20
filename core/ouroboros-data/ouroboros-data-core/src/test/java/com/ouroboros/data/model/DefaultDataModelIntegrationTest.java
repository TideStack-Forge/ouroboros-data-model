package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.integration.MockTestUtils;
import com.ouroboros.data.model.valuetypes.RelatedValue;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

/**
 * DefaultDataModel 集成测试
 * <p>
 * 测试 QueryOrchestrator 与 DefaultDataModel 的集成。
 * </p>
 */
class DefaultDataModelIntegrationTest {

  private DefaultDataModel employeeModel;
  private DefaultDataModel departmentModel;
  private DefaultDataModel projectModel;
  private DefaultDataModel skillModel;
  private DefaultDataModel locationModel;
  private DataAdapter employeeAdapter;
  private Field dataModelCenterField;
  private Object originalDataModelMap;

  @BeforeEach
  void setUp() throws Exception {
    // 1. Create DataStation A (for Employee)
    DataStation<?> dataStationA = mock(DataStation.class);
    DataAdapter adapterA = mock(DataAdapter.class);
    employeeAdapter = adapterA;
    when(dataStationA.getDataAdapter()).thenReturn(adapterA);

    // Mock adapterA.query() to return empty RecordList
    when(adapterA.query(any(QueryStatement.class), any(TranspileContext.class)))
        .thenReturn(Try.success(RecordList.empty()));
    when(adapterA.query(any(QueryStatement.class)))
        .thenReturn(Try.success(RecordList.empty()));

    // 2. Create DataStation B (for Department)
    DataStation<?> dataStationB = mock(DataStation.class);
    DataAdapter adapterB = mock(DataAdapter.class);
    when(dataStationB.getDataAdapter()).thenReturn(adapterB);

    // Mock adapterB.query() to return empty RecordList
    when(adapterB.query(any(QueryStatement.class), any(TranspileContext.class)))
        .thenReturn(Try.success(RecordList.empty()));
    when(adapterB.query(any(QueryStatement.class)))
        .thenReturn(Try.success(RecordList.empty()));

    // 2.5. Create DataStation C (for Location)
    DataStation<?> dataStationC = mock(DataStation.class);
    DataAdapter adapterC = mock(DataAdapter.class);
    when(dataStationC.getDataAdapter()).thenReturn(adapterC);

    // Mock adapterC.query() to return empty RecordList
    when(adapterC.query(any(QueryStatement.class), any(TranspileContext.class)))
        .thenReturn(Try.success(RecordList.empty()));
    when(adapterC.query(any(QueryStatement.class)))
        .thenReturn(Try.success(RecordList.empty()));

    // 3. Create Department model meta
    DataModelMeta departmentMeta = mock(DataModelMeta.class);
    when(departmentMeta.getName()).thenReturn("Department");
    when(departmentMeta.getRawName()).thenReturn("department");
    when(departmentMeta.getFullName()).thenReturn("Department");

    DataModelFieldMeta deptIdField = mock(DataModelFieldMeta.class);
    when(deptIdField.getName()).thenReturn("id");

    DataModelFieldMeta deptNameField = mock(DataModelFieldMeta.class);
    when(deptNameField.getName()).thenReturn("name");

    DataModelFieldMeta deptManagerIdField = mock(DataModelFieldMeta.class);
    when(deptManagerIdField.getName()).thenReturn("managerId");

    // Create manager relation field (ToOne, cross-source to Employee)
    DataModelFieldMeta deptManagerField = mock(DataModelFieldMeta.class);
    when(deptManagerField.getName()).thenReturn("manager");
    when(deptManagerField.getType()).thenReturn("Model");
    Map<String, Object> deptManagerFieldExtraProps = new HashMap<>();
    deptManagerFieldExtraProps.put("model", "Employee");
    deptManagerFieldExtraProps.put("key", "managerId");
    deptManagerFieldExtraProps.put("referenceKey", "id");
    when(deptManagerField.getExtraProps()).thenReturn(deptManagerFieldExtraProps);
    when(deptManagerField.getExtraProp("model")).thenReturn(Optional.of("Employee"));
    when(deptManagerField.getExtraProp("key")).thenReturn(Optional.of("managerId"));
    when(deptManagerField.getExtraProp("referenceKey")).thenReturn(Optional.of("id"));
    when(deptManagerField.getExtraProp(CharSequence.class, "model")).thenReturn(Optional.of("Employee"));
    when(deptManagerField.getExtraProp(CharSequence.class, "key")).thenReturn(Optional.of("managerId"));
    when(deptManagerField.getExtraProp(CharSequence.class, "referenceKey")).thenReturn(Optional.of("id"));

    DataModelFieldMeta deptLocationIdField = mock(DataModelFieldMeta.class);
    when(deptLocationIdField.getName()).thenReturn("locationId");

    // Create location relation field (ToOne, cross-source to Location)
    DataModelFieldMeta deptLocationField = mock(DataModelFieldMeta.class);
    when(deptLocationField.getName()).thenReturn("location");
    when(deptLocationField.getType()).thenReturn("Model");
    Map<String, Object> deptLocationFieldExtraProps = new HashMap<>();
    deptLocationFieldExtraProps.put("model", "Location");
    deptLocationFieldExtraProps.put("key", "locationId");
    deptLocationFieldExtraProps.put("referenceKey", "id");
    when(deptLocationField.getExtraProps()).thenReturn(deptLocationFieldExtraProps);
    when(deptLocationField.getExtraProp("model")).thenReturn(Optional.of("Location"));
    when(deptLocationField.getExtraProp("key")).thenReturn(Optional.of("locationId"));
    when(deptLocationField.getExtraProp("referenceKey")).thenReturn(Optional.of("id"));
    when(deptLocationField.getExtraProp(CharSequence.class, "model")).thenReturn(Optional.of("Location"));
    when(deptLocationField.getExtraProp(CharSequence.class, "key")).thenReturn(Optional.of("locationId"));
    when(deptLocationField.getExtraProp(CharSequence.class, "referenceKey")).thenReturn(Optional.of("id"));

    when(departmentMeta.getFields()).thenReturn(Arrays.asList(
        deptIdField, deptNameField,
        deptManagerIdField, deptManagerField,
        deptLocationIdField, deptLocationField));

    // 4. Create Project model meta (same DataStation as Employee)
    DataModelMeta projectMeta = mock(DataModelMeta.class);
    when(projectMeta.getName()).thenReturn("Project");
    when(projectMeta.getRawName()).thenReturn("project");
    when(projectMeta.getFullName()).thenReturn("Project");

    DataModelFieldMeta projIdField = mock(DataModelFieldMeta.class);
    when(projIdField.getName()).thenReturn("id");

    DataModelFieldMeta projNameField = mock(DataModelFieldMeta.class);
    when(projNameField.getName()).thenReturn("name");

    DataModelFieldMeta projStatusField = mock(DataModelFieldMeta.class);
    when(projStatusField.getName()).thenReturn("status");

    when(projectMeta.getFields()).thenReturn(Arrays.asList(projIdField, projNameField, projStatusField));

    // 4.5. Create Skill model meta (same DataStation as Employee)
    DataModelMeta skillMeta = mock(DataModelMeta.class);
    when(skillMeta.getName()).thenReturn("Skill");
    when(skillMeta.getRawName()).thenReturn("skill");
    when(skillMeta.getFullName()).thenReturn("Skill");

    DataModelFieldMeta skillIdField = mock(DataModelFieldMeta.class);
    when(skillIdField.getName()).thenReturn("id");

    DataModelFieldMeta skillNameField = mock(DataModelFieldMeta.class);
    when(skillNameField.getName()).thenReturn("name");

    when(skillMeta.getFields()).thenReturn(Arrays.asList(skillIdField, skillNameField));

    // 4.6. Create Location model meta
    DataModelMeta locationMeta = mock(DataModelMeta.class);
    when(locationMeta.getName()).thenReturn("Location");
    when(locationMeta.getRawName()).thenReturn("location");
    when(locationMeta.getFullName()).thenReturn("Location");

    DataModelFieldMeta locIdField = mock(DataModelFieldMeta.class);
    when(locIdField.getName()).thenReturn("id");

    DataModelFieldMeta locCityField = mock(DataModelFieldMeta.class);
    when(locCityField.getName()).thenReturn("city");

    DataModelFieldMeta locCountryField = mock(DataModelFieldMeta.class);
    when(locCountryField.getName()).thenReturn("country");

    when(locationMeta.getFields()).thenReturn(Arrays.asList(locIdField, locCityField, locCountryField));

    // 5. Create Employee model meta
    DataModelMeta employeeMeta = mock(DataModelMeta.class);
    when(employeeMeta.getName()).thenReturn("Employee");
    when(employeeMeta.getRawName()).thenReturn("employee");
    when(employeeMeta.getFullName()).thenReturn("Employee");

    DataModelFieldMeta empIdField = mock(DataModelFieldMeta.class);
    when(empIdField.getName()).thenReturn("id");

    DataModelFieldMeta empNameField = mock(DataModelFieldMeta.class);
    when(empNameField.getName()).thenReturn("name");

    DataModelFieldMeta empDeptIdField = mock(DataModelFieldMeta.class);
    when(empDeptIdField.getName()).thenReturn("departmentId");

    // Create department relation field (ToOne)
    DataModelFieldMeta empDeptField = mock(DataModelFieldMeta.class);
    when(empDeptField.getName()).thenReturn("department");
    when(empDeptField.getType()).thenReturn("Model");
    Map<String, Object> deptFieldExtraProps = new HashMap<>();
    deptFieldExtraProps.put("model", "Department");
    deptFieldExtraProps.put("key", "departmentId");
    deptFieldExtraProps.put("referenceKey", "id");
    when(empDeptField.getExtraProps()).thenReturn(deptFieldExtraProps);
    when(empDeptField.getExtraProp("model")).thenReturn(Optional.of("Department"));
    when(empDeptField.getExtraProp("key")).thenReturn(Optional.of("departmentId"));
    when(empDeptField.getExtraProp("referenceKey")).thenReturn(Optional.of("id"));
    when(empDeptField.getExtraProp(CharSequence.class, "model")).thenReturn(Optional.of("Department"));
    when(empDeptField.getExtraProp(CharSequence.class, "key")).thenReturn(Optional.of("departmentId"));
    when(empDeptField.getExtraProp(CharSequence.class, "referenceKey")).thenReturn(Optional.of("id"));

    DataModelFieldMeta empProjectIdField = mock(DataModelFieldMeta.class);
    when(empProjectIdField.getName()).thenReturn("projectId");

    // Create project relation field (ToOne, same source)
    DataModelFieldMeta empProjectField = mock(DataModelFieldMeta.class);
    when(empProjectField.getName()).thenReturn("project");
    when(empProjectField.getType()).thenReturn("Model");
    Map<String, Object> projectFieldExtraProps = new HashMap<>();
    projectFieldExtraProps.put("model", "Project");
    projectFieldExtraProps.put("key", "projectId");
    projectFieldExtraProps.put("referenceKey", "id");
    when(empProjectField.getExtraProps()).thenReturn(projectFieldExtraProps);
    when(empProjectField.getExtraProp("model")).thenReturn(Optional.of("Project"));
    when(empProjectField.getExtraProp("key")).thenReturn(Optional.of("projectId"));
    when(empProjectField.getExtraProp("referenceKey")).thenReturn(Optional.of("id"));
    when(empProjectField.getExtraProp(CharSequence.class, "model")).thenReturn(Optional.of("Project"));
    when(empProjectField.getExtraProp(CharSequence.class, "key")).thenReturn(Optional.of("projectId"));
    when(empProjectField.getExtraProp(CharSequence.class, "referenceKey")).thenReturn(Optional.of("id"));

    DataModelFieldMeta empManagerIdField = mock(DataModelFieldMeta.class);
    when(empManagerIdField.getName()).thenReturn("managerId");

    DataModelFieldMeta empSettingsField = mock(DataModelFieldMeta.class);
    when(empSettingsField.getName()).thenReturn("settings");
    when(empSettingsField.getType()).thenReturn("Map");

    // Create manager relation field (ToOne, same source, self-reference)
    DataModelFieldMeta empManagerField = mock(DataModelFieldMeta.class);
    when(empManagerField.getName()).thenReturn("manager");
    when(empManagerField.getType()).thenReturn("Model");
    Map<String, Object> managerFieldExtraProps = new HashMap<>();
    managerFieldExtraProps.put("model", "Employee");
    managerFieldExtraProps.put("key", "managerId");
    managerFieldExtraProps.put("referenceKey", "id");
    when(empManagerField.getExtraProps()).thenReturn(managerFieldExtraProps);
    when(empManagerField.getExtraProp("model")).thenReturn(Optional.of("Employee"));
    when(empManagerField.getExtraProp("key")).thenReturn(Optional.of("managerId"));
    when(empManagerField.getExtraProp("referenceKey")).thenReturn(Optional.of("id"));
    when(empManagerField.getExtraProp(CharSequence.class, "model")).thenReturn(Optional.of("Employee"));
    when(empManagerField.getExtraProp(CharSequence.class, "key")).thenReturn(Optional.of("managerId"));
    when(empManagerField.getExtraProp(CharSequence.class, "referenceKey")).thenReturn(Optional.of("id"));

    when(employeeMeta.getFields()).thenReturn(Arrays.asList(
        empIdField, empNameField, empDeptIdField, empDeptField,
        empProjectIdField, empProjectField,
        empManagerIdField, empManagerField,
        empSettingsField));

    // 6. Create DataModel instances
    departmentModel = new DefaultDataModel(departmentMeta, dataStationB);
    employeeModel = new DefaultDataModel(employeeMeta, dataStationA);
    projectModel = new DefaultDataModel(projectMeta, dataStationA);
    skillModel = new DefaultDataModel(skillMeta, dataStationA);
    locationModel = new DefaultDataModel(locationMeta, dataStationC);

    // 7. Register models in DataModelCenter using reflection
    Map<String, DataModel> modelMap = new HashMap<>();
    modelMap.put("Department", departmentModel);
    modelMap.put("Employee", employeeModel);
    modelMap.put("Project", projectModel);
    modelMap.put("Skill", skillModel);
    modelMap.put("Location", locationModel);

    dataModelCenterField = DataModelCenter.class.getDeclaredField("dataModelMap");
    dataModelCenterField.setAccessible(true);
    originalDataModelMap = dataModelCenterField.get(null);
    dataModelCenterField.set(null, modelMap);
  }

  @AfterEach
  void tearDown() throws IllegalAccessException {
    if (dataModelCenterField != null) {
      dataModelCenterField.set(null, originalDataModelMap);
    }
  }

  /**
   * 测试场景 1：简单查询（无 JOIN，无 POPULATE）
   */
  @Test
  void testSimpleQuery() {
    // Given: 简单查询语句
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .build();

    // When: 执行查询
    Try<RecordList> result = employeeModel.query(statement);

    // Then: 验证结果
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  @Test
  void queryConvertsMapFieldValuesFromPersistentJson() {
    Map<String, Object> row = new HashMap<String, Object>();
    row.put("id", "1");
    row.put("settings", "{\"schema\":{\"type\":\"page\"}}");
    when(employeeAdapter.query(any(QueryStatement.class), any(TranspileContext.class)))
        .thenReturn(Try.success(RecordList.of(Collections.singletonList(row))));

    Try<RecordList> result = employeeModel.query(Collections.emptyMap());

    assertTrue(result.isSuccess(), "Query should succeed");
    Object settings = result.get().get(0).get("settings");
    assertTrue(settings instanceof Map, "Map field should be converted from persistent JSON");
    assertEquals("page", ((Map<?, ?>) ((Map<?, ?>) settings).get("schema")).get("type"));
  }

  /**
   * 测试场景 2：带 WHERE 条件的查询
   */
  @Test
  void testQueryWithWhere() {
    // Given: 带 WHERE 条件的查询
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("Alice")
        ))
        .build();

    // When: 执行查询
    Try<RecordList> result = employeeModel.query(statement);

    // Then: 验证结果
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  /**
   * 测试场景 3：跨源 JOIN 查询
   */
  @Test
  void testCrossSourceJoin() {
    // Given: 跨源 JOIN 查询
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.create(ExtOps.FIELD, "department", "name"),
            SExpression.constant("Engineering")
        ))
        .build();

    // When: 执行查询
    Try<RecordList> result = employeeModel.query(statement);

    // Then: 验证结果
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  /**
   * 测试场景 4：同源 POPULATE（JOIN 策略）
   */
  @Test
  void testPopulateWithJoin() {
    // Given: Query with POPULATE on same-source relation
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .populate("department")
        .build();

    // When: Execute query
    Try<RecordList> result = employeeModel.query(statement);

    // Then: Verify result
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");

    // Note: With mock data, we can't verify actual populate behavior
    // This test verifies the orchestration path executes without errors
  }

  /**
   * 测试场景 C1：跨源关联条件查询（EXISTS 策略）
   */
  @Test
  void testCrossSourceCondition() {
    // Given: Query with WHERE on cross-source relation
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.create(ExtOps.FIELD, "department", "name"),
            SExpression.constant("Engineering")
        ))
        .build();

    // When: Execute query
    Try<RecordList> result = employeeModel.query(statement);

    // Then: Verify result
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  /**
   * 测试场景 C2：跨源 POPULATE（Separate 策略）
   */
  @Test
  void testCrossSourcePopulate() {
    // Given: Query with POPULATE on cross-source relation
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .populate("department")
        .build();

    // When: Execute query
    Try<RecordList> result = employeeModel.query(statement);

    // Then: Verify result
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  /**
   * 测试场景 C3：混合同源和跨源条件
   */
  @Test
  void testMixedSameAndCrossSource() {
    // Given: Query with WHERE on both same-source and cross-source relations
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .where(SExpression.create(
            Operators.AND,
            SExpression.create(
                Operators.EQ,
                SExpression.create(ExtOps.FIELD, "department", "name"),
                SExpression.constant("Engineering")
            ),
            SExpression.create(
                Operators.EQ,
                SExpression.create(ExtOps.FIELD, "project", "status"),
                SExpression.constant("Active")
            )
        ))
        .build();

    // When: Execute query
    Try<RecordList> result = employeeModel.query(statement);

    // Then: Verify result
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  /**
   * 测试场景 C4：多个 POPULATE（同源 + 跨源）
   */
  @Test
  void testMultiplePopulate() {
    // Given: Query with POPULATE on both same-source and cross-source relations
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .populate("department")  // Cross-source
        .populate("project")     // Same-source
        .build();

    // When: Execute query
    Try<RecordList> result = employeeModel.query(statement);

    // Then: Verify result
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  /**
   * 测试场景：嵌套 POPULATE（两级关联填充）
   */
  @Test
  void testNestedPopulate() {
    // Given: Query with nested POPULATE
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .populate("department")
        .populate("department.manager")
        .build();

    // When: Execute query
    Try<RecordList> result = employeeModel.query(statement);

    // Then: Verify result
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  /**
   * 测试场景：多个 WHERE 条件 + 多个 POPULATE
   */
  @Test
  void testMultipleWhereAndPopulate() {
    // Given: Query with multiple WHERE and multiple POPULATE
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .where(SExpression.create(
            Operators.AND,
            SExpression.create(
                Operators.EQ,
                SExpression.create(ExtOps.FIELD, "department", "name"),
                SExpression.constant("Engineering")
            ),
            SExpression.create(
                Operators.EQ,
                SExpression.create(ExtOps.FIELD, "project", "status"),
                SExpression.constant("Active")
            )
        ))
        .populate("manager")
        .populate("project")
        .build();

    // When: Execute query
    Try<RecordList> result = employeeModel.query(statement);

    // Then: Verify result
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  /**
   * 测试场景：跨源链式查询（三级跨源）
   */
  @Test
  void testCrossSourceChain() {
    // Given: Query with 3-level cross-source condition
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.create(ExtOps.FIELD, "department", "location", "city"),
            SExpression.constant("Beijing")
        ))
        .build();

    // When: Execute query
    Try<RecordList> result = employeeModel.query(statement);

    // Then: Verify result
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
  }

  /**
   * 测试场景：大结果集性能验证
   */
  @Test
  void testLargeResultSet() {
    // Given: Mock adapter to return large result set
    RecordList largeResult = createLargeRecordList(1500);
    DataAdapter adapterA = employeeModel.getDataStation().getDataAdapter();
    when(adapterA.query(any(QueryStatement.class)))
        .thenReturn(Try.success(largeResult));
    when(adapterA.query(any(QueryStatement.class), any(TranspileContext.class)))
        .thenReturn(Try.success(largeResult));

    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .build();

    // When: Execute query
    Try<RecordList> result = employeeModel.query(statement);

    // Then: Verify result
    if (result.isFailure()) {
      result.getCause().printStackTrace();
    }
    assertTrue(result.isSuccess(), "Query should succeed");
    assertNotNull(result.get(), "Result should not be null");
    assertEquals(1500, result.get().size(), "Result should contain 1500 records");
  }

  /**
   * Helper method to create large RecordList
   */
  private RecordList createLargeRecordList(int size) {
    // Create simple RecordList with repeated data
    List<Map<String, Object>> records = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      record.put("name", "Employee" + i);
      records.add(record);
    }
    return RecordList.of(records);
  }
}
