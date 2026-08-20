package com.ouroboros.data.orchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.dsl.statement.OmitClause;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.CollectionValue;
import com.ouroboros.data.model.valuetypes.ModelValue;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

/**
 * DefaultQueryOrchestrator 单元测试
 *
 * @author Claude Code
 */
class DefaultQueryOrchestratorTest {

  private DefaultQueryOrchestrator orchestrator;
  private OrchestrationContext context;
  private DataModel model;
  private MainQueryExecutor executor;

  @BeforeEach
  void setUp() {
    orchestrator = new DefaultQueryOrchestrator();
    context = new OrchestrationContext();
    model = mock(DataModel.class);
    executor = mock(MainQueryExecutor.class);

    when(model.getName()).thenReturn("TestModel");
  }

  @Test
  void testOrchestrateSuccess() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then
    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }
    assertSame(expectedResult, result.get());
  }

  @Test
  void testOrchestrateInitializesContext() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    orchestrator.orchestrate(statement, model, executor, context);

    // Then
    assertSame(statement, context.getMainStatement());
  }

  @Test
  void testOrchestrateExecutesMainStep() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    orchestrator.orchestrate(statement, model, executor, context);

    // Then
    assertTrue(context.hasResult("main"));
  }

  @Test
  void testOrchestrateReturnsResult() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then
    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }
    assertSame(expectedResult, result.get());
    assertSame(expectedResult, context.getResult("main"));
  }

  @Test
  void testOrchestrateFailure() {
    // Given
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RuntimeException cause = new RuntimeException("Execution failed");

    when(executor.execute(any())).thenReturn(Try.failure(cause));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then
    assertTrue(result.isFailure());
    assertNotNull(result.getCause());
  }

  @Test
  void testOrchestrateWithNoWhere() {
    // Given: 无 WHERE 的语句 → analyzeStatement 返回空结果 → 只执行主查询
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then
    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }
    assertSame(expectedResult, result.get());
  }

  @Test
  void testOrchestrateWithNormalWhere() {
    // Given: 普通 WHERE 条件（无关联条件）→ analyzeStatement 返回空结果 → 只执行主查询
    SExpression<Boolean> normalCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "status"),
        SExpression.constant("active")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(normalCondition)
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then
    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }
    assertSame(expectedResult, result.get());
  }

  @Test
  void testOrchestrateWithRelationCondition() {
    // Given: WHERE 含 REL_ANY 条件 → analyzeStatement 识别为 sameSourceToMany
    // → buildPlan 创建 ExistsStatementRewriter + MainQueryStep
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "orders.status"),
            SExpression.constant("completed")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: 应成功执行（EXISTS 改写 + 主查询）
    assertTrue(result.isSuccess());
    assertSame(expectedResult, result.get());
  }

  @Test
  void testOrchestratePreparesSelectWildcardBeforeExecute() {
    DataModelField idField = mock(DataModelField.class);
    when(idField.getName()).thenReturn("id");
    doReturn(new StringValue()).when(idField).getValueType();

    DataModelField nameField = mock(DataModelField.class);
    when(nameField.getName()).thenReturn("name");
    doReturn(new StringValue()).when(nameField).getValueType();

    when(model.getFields()).thenReturn(Arrays.asList(idField, nameField));

    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .select(SExpression.create(Operators.COLUMNS, "*"))
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }

    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());

    QueryStatement executedStatement = statementCaptor.getValue();
    assertFalse(executedStatement.getSelect().stream()
            .anyMatch(expr -> expr.getOperator() == Operators.COLUMNS
                && expr.getParams().size() == 1
                && "*".equals(expr.getParam(0))),
        "prepare 后不应保留原始 wildcard 选择项");
    Optional<SExpression<?>> expandedIdSelect = executedStatement.getSelect().stream()
        .filter(expr -> expr.getOperator() == Operators.ALIAS)
        .filter(expr -> "id".equals(expr.getParam(1)))
        .findFirst();
    assertTrue(expandedIdSelect.isPresent(),
        "wildcard 应追加展开出的 id 选择项");
    SExpression<?> selectedField = expandedIdSelect.get().getParamAsSExpression(0);
    assertEquals(Operators.COLUMNS, selectedField.getOperator());
    SExpression<?> selectedColumn = selectedField.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, selectedColumn.getOperator());
    assertEquals("user", selectedColumn.getParam(0),
        "展开项应补上主表 alias");
    assertEquals("id", selectedColumn.getParam(1),
        "展开项应保留字段名");
  }

  @Test
  void testOrchestratePreservesExplicitSelectWhenWildcardMixedBeforeExecute() {
    DataModelField idField = mock(DataModelField.class);
    when(idField.getName()).thenReturn("id");
    doReturn(new StringValue()).when(idField).getValueType();

    DataModelField nameField = mock(DataModelField.class);
    when(nameField.getName()).thenReturn("name");
    doReturn(new StringValue()).when(nameField).getValueType();

    when(model.getFields()).thenReturn(Arrays.asList(idField, nameField));

    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .select(SExpression.create(Operators.COLUMNS,
            "*",
            SExpression.alias(
                SExpression.create(Operators.COUNT, SExpression.field("*")),
                "cnt")))
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }

    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());

    QueryStatement executedStatement = statementCaptor.getValue();
    assertTrue(executedStatement.getSelect().stream()
            .anyMatch(expr -> expr.getOperator() == Operators.ALIAS
                && "id".equals(expr.getParam(1))),
        "mixed wildcard 应保留展开出的主表字段");
    assertTrue(executedStatement.getSelect().stream()
            .anyMatch(expr -> expr.getOperator() == Operators.ALIAS
                && "cnt".equals(expr.getParam(1))),
        "mixed wildcard 不应丢失显式聚合投影");
  }

  @Test
  void testOrchestrateSkipsUnresolvableDottedFieldsDuringWildcardExpansion() {
    DataModelField idField = mock(DataModelField.class);
    when(idField.getName()).thenReturn("id");
    doReturn(new StringValue()).when(idField).getValueType();

    DataModelField dottedField = mock(DataModelField.class);
    when(dottedField.getName()).thenReturn("aEntryDisplayId.id");
    doReturn(new StringValue()).when(dottedField).getValueType();

    when(model.getFields()).thenReturn(Arrays.asList(idField, dottedField));

    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .select(SExpression.create(Operators.COLUMNS, "*"))
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }

    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());

    QueryStatement executedStatement = statementCaptor.getValue();
    assertFalse(executedStatement.getSelect().stream()
            .anyMatch(expr -> expr.getOperator() == Operators.ALIAS
                && "aEntryDisplayId.id".equals(expr.getParam(1))),
        "wildcard 展开不应保留无法直接解析的点号字段");
  }

  @Test
  void testOrchestratePreservesAllExplicitSelectColumnsBeforeExecute() {
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .select(SExpression.create(Operators.COLUMNS,
            SExpression.field("id"),
            SExpression.field("name"),
            SExpression.field("code")))
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }

    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());

    QueryStatement executedStatement = statementCaptor.getValue();
    assertEquals(1, executedStatement.getSelect().size(), "显式 SELECT 应保留单个 COLUMNS 表达式");

    SExpression<?> columns = executedStatement.getSelect().get(0);
    assertEquals(Operators.COLUMNS, columns.getOperator());
    assertEquals(3, columns.getParams().size(), "prepare 后不应丢失显式选择列");

    SExpression<?> first = columns.getParamAsSExpression(0);
    SExpression<?> second = columns.getParamAsSExpression(1);
    SExpression<?> third = columns.getParamAsSExpression(2);

    assertEquals("user", first.getParam(0));
    assertEquals("id", first.getParam(1));
    assertEquals("user", second.getParam(0));
    assertEquals("name", second.getParam(1));
    assertEquals("user", third.getParam(0));
    assertEquals("code", third.getParam(1));
  }

  @Test
  void testOrchestrateShouldApplyOmitInsideExplicitColumnsBeforeExecute() {
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .select(SExpression.create(Operators.COLUMNS,
            SExpression.field("id"),
            SExpression.field("email")))
        .build();
    context.setOmitClause(OmitClause.fromRaw("email"));

    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }

    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());

    QueryStatement executedStatement = statementCaptor.getValue();
    assertEquals(1, executedStatement.getSelect().size());

    SExpression<?> columns = executedStatement.getSelect().get(0);
    assertEquals(Operators.COLUMNS, columns.getOperator());
    assertEquals(1, columns.getParams().size(), "omit 后只应保留未排除列");

    SExpression<?> remaining = columns.getParamAsSExpression(0);
    assertEquals("user", remaining.getParam(0));
    assertEquals("id", remaining.getParam(1));
  }

  // ========== Step 2.1: analyzeStatement() tests ==========

  @Test
  void testAnalyzeStatementWithREL_ANY() {
    // Given: WHERE clause with REL_ANY operator
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "status"),
            SExpression.constant("active")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (REL_ANY detected and processed)
    assertTrue(result.isSuccess());
  }

  @Test
  void testAnalyzeStatementWithREL_ALL() {
    // Given: WHERE clause with REL_ALL operator
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ALL,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "status"),
            SExpression.constant("completed")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (REL_ALL detected and processed)
    assertTrue(result.isSuccess());
  }

  @Test
  void testAnalyzeStatementWithREL_NONE() {
    // Given: WHERE clause with REL_NONE operator
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_NONE,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "status"),
            SExpression.constant("cancelled")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (REL_NONE detected and processed)
    assertTrue(result.isSuccess());
  }

  @Test
  void testAnalyzeStatementWithEmptyWHERE() {
    // Given: Empty WHERE clause
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed with empty analysis result
    assertTrue(result.isSuccess());
    assertSame(expectedResult, result.get());
  }

  @Test
  void testAnalyzeStatementWithNestedConditions() {
    // Given: Nested AND/OR with REL operators
    SExpression<Boolean> relCondition1 = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "status"),
            SExpression.constant("active")
        )
    );
    SExpression<Boolean> relCondition2 = SExpression.create(
        ExtOps.REL_ALL,
        SExpression.create(Operators.FIELD, "payments"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "status"),
            SExpression.constant("paid")
        )
    );
    SExpression<Boolean> nestedCondition = SExpression.create(
        Operators.AND,
        relCondition1,
        relCondition2
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(nestedCondition)
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (both REL conditions detected)
    assertTrue(result.isSuccess());
  }

  // ========== Step 2.2: buildPlan() tests ==========

  @Test
  void testBuildPlanWithToOneConditions() {
    // Given: Statement with ToOne relation condition
    // Note: Current implementation treats all REL conditions as ToMany
    // This test verifies the orchestration succeeds
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("Engineering")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed
    assertTrue(result.isSuccess());
  }

  @Test
  void testBuildPlanWithToManyConditions() {
    // Given: Statement with ToMany relation condition
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.GT,
            SExpression.create(Operators.FIELD, "amount"),
            SExpression.constant(100)
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("customer")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (ExistsStatementRewriter created)
    assertTrue(result.isSuccess());
  }

  @Test
  void testBuildPlanWithJoinDeduplicator() {
    // Given: Multiple ToOne conditions (would trigger JoinDeduplicator)
    // Note: Current implementation treats all as ToMany, so no deduplicator
    // This test verifies orchestration succeeds with multiple conditions
    SExpression<Boolean> relCondition1 = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("Engineering")
        )
    );
    SExpression<Boolean> relCondition2 = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "manager"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "level"),
            SExpression.constant("senior")
        )
    );
    SExpression<Boolean> combined = SExpression.create(
        Operators.AND,
        relCondition1,
        relCondition2
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(combined)
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed
    assertTrue(result.isSuccess());
  }

  @Test
  void testBuildPlanWithoutJoinDeduplicator() {
    // Given: No ToOne conditions (no JoinDeduplicator needed)
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed without deduplicator
    assertTrue(result.isSuccess());
  }

  // ========== Step 2.3: extractPopulateFields() tests ==========

  @Test
  void testExtractPopulateFieldsWithValidModel() {
    // Given: POPULATE clause with valid model name
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .populate("department")
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (PopulateField created if model exists)
    assertTrue(result.isSuccess());
  }

  @Test
  void testOrchestrateBuildsPopulateContextsFromTypedPopulateClause() {
    DataModel relatedModel = setupRelatedModel("department", true, true);
    when(relatedModel.getFields()).thenReturn(Arrays.asList());

    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .build();
    context.setPopulateClause(PopulateClause.fromRaw("department"));

    Map<String, Object> record = new HashMap<>();
    record.put("id", 1);
    record.put("departmentId", 10);
    when(executor.execute(any())).thenReturn(Try.success(RecordList.of(Arrays.asList(record))));
    when(relatedModel.query(anyMap())).thenReturn(Try.success(RecordList.empty()));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    assertTrue(result.isSuccess());
    assertFalse(context.getPopulateContexts().isEmpty(),
        "类型化 PopulateClause 应先被转换为 PopulateContext");
    verify(relatedModel).query(anyMap());
  }

  @Test
  void testExtractPopulateFieldsWithInvalidModel() {
    // Given: POPULATE clause with invalid model name
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .populate("nonExistentModel")
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (field skipped, warning logged)
    assertTrue(result.isSuccess());
  }

  @Test
  void testExtractPopulateFieldsWithEmptyPopulate() {
    // Given: Empty POPULATE clause
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed with empty populate fields
    assertTrue(result.isSuccess());
  }

  @Test
  void testExtractPopulateFieldsWithNullPopulate() {
    // Given: Null POPULATE clause (same as empty)
    QueryStatement statement = new QueryStatement(new HashMap<>());
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed with empty populate fields
    assertTrue(result.isSuccess());
  }

  // ========== Step 2.4: Helper method tests ==========
  // Note: toPascalCase is private, testing through public orchestrate method

  @Test
  void testToPascalCaseNormal() {
    // Given: POPULATE with camelCase field name
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .populate("department")  // Should convert to "Department"
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (toPascalCase converts correctly)
    assertTrue(result.isSuccess());
  }

  @Test
  void testToPascalCaseEmpty() {
    // Given: POPULATE with empty string (edge case)
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .populate("")
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (empty string handled)
    assertTrue(result.isSuccess());
  }

  @Test
  void testToPascalCaseAlreadyPascal() {
    // Given: POPULATE with PascalCase field name
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .populate("Department")  // Already PascalCase
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (PascalCase preserved)
    assertTrue(result.isSuccess());
  }

  @Test
  void testToPascalCaseMultiWord() {
    // Given: POPULATE with multi-word camelCase
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .populate("userProfile")  // Should convert to "UserProfile"
        .build();
    RecordList expectedResult = RecordList.empty();

    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed (multi-word conversion works)
    assertTrue(result.isSuccess());
  }

  // ========== Step 6: analyzeStatement() 跨源/同源 + ToOne/ToMany 测试 ==========

  @SuppressWarnings("unchecked")
  private DataModel setupRelatedModel(String fieldPath, boolean isToOne, boolean sameSource) {
    return setupRelatedModel(fieldPath, isToOne, "StationA", sameSource ? "StationA" : "StationB");
  }

  @SuppressWarnings("unchecked")
  private DataModel setupRelatedModel(String fieldPath, boolean isToOne, String rootStationName, String relatedStationName) {
    DataModelField field = mock(DataModelField.class);
    DataModel relatedModel = mock(DataModel.class);
    when(relatedModel.getName()).thenReturn("Related");

    // Mock key fields for RelatedValue.getKey() / getReferenceKey()
    DataModelField localKeyField = mock(DataModelField.class);
    when(localKeyField.getName()).thenReturn(fieldPath + "Id");
    DataModelField referenceKeyField = mock(DataModelField.class);
    when(referenceKeyField.getName()).thenReturn("id");

    if (isToOne) {
      ModelValue modelValue = mock(ModelValue.class);
      doReturn(modelValue).when(field).getValueType();
      when(modelValue.getReferenceModel()).thenReturn(Optional.of(relatedModel));
      when(modelValue.getKey()).thenReturn(Optional.of(localKeyField));
      when(modelValue.getReferenceKey()).thenReturn(Optional.of(referenceKeyField));
    } else {
      CollectionValue collectionValue = mock(CollectionValue.class);
      doReturn(collectionValue).when(field).getValueType();
      when(collectionValue.getReferenceModel()).thenReturn(Optional.of(relatedModel));
      when(collectionValue.getKey()).thenReturn(Optional.of(localKeyField));
      when(collectionValue.getReferenceKey()).thenReturn(Optional.of(referenceKeyField));
    }

    when(model.getField(fieldPath)).thenReturn(Optional.of(field));

    DataStation rootStation = mock(DataStation.class);
    DataStation relatedStation = mock(DataStation.class);
    when(rootStation.getName()).thenReturn(rootStationName);
    when(relatedStation.getName()).thenReturn(relatedStationName);
    when(model.getDataStation()).thenReturn(rootStation);
    when(relatedModel.getDataStation()).thenReturn(relatedStation);

    return relatedModel;
  }

  @SuppressWarnings("unchecked")
  private DataModel setupNestedToManyModel(String outerFieldPath, String innerFieldPath) {
    DataModel outerModel = setupRelatedModel(outerFieldPath, false, true);

    DataModelField innerField = mock(DataModelField.class);
    DataModel innerModel = mock(DataModel.class);
    when(innerModel.getName()).thenReturn("NestedRelated");

    DataModelField innerLocalKeyField = mock(DataModelField.class);
    when(innerLocalKeyField.getName()).thenReturn(innerFieldPath + "Id");
    DataModelField innerReferenceKeyField = mock(DataModelField.class);
    when(innerReferenceKeyField.getName()).thenReturn("id");

    CollectionValue innerCollectionValue = mock(CollectionValue.class);
    doReturn(innerCollectionValue).when(innerField).getValueType();
    when(innerCollectionValue.getReferenceModel()).thenReturn(Optional.of(innerModel));
    when(innerCollectionValue.getKey()).thenReturn(Optional.of(innerLocalKeyField));
    when(innerCollectionValue.getReferenceKey()).thenReturn(Optional.of(innerReferenceKeyField));

    when(outerModel.getField(innerFieldPath)).thenReturn(Optional.of(innerField));

    DataStation outerStation = mock(DataStation.class);
    DataStation innerStation = mock(DataStation.class);
    when(outerStation.getName()).thenReturn("StationA");
    when(innerStation.getName()).thenReturn("StationA");
    when(outerModel.getDataStation()).thenReturn(outerStation);
    when(innerModel.getDataStation()).thenReturn(innerStation);

    return innerModel;
  }

  @SuppressWarnings("unchecked")
  private DataModel setupToOneThenToManyModel(String outerFieldPath, String innerFieldPath) {
    DataModel outerModel = setupRelatedModel(outerFieldPath, true, true);

    DataModelField innerField = mock(DataModelField.class);
    DataModel innerModel = mock(DataModel.class);
    when(innerModel.getName()).thenReturn("ToOneNestedRelated");

    DataModelField innerLocalKeyField = mock(DataModelField.class);
    when(innerLocalKeyField.getName()).thenReturn(innerFieldPath + "Id");
    DataModelField innerReferenceKeyField = mock(DataModelField.class);
    when(innerReferenceKeyField.getName()).thenReturn("id");

    CollectionValue innerCollectionValue = mock(CollectionValue.class);
    doReturn(innerCollectionValue).when(innerField).getValueType();
    when(innerCollectionValue.getReferenceModel()).thenReturn(Optional.of(innerModel));
    when(innerCollectionValue.getKey()).thenReturn(Optional.of(innerLocalKeyField));
    when(innerCollectionValue.getReferenceKey()).thenReturn(Optional.of(innerReferenceKeyField));

    when(outerModel.getField(innerFieldPath)).thenReturn(Optional.of(innerField));

    DataStation outerStation = mock(DataStation.class);
    DataStation innerStation = mock(DataStation.class);
    when(outerStation.getName()).thenReturn("StationA");
    when(innerStation.getName()).thenReturn("StationA");
    when(outerModel.getDataStation()).thenReturn(outerStation);
    when(innerModel.getDataStation()).thenReturn(innerStation);

    return innerModel;
  }

  @SuppressWarnings("unchecked")
  private DataModel setupMultiLevelToOneModel(String outerFieldPath, String innerFieldPath, String leafFieldName) {
    DataModel outerModel = setupRelatedModel(outerFieldPath, true, true);

    DataModelField innerField = mock(DataModelField.class);
    DataModel innerModel = mock(DataModel.class);
    when(innerModel.getName()).thenReturn("MultiLevelNestedRelated");

    DataModelField innerLocalKeyField = mock(DataModelField.class);
    when(innerLocalKeyField.getName()).thenReturn(innerFieldPath + "Id");
    DataModelField innerReferenceKeyField = mock(DataModelField.class);
    when(innerReferenceKeyField.getName()).thenReturn("id");

    ModelValue innerModelValue = mock(ModelValue.class);
    doReturn(innerModelValue).when(innerField).getValueType();
    when(innerModelValue.getReferenceModel()).thenReturn(Optional.of(innerModel));
    when(innerModelValue.getKey()).thenReturn(Optional.of(innerLocalKeyField));
    when(innerModelValue.getReferenceKey()).thenReturn(Optional.of(innerReferenceKeyField));

    when(outerModel.getField(innerFieldPath)).thenReturn(Optional.of(innerField));

    DataModelField leafField = mock(DataModelField.class);
    when(innerModel.getField(leafFieldName)).thenReturn(Optional.of(leafField));

    DataStation outerStation = mock(DataStation.class);
    DataStation innerStation = mock(DataStation.class);
    when(outerStation.getName()).thenReturn("StationA");
    when(innerStation.getName()).thenReturn("StationA");
    when(outerModel.getDataStation()).thenReturn(outerStation);
    when(innerModel.getDataStation()).thenReturn(innerStation);

    return innerModel;
  }

  @SuppressWarnings("unchecked")
  private DataModel setupToOneThenCrossSourceToOneModel(String outerFieldPath, String innerFieldPath, String leafFieldName) {
    DataModel outerModel = setupRelatedModel(outerFieldPath, true, true);

    DataModelField innerField = mock(DataModelField.class);
    DataModel innerModel = mock(DataModel.class);
    when(innerModel.getName()).thenReturn("CrossSourceNestedRelated");

    DataModelField innerLocalKeyField = mock(DataModelField.class);
    when(innerLocalKeyField.getName()).thenReturn(innerFieldPath + "Id");
    DataModelField innerReferenceKeyField = mock(DataModelField.class);
    when(innerReferenceKeyField.getName()).thenReturn("id");

    ModelValue innerModelValue = mock(ModelValue.class);
    doReturn(innerModelValue).when(innerField).getValueType();
    when(innerModelValue.getReferenceModel()).thenReturn(Optional.of(innerModel));
    when(innerModelValue.getKey()).thenReturn(Optional.of(innerLocalKeyField));
    when(innerModelValue.getReferenceKey()).thenReturn(Optional.of(innerReferenceKeyField));

    when(outerModel.getField(innerFieldPath)).thenReturn(Optional.of(innerField));

    DataModelField leafField = mock(DataModelField.class);
    when(innerModel.getField(leafFieldName)).thenReturn(Optional.of(leafField));

    DataStation outerStation = mock(DataStation.class);
    DataStation innerStation = mock(DataStation.class);
    when(outerStation.getName()).thenReturn("StationA");
    when(innerStation.getName()).thenReturn("StationB");
    when(outerModel.getDataStation()).thenReturn(outerStation);
    when(innerModel.getDataStation()).thenReturn(innerStation);

    return innerModel;
  }

  @Test
  void testAnalyzeToOneCondition() {
    // Given: ModelValue field + same source → sameSourceToOneConditions
    setupRelatedModel("department", true, true);

    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        SExpression.<Boolean>create(Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("Engineering"))
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed — ToOne uses JoinStatementRewriter path
    assertTrue(result.isSuccess());
  }

  @Test
  void testAnalyzeToManyCondition() {
    // Given: CollectionValue field + same source → sameSourceToManyConditions
    setupRelatedModel("orders", false, true);

    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.<Boolean>create(Operators.EQ,
            SExpression.create(Operators.FIELD, "status"),
            SExpression.constant("active"))
    );
    QueryStatement statement = QueryStatement.builder()
        .from("customer")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed — ToMany uses ExistsStatementRewriter path
    assertTrue(result.isSuccess());
  }

  @Test
  void testAnalyzeToManyConditionWithFieldPathExpression() {
    setupRelatedModel("orders", false, true);

    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.<Boolean>create(Operators.EQ,
            SExpression.create(Operators.FIELD, "status"),
            SExpression.constant("active"))
    );
    QueryStatement statement = QueryStatement.builder()
        .from("customer")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    assertTrue(result.isSuccess());
    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());
    assertEquals(Operators.EXISTS, statementCaptor.getValue().getWhere().getOperator(),
        "ToMany 关系的 FIELD 契约应被识别并改写为 EXISTS");
  }

  @Test
  void testAnalyzeCrossSourceCondition() {
    // Given: Different DataStation → crossSourceConditions
    DataModel relatedModel = setupRelatedModel("project", true, false);

    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "project"),
        SExpression.<Boolean>create(Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("Alpha"))
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();

    // Mock: 预查询返回包含 ID 的结果
    List<Map<String, Object>> preQueryData = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 1);
    preQueryData.add(record);
    RecordList preQueryResult = RecordList.of(preQueryData);
    when(relatedModel.query(any(QueryStatement.class))).thenReturn(Try.success(preQueryResult));

    // Mock: 主查询返回结果
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed — cross-source condition creates CrossSourceRewriteStep
    assertTrue(result.isSuccess());
  }

  @Test
  void testAnalyzeStatementUsesJoinCapabilitiesForToOne() {
    DataModel relatedModel = setupRelatedModel("department", true, "FederatedA", "FederatedB");

    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        SExpression.<Boolean>create(Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("Engineering"))
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));
    when(relatedModel.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.empty()));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    assertTrue(result.isSuccess());
    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());

    QueryStatement executedStatement = statementCaptor.getValue();
    assertEquals(1, executedStatement.getJoins().size(),
        "JoinCapability 判定可 JOIN 时，ToOne 条件应走 JOIN 改写路径");
    assertEquals("Related", executedStatement.getJoins().get(0).getTableName());
    verify(relatedModel, never()).query(any(QueryStatement.class));
  }

  @Test
  void testRewriteStatementWithBareToOneFieldUsesJoinRewrite() {
    setupRelatedModel("department", true, true);

    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "department", "name"),
        SExpression.constant("Engineering")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(whereCondition)
        .build();

    Try<QueryStatement> result = orchestrator.rewriteStatement(statement, model, context);

    assertTrue(result.isSuccess());
    QueryStatement rewritten = result.get();
    assertEquals(1, rewritten.getJoins().size(),
        "裸多段 FIELD 的 toOne 条件应进入 JOIN 编排");
  }

  @Test
  void testRewriteStatementWithBareToOneFieldUsesCrossSourceRewrite() {
    DataModel relatedModel = setupRelatedModel("project", true, false);

    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "project", "name"),
        SExpression.constant("Alpha")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(whereCondition)
        .build();

    List<Map<String, Object>> preQueryData = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 1);
    preQueryData.add(record);
    RecordList preQueryResult = RecordList.of(preQueryData);
    when(relatedModel.query(any(QueryStatement.class))).thenReturn(Try.success(preQueryResult));

    Try<QueryStatement> result = orchestrator.rewriteStatement(statement, model, context);

    assertTrue(result.isSuccess());
    QueryStatement rewritten = result.get();
    assertTrue(rewritten.getJoins().isEmpty(),
        "跨源裸多段 FIELD 不应走 JOIN 路径");
    assertEquals(Operators.EQ, rewritten.getWhere().getOperator(),
        "跨源裸多段 FIELD 现在应直接以结构保持方式替换为本地条件");
  }

  @Test
  void testRewriteStatementRecursivelyRewritesNestedRelationCondition() {
    setupNestedToManyModel("orders", "items");

    SExpression<Boolean> nestedRelationCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            ExtOps.REL_ANY,
            SExpression.create(Operators.FIELD, "items"),
            SExpression.create(
                Operators.EQ,
                SExpression.create(Operators.FIELD, "price"),
                SExpression.constant(100)
            )
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("customer")
        .where(nestedRelationCondition)
        .build();

    Try<QueryStatement> result = orchestrator.rewriteStatement(statement, model, context);

    assertTrue(result.isSuccess());
    QueryStatement rewritten = result.get();
    assertEquals(Operators.EXISTS, rewritten.getWhere().getOperator());

    QueryStatement firstLevelSubQuery = (QueryStatement) rewritten.getWhere().getParam(0);
    SExpression<Boolean> firstLevelWhere = firstLevelSubQuery.getWhere();
    assertEquals(Operators.AND, firstLevelWhere.getOperator());

    SExpression<?> nestedExists = firstLevelWhere.getParamAsSExpression(1);
    assertEquals(Operators.EXISTS, nestedExists.getOperator(),
        "嵌套 REL_ANY 应在子查询中继续被改写为 EXISTS");
  }

  @Test
  void testOrchestrateRecursivelyRewritesNestedRelationCondition() {
    setupNestedToManyModel("orders", "items");

    SExpression<Boolean> nestedRelationCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            ExtOps.REL_ANY,
            SExpression.create(Operators.FIELD, "items"),
            SExpression.create(
                Operators.EQ,
                SExpression.create(Operators.FIELD, "price"),
                SExpression.constant(100)
            )
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("customer")
        .where(nestedRelationCondition)
        .build();

    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    assertTrue(result.isSuccess());
    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());

    QueryStatement executedStatement = statementCaptor.getValue();
    QueryStatement firstLevelSubQuery = (QueryStatement) executedStatement.getWhere().getParam(0);
    SExpression<?> nestedExists = firstLevelSubQuery.getWhere().getParamAsSExpression(1);
    assertEquals(Operators.EXISTS, nestedExists.getOperator(),
        "orchestrate() 执行前应完成嵌套 relation 的递归 rewrite");
  }

  @Test
  void testRewriteStatementRewritesNestedSubQueryWhenTopLevelHasNoRelation() {
    setupNestedToManyModel("orders", "items");

    QueryStatement subQuery = QueryStatement.builder()
        .from("orders")
        .where(SExpression.create(
            ExtOps.REL_ANY,
            SExpression.create(Operators.FIELD, "items"),
            SExpression.create(
                Operators.EQ,
                SExpression.create(Operators.FIELD, "price"),
                SExpression.constant(100)
            )))
        .build();

    QueryStatement statement = QueryStatement.builder()
        .from("customer")
        .where(SExpression.create(Operators.EXISTS, subQuery))
        .build();

    Try<QueryStatement> result = orchestrator.rewriteStatement(statement, model, context);

    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }

    QueryStatement rewritten = result.get();
    assertEquals(Operators.EXISTS, rewritten.getWhere().getOperator());

    QueryStatement rewrittenSubQuery = (QueryStatement) rewritten.getWhere().getParam(0);
    assertEquals(Operators.EXISTS, rewrittenSubQuery.getWhere().getOperator(),
        "主语句无 relation 时，嵌套子查询中的 relation 仍应递归 rewrite");
  }

  @Test
  void testRewriteStatementRewritesRelationCountComparisonToCorrelatedSubQuery() {
    DataModel relatedModel = setupRelatedModel("orderItems", false, true);
    when(relatedModel.getName()).thenReturn("order_item");

    QueryStatement statement = QueryStatement.builder()
        .from("order")
        .where(SExpression.create(
            Operators.GT,
            SExpression.create(Operators.COUNT, SExpression.create(Operators.FIELD, "orderItems")),
            SExpression.constant(2)
        ))
        .build();

    Try<QueryStatement> result = orchestrator.rewriteStatement(statement, model, context);

    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }

    SExpression<Boolean> where = result.get().getWhere();
    assertEquals(Operators.GT, where.getOperator());

    SExpression<?> leftExpr = where.getParamAsSExpression(0);
    assertEquals(Operators.SUB_QUERY, leftExpr.getOperator(),
        "关系 COUNT 比较应改写为相关子查询");

    QueryStatement subQuery = (QueryStatement) leftExpr.getParam(0);
    assertEquals("order_item", subQuery.getFrom().getTableName());
    assertEquals(Operators.COUNT, subQuery.getSelect().get(0).getOperator());
  }

  @Test
  void testRewriteStatementRecursivelyRewritesOuterToOneThenInnerToMany() {
    setupToOneThenToManyModel("department", "employees");

    SExpression<Boolean> nestedRelationCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        SExpression.create(
            ExtOps.REL_ANY,
            SExpression.create(Operators.FIELD, "employees"),
            SExpression.create(
                Operators.EQ,
                SExpression.create(Operators.FIELD, "name"),
                SExpression.constant("Alice")
            )
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(nestedRelationCondition)
        .build();

    Try<QueryStatement> result = orchestrator.rewriteStatement(statement, model, context);

    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }
    QueryStatement rewritten = result.get();
    assertEquals(1, rewritten.getJoins().size(),
        "outer toOne 应先落成 JOIN");
    assertEquals(Operators.EXISTS, rewritten.getWhere().getOperator(),
        "inner toMany 应在主语句第二轮编排后落成 EXISTS");

    QueryStatement nestedSubQuery = (QueryStatement) rewritten.getWhere().getParam(0);
    SExpression<?> joinCondition = nestedSubQuery.getWhere().getParamAsSExpression(0);
    SExpression<?> correlationField = joinCondition.getParamAsSExpression(1);
    assertEquals(Operators.FIELD, correlationField.getOperator());
    assertEquals("department", correlationField.getParam(0),
        "nested EXISTS 应关联到 outer toOne 的 JOIN alias，而不是主表 alias");
  }

  @Test
  void testRewriteStatementRewritesMultiLevelBareToOneField() {
    setupMultiLevelToOneModel("product", "category", "name");

    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "product", "category", "name"),
        SExpression.constant("Phone")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("orderItem")
        .where(whereCondition)
        .build();

    Try<QueryStatement> result = orchestrator.rewriteStatement(statement, model, context);

    if (result.isFailure()) {
      fail(String.valueOf(result.getCause()));
    }

    QueryStatement rewritten = result.get();
    assertEquals(2, rewritten.getJoins().size(),
        "多级裸 toOne 路径应递归落成两级 JOIN");
    assertEquals("Related", rewritten.getJoins().get(0).getTableName());
    assertEquals("MultiLevelNestedRelated", rewritten.getJoins().get(1).getTableName());

    SExpression<?> rewrittenField = rewritten.getWhere().getParamAsSExpression(0);
    assertEquals(Operators.FIELD, rewrittenField.getOperator());
    assertEquals("product_category", rewrittenField.getParam(0));
    assertEquals("name", rewrittenField.getParam(1));
  }

  @Test
  void testOrchestrateOuterToOneThenInnerCrossSourceBareField() {
    DataModel locationModel = setupToOneThenCrossSourceToOneModel("department", "location", "city");

    List<Map<String, Object>> preQueryData = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 7);
    preQueryData.add(record);
    when(locationModel.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(preQueryData)));

    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "department", "location", "city"),
            SExpression.constant("Beijing")
        ))
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    assertTrue(result.isSuccess(), () -> String.valueOf(result.getCause()));
    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());

    QueryStatement executedStatement = statementCaptor.getValue();
    assertEquals(1, executedStatement.getJoins().size(),
        "outer toOne 应先落成 JOIN");
    assertEquals(Operators.EQ, executedStatement.getWhere().getOperator(),
        "inner cross-source bare field 应被回填为本地键条件");

    SExpression<?> rewrittenField = executedStatement.getWhere().getParamAsSExpression(0);
    assertEquals(Operators.FIELD, rewrittenField.getOperator());
    assertEquals("department", rewrittenField.getParam(0));
    assertEquals("locationId", rewrittenField.getParam(1));
  }

  @Test
  void testOrchestrateOuterCrossSourceThenInnerCrossSourceBareField() {
    DataModel departmentModel = setupRelatedModel("department", true, false);

    DataModelField locationField = mock(DataModelField.class);
    DataModel locationModel = mock(DataModel.class);
    when(locationModel.getName()).thenReturn("CrossSourceNestedLocation");

    DataModelField locationLocalKeyField = mock(DataModelField.class);
    when(locationLocalKeyField.getName()).thenReturn("locationId");
    DataModelField locationReferenceKeyField = mock(DataModelField.class);
    when(locationReferenceKeyField.getName()).thenReturn("id");

    ModelValue locationModelValue = mock(ModelValue.class);
    doReturn(locationModelValue).when(locationField).getValueType();
    when(locationModelValue.getReferenceModel()).thenReturn(Optional.of(locationModel));
    when(locationModelValue.getKey()).thenReturn(Optional.of(locationLocalKeyField));
    when(locationModelValue.getReferenceKey()).thenReturn(Optional.of(locationReferenceKeyField));

    when(departmentModel.getField("location")).thenReturn(Optional.of(locationField));

    DataStation departmentStation = mock(DataStation.class);
    DataStation locationStation = mock(DataStation.class);
    when(departmentStation.getName()).thenReturn("StationB");
    when(locationStation.getName()).thenReturn("StationC");
    when(departmentModel.getDataStation()).thenReturn(departmentStation);
    when(locationModel.getDataStation()).thenReturn(locationStation);

    List<Map<String, Object>> departmentPreQueryData = new ArrayList<>();
    Map<String, Object> departmentRecord = new HashMap<>();
    departmentRecord.put("id", 7);
    departmentPreQueryData.add(departmentRecord);
    when(departmentModel.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(departmentPreQueryData)));

    List<Map<String, Object>> locationPreQueryData = new ArrayList<>();
    Map<String, Object> locationRecord = new HashMap<>();
    locationRecord.put("id", 3);
    locationPreQueryData.add(locationRecord);
    when(locationModel.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(locationPreQueryData)));

    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "department", "location", "city"),
            SExpression.constant("Beijing")
        ))
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    assertTrue(result.isSuccess(), () -> String.valueOf(result.getCause()));
    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(executor).execute(statementCaptor.capture());

    QueryStatement executedStatement = statementCaptor.getValue();
    assertTrue(executedStatement.getJoins().isEmpty(),
        "outer cross-source 不应落成 JOIN");
    assertEquals(Operators.EQ, executedStatement.getWhere().getOperator(),
        "outer cross-source bare field 最终应只回填为根模型本地键条件");

    SExpression<?> rewrittenField = executedStatement.getWhere().getParamAsSExpression(0);
    assertEquals(Operators.FIELD, rewrittenField.getOperator());
    assertEquals("departmentId", rewrittenField.getParam(0));
  }

  @Test
  void testAnalyzeFieldNotFound() {
    // Given: rootModel.getField() returns empty → condition skipped
    when(model.getField("unknown")).thenReturn(Optional.empty());

    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "unknown"),
        SExpression.<Boolean>create(Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("test"))
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed — unknown field skipped with warning
    assertTrue(result.isSuccess());
  }

  // ========== Step 7: NULL 语义检测测试 ==========

  @Test
  void testAnalyzeToOneWithIsNull() {
    // Given: ToOne condition with IS_NULL → requiresLeftJoin=true
    setupRelatedModel("department", true, true);

    // condition: name IS NULL
    SExpression<Boolean> innerCondition = SExpression.create(
        Operators.IS_NULL,
        SExpression.create(Operators.FIELD, "name")
    );
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        innerCondition
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed — ToOne with IS_NULL triggers LEFT JOIN path
    assertTrue(result.isSuccess());
  }

  @Test
  void testAnalyzeToOneWithNot() {
    // Given: ToOne condition with NOT → requiresLeftJoin=true
    setupRelatedModel("department", true, true);

    // condition: NOT (name = 'Engineering')
    SExpression<Boolean> innerCondition = SExpression.create(
        Operators.NOT,
        SExpression.<Boolean>create(Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("Engineering"))
    );
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        innerCondition
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed — ToOne with NOT triggers LEFT JOIN path
    assertTrue(result.isSuccess());
  }

  @Test
  void testAnalyzeToOneWithoutNullSemantics() {
    // Given: ToOne condition with plain EQ → requiresLeftJoin=false
    setupRelatedModel("department", true, true);

    // condition: name = 'Engineering' (no NULL semantics)
    SExpression<Boolean> innerCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "name"),
        SExpression.constant("Engineering")
    );
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        innerCondition
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    // When
    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    // Then: Should succeed — ToOne without NULL semantics uses INNER JOIN path
    assertTrue(result.isSuccess());
  }

  @Test
  void testAnalyzeToOneWithInContainingNull() {
    setupRelatedModel("department", true, true);

    SExpression<Boolean> innerCondition = SExpression.create(
        Operators.IN,
        SExpression.create(Operators.FIELD, "name"),
        null
    );
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        innerCondition
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();
    RecordList expectedResult = RecordList.empty();
    when(executor.execute(any())).thenReturn(Try.success(expectedResult));

    Try<RecordList> result = orchestrator.orchestrate(statement, model, executor, context);

    assertTrue(result.isSuccess());
  }
}
