package com.ouroboros.data.model.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.EnhancedDataModelProxy;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;

public class LogicalDeleteDataModelPluginTest {

  @Test
  public void testInsertShouldInitializeLogicalDeleteFieldAndDropDeleteAuditFields() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", "deletedAt", "deletedBy");
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<Record> result = plugin.insert(linkedMap("id", "A1", "deletedAt", "bad-time", "deletedBy", "bad-user"), context);

    assertTrue(result.isSuccess());
    assertEquals(Boolean.FALSE, nextPlugin.insertedData.get("isDeleted"));
    assertFalse(nextPlugin.insertedData.containsKey("deletedAt"));
    assertFalse(nextPlugin.insertedData.containsKey("deletedBy"));
  }

  @Test
  public void testBatchInsertShouldInitializeLogicalDeleteField() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", null, null);
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<RecordList> result = plugin.batchInsert(Collections.singletonList(linkedMap("id", "A1")), context);

    assertTrue(result.isSuccess());
    assertEquals(Boolean.FALSE, nextPlugin.batchInsertedData.get(0).get("isDeleted"));
  }

  @Test
  public void testUpdateShouldPreventChangingLogicalDeleteFields() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", "deletedAt", "deletedBy");
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);
    SExpression<Boolean> where = eq("id", "A1");
    Map<String, Object> data = linkedMap("name", "new-name", "isDeleted", true, "deletedAt", "bad-time", "deletedBy", "bad-user");

    Try<Long> result = plugin.update(where, data, context);

    assertTrue(result.isSuccess());
    assertEquals(where, nextPlugin.updatedWhere);
    assertEquals(Collections.singletonMap("name", "new-name"), nextPlugin.updatedData);
  }

  @Test
  public void testDeleteShouldRewriteToUpdate() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", "deletedAt", "deletedBy");
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);
    SExpression<Boolean> where = eq("id", "A1");

    Try<Long> result = plugin.delete(where, context);

    assertTrue(result.isSuccess());
    assertEquals(where, nextPlugin.updatedWhere);
    assertEquals(Boolean.TRUE, nextPlugin.updatedData.get("isDeleted"));
    assertTrue(nextPlugin.updatedData.containsKey("deletedAt"));
    assertEquals(TestDataOperationIdentityProvider.OPERATOR, nextPlugin.updatedData.get("deletedBy"));
  }

  @Test
  public void testDeleteShouldAllowMinimalFieldConfiguration() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", null, null);
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<Long> result = plugin.delete(eq("id", "A1"), context);

    assertTrue(result.isSuccess());
    assertEquals(Boolean.TRUE, nextPlugin.updatedData.get("isDeleted"));
    assertEquals(Collections.singleton("isDeleted"), nextPlugin.updatedData.keySet());
  }

  @Test
  public void testCountShouldAppendLogicalDeleteFilter() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", "deletedAt", "deletedBy");
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<Long> result = plugin.count(QueryStatement.builder().where(eq("id", "A1")).build(), context);

    assertTrue(result.isSuccess());
    assertTrue(containsConstant(nextPlugin.countedWhere, Boolean.FALSE));
    assertTrue(containsConstant(nextPlugin.countedWhere, "A1"));
  }

  @Test
  public void testCountShouldKeepExistingLogicalDeleteFilter() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", null, null);
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);
    SExpression<Boolean> where = SExpression.create(
        Operators.OR,
        eq("isDeleted", Boolean.TRUE),
        eq("id", "A1"));

    Try<Long> result = plugin.count(QueryStatement.builder().where(where).build(), context);

    assertTrue(result.isSuccess());
    assertEquals(where, nextPlugin.countedWhere);
  }

  @Test
  public void testQueryShouldAppendLogicalDeleteFilter() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    RecordList expected = RecordList.of(Collections.emptyList());
    nextPlugin.queryResult = Try.success(expected);
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", "deletedAt", "deletedBy");
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<RecordList> result = plugin.query(QueryStatement.builder().where(eq("id", "A1")).build(), context);

    assertTrue(result.isSuccess());
    assertTrue(containsConstant(nextPlugin.queryWhere, Boolean.FALSE));
    assertTrue(containsConstant(nextPlugin.queryWhere, "A1"));
    assertSame(expected, result.get());
  }

  @Test
  public void testTypedQueryShouldAppendLogicalDeleteExpressionWithoutDroppingCallerWhere() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", null, null);
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);
    SExpression<Boolean> callerWhere = eq("id", "A1");
    QueryStatement statement = QueryStatement.builder().where(callerWhere).build();

    Try<RecordList> result = plugin.query(statement, context);

    assertTrue(result.isSuccess());
    assertTrue(containsExpression(nextPlugin.queryWhere, callerWhere));
    assertTrue(containsConstant(nextPlugin.queryWhere, Boolean.FALSE));
    assertEquals(callerWhere, statement.getWhere());
  }

  @Test
  public void testTypedQueryShouldKeepExplicitLogicalDeleteExpression() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", null, null);
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);
    QueryStatement statement = QueryStatement.builder()
        .where(eq("isDeleted", Boolean.TRUE))
        .build();

    Try<RecordList> result = plugin.query(statement, context);

    assertTrue(result.isSuccess());
    assertEquals(statement.getWhere(), nextPlugin.queryWhere);
  }

  @Test
  public void testTypedQueryShouldRecognizeRootAliasInBothFieldRepresentations() {
    List<SExpression<?>> rootFields = Arrays.asList(
        SExpression.field("u", "isDeleted"),
        SExpression.field("u.isDeleted")
    );

    for (SExpression<?> rootField : rootFields) {
      DataModel coreDataModel = mock(DataModel.class);
      stubFields(coreDataModel, "id", "isDeleted");
      when(coreDataModel.query(any(QueryStatement.class)))
          .thenReturn(Try.success(RecordList.empty()));
      DataModel proxy = new EnhancedDataModelProxy(
          coreDataModel,
          Collections.singletonList(new PluginDescriptor(
              "LogicalDelete",
              Collections.singletonMap("isDeletedField", "isDeleted"))));
      QueryStatement statement = QueryStatement.builder()
          .from("users", "u")
          .where(SExpression.create(
              Operators.EQ, rootField, SExpression.constant(Boolean.TRUE)))
          .build();

      Try<RecordList> result = proxy.query(statement);

      assertTrue(result.isSuccess());
      org.mockito.ArgumentCaptor<QueryStatement> statementCaptor = org.mockito.ArgumentCaptor.forClass(QueryStatement.class);
      verify(coreDataModel).query(statementCaptor.capture());
      assertEquals(statement.getWhere(), statementCaptor.getValue().getWhere());
    }
  }

  @Test
  public void testTypedQueryShouldNotTreatJoinedAliasAsRootLogicalDeleteField() {
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", null, null);
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);
    SExpression<Boolean> callerWhere = SExpression.create(
        Operators.EQ,
        SExpression.field("orders", "isDeleted"),
        SExpression.constant(Boolean.TRUE));
    QueryStatement statement = QueryStatement.builder()
        .from("users", "u")
        .where(callerWhere)
        .build();

    Try<RecordList> result = plugin.query(statement, context);

    assertTrue(result.isSuccess());
    assertTrue(containsExpression(nextPlugin.queryWhere, callerWhere));
    assertTrue(containsConstant(nextPlugin.queryWhere, Boolean.FALSE));
  }

  @Test
  public void testTypedRelationConditionsShouldNotCountRelatedLogicalDeleteFieldAsRootField() {
    for (com.querydsl.core.types.Operator relationOperator
        : Arrays.asList(ExtOps.REL_ANY, ExtOps.REL_ALL, ExtOps.REL_NONE)) {
      RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
      LogicalDeleteDataModelPlugin plugin = new LogicalDeleteDataModelPlugin("isDeleted", null, null);
      DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);
      SExpression<Boolean> relationCondition = SExpression.create(
          relationOperator,
          SExpression.field("orders"),
          SExpression.create(
              Operators.EQ,
              SExpression.field("isDeleted"),
              SExpression.constant(Boolean.TRUE)));
      QueryStatement statement = QueryStatement.builder()
          .from("users", "u")
          .where(relationCondition)
          .build();

      Try<RecordList> result = plugin.query(statement, context);

      assertTrue(result.isSuccess());
      assertTrue(containsExpression(nextPlugin.queryWhere, relationCondition));
      assertTrue(containsConstant(nextPlugin.queryWhere, Boolean.FALSE));
    }
  }

  @Test
  public void testTypedRootAliasShouldFailClosedWhenLaterPluginReplacesFrom() {
    DataModel coreDataModel = mock(DataModel.class);
    stubFields(coreDataModel, "id", "isDeleted");
    ProbeDataModelPlugin.REPLACE_FROM = true;
    DataModel proxy = new EnhancedDataModelProxy(
        coreDataModel,
        Arrays.asList(
            new PluginDescriptor(
                "LogicalDelete",
                Collections.singletonMap("isDeletedField", "isDeleted")),
            new PluginDescriptor("Probe")));
    QueryStatement statement = QueryStatement.builder()
        .from("users", "orders")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.field("orders", "isDeleted"),
            SExpression.constant(Boolean.FALSE)))
        .build();

    Try<RecordList> result;
    try {
      result = proxy.query(statement);
    } finally {
      ProbeDataModelPlugin.reset();
    }

    assertTrue(result.isFailure());
    verify(coreDataModel, never()).query(anyMap());
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void testPluginDescriptorShouldBuildViaProxyAndRewriteDeleteToUpdate() {
    DataModel coreDataModel = mock(DataModel.class);
    stubFields(coreDataModel, "id", "isDeleted");
    when(coreDataModel.update(any(SExpression.class), anyMap())).thenReturn(Try.success(1L));

    DataModel proxy = new EnhancedDataModelProxy(
        coreDataModel,
        Collections.singletonList(new PluginDescriptor("LogicalDelete", Collections.singletonMap("isDeletedField", "isDeleted")))
    );

    Try<Long> result = proxy.delete(Collections.singletonMap("id", "A1"));

    assertTrue(result.isSuccess());
  }

  @Test
  public void testLegacySoftDeleteDescriptorShouldBuildViaProxyAndRewriteDeleteToUpdate() {
    DataModel coreDataModel = mock(DataModel.class);
    stubFields(coreDataModel, "id", "isDeleted");
    when(coreDataModel.update(any(SExpression.class), anyMap())).thenReturn(Try.success(1L));

    DataModel proxy = new EnhancedDataModelProxy(
        coreDataModel,
        Collections.singletonList(new PluginDescriptor("SoftDelete", Collections.singletonMap("isDeletedField", "isDeleted")))
    );

    Try<Long> result = proxy.delete(Collections.singletonMap("id", "A1"));

    assertTrue(result.isSuccess());
  }

  @Test
  public void testBuilderShouldSupportLogicalAndLegacySoftDeleteNames() {
    assertTrue(new LogicalDeleteDataModelPlugin.Builder().support("LogicalDelete"));
    assertTrue(new LogicalDeleteDataModelPlugin.Builder().support("SoftDelete"));
  }

  @Test
  public void testBuilderShouldRejectModelWithoutLogicalDeleteField() {
    DataModel dataModel = mock(DataModel.class);
    stubFields(dataModel, "id", "name");

    assertFalse(new LogicalDeleteDataModelPlugin.Builder()
        .build(dataModel, Collections.singletonMap("isDeletedField", "isDeleted"))
        .isPresent());
  }

  @Test
  public void testBuilderShouldRejectBlankLogicalDeleteField() {
    DataModel dataModel = mock(DataModel.class);
    stubFields(dataModel, "id", "isDeleted");

    assertFalse(new LogicalDeleteDataModelPlugin.Builder()
        .build(dataModel, Collections.singletonMap("isDeletedField", ""))
        .isPresent());
  }

  @Test
  public void testBuilderShouldDisableConfiguredAuditFieldWhenMissing() {
    DataModel dataModel = mock(DataModel.class);
    stubFields(dataModel, "id", "isDeleted");

    assertTrue(new LogicalDeleteDataModelPlugin.Builder()
        .build(dataModel, Collections.singletonMap("deletedAtField", "deletedAt"))
        .isPresent());
  }

  @Test
  public void testBuilderShouldDisableMissingAuditFieldForDeletePatch() {
    DataModel dataModel = mock(DataModel.class);
    stubFields(dataModel, "id", "isDeleted");
    DataModelPlugin plugin = new LogicalDeleteDataModelPlugin.Builder()
        .build(dataModel, Collections.singletonMap("deletedAtField", "deletedAt"))
        .get();
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<Long> result = plugin.delete(eq("id", "A1"), context);

    assertTrue(result.isSuccess());
    assertEquals(Collections.singleton("isDeleted"), nextPlugin.updatedData.keySet());
  }

  private Map<String, Object> linkedMap(Object... pairs) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put((String) pairs[i], pairs[i + 1]);
    }
    return map;
  }

  private static SExpression<Boolean> eq(String field, Object value) {
    return SExpression.create(Operators.EQ, SExpression.field(field), SExpression.constant(value));
  }

  private static boolean containsExpression(SExpression<?> expression, SExpression<?> expected) {
    return contains(expression, expected::equals);
  }

  private static boolean containsConstant(SExpression<?> expression, Object expected) {
    return contains(expression, node -> node.getOperator() == Operators.CONSTANT
        && !node.getParams().isEmpty()
        && java.util.Objects.equals(node.getParam(0), expected));
  }

  private static boolean contains(SExpression<?> expression, java.util.function.Predicate<SExpression<?>> predicate) {
    if (expression == null || expression.isEmpty()) {
      return false;
    }
    final boolean[] found = new boolean[] {false};
    expression.walk((node, context) -> {
      if (predicate.test(node)) {
        found[0] = true;
      }
    });
    return found[0];
  }

  private void stubFields(DataModel dataModel, String... names) {
    List<DataModelField> fields = new ArrayList<>();
    for (String name : names) {
      DataModelField field = mock(DataModelField.class);
      when(field.getName()).thenReturn(name);
      fields.add(field);
    }
    when(dataModel.getFields()).thenReturn(fields);
    when(dataModel.getFullName()).thenReturn("demo.Test");
    when(dataModel.getRawName()).thenReturn("demo_test");
  }

  private static class RecordingTailPlugin implements DataModelPlugin {
    private SExpression<Boolean> countedWhere;
    private SExpression<Boolean> queryWhere;
    private Map<String, Object> insertedData;
    private List<Map<String, Object>> batchInsertedData;
    private SExpression<Boolean> updatedWhere;
    private Map<String, Object> updatedData;
    private Try<RecordList> queryResult = Try.success(RecordList.of(Collections.emptyList()));

    @Override
    public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
      insertedData = new LinkedHashMap<>(data);
      return Try.success(Record.of(insertedData));
    }

    @Override
    public Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
      batchInsertedData = dataList.stream().map(LinkedHashMap::new).collect(Collectors.toList());
      return Try.success(RecordList.of(batchInsertedData));
    }

    @Override
    public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
      updatedWhere = where;
      updatedData = new LinkedHashMap<>(data);
      return Try.success(1L);
    }

    @Override
    public Try<Long> count(QueryStatement statement, DataModelPluginContext context) {
      countedWhere = statement.getWhere();
      return Try.success(1L);
    }

    @Override
    public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
      queryWhere = statement.getWhere();
      return queryResult;
    }
  }
}
