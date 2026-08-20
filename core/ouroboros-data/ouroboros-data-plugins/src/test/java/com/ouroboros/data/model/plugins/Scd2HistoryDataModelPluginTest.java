package com.ouroboros.data.model.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.valuetypes.BooleanValue;
import com.ouroboros.data.model.valuetypes.DateTimeValue;
import com.ouroboros.data.model.valuetypes.LongValue;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;

public class Scd2HistoryDataModelPluginTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-18T01:02:03Z"), ZoneOffset.UTC);
  private static final String OPERATOR = TestDataOperationIdentityProvider.OPERATOR;

  @Test
  public void builderShouldFailFastOnInvalidConfigs() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    DataModel historyModel = createHistoryModel(false);
    Scd2HistoryDataModelPlugin.Builder builder = new Scd2HistoryDataModelPlugin.Builder(FIXED_CLOCK, modelResolver(historyModel));

    assertThrows(IllegalArgumentException.class, () -> builder.build(sourceModel, Collections.<String, Object>emptyMap()));
    assertThrows(IllegalArgumentException.class, () -> builder.build(sourceModel, configWithoutRequiredColumn()));
    assertThrows(IllegalArgumentException.class, () -> builder.build(createSourceModelWithCompositePrimaryKey(), baseConfig()));
    assertThrows(IllegalArgumentException.class, () -> builder.build(sourceModel, configWithStoredDiff()));
  }

  @Test
  public void builderShouldFailFastWhenHistoryModelAlsoEnablesScd2() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    DataModel historyModel = createHistoryModel(false);
    when(historyModel.getExtraProp(Boolean.class, "enableScd2History")).thenReturn(Optional.of(true));

    Scd2HistoryDataModelPlugin.Builder builder = new Scd2HistoryDataModelPlugin.Builder(FIXED_CLOCK, modelResolver(historyModel));

    assertThrows(IllegalArgumentException.class, () -> builder.build(sourceModel, baseConfig()));
  }

  @Test
  public void updateShouldSkipHistoryWhenOnlyIgnoredFieldsChange() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    DataModel historyModel = createHistoryModel(false);
    DataModel rawHistoryModel = wireRawHistoryModel(historyModel);
    Scd2HistoryDataModelPlugin plugin = createPlugin(sourceModel, historyModel, configWithIgnoreFields());
    TailPlugin tail = new TailPlugin(
        query -> Try.failure(new UnsupportedOperationException()),
        data -> Try.failure(new UnsupportedOperationException()),
        (where, data) -> Try.success(1L),
        where -> Try.failure(new UnsupportedOperationException()));

    when(sourceModel.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(Collections.singletonList(record(
        "id", 1L,
        "name", "Alice",
        "updatedAt", "2026-03-18T08:00:00",
        "updatedBy", "tester-1")))));
    when(sourceModel.query(anyList())).thenReturn(Try.success(RecordList.of(Collections.singletonList(record(
        "id", 1L,
        "name", "Alice",
        "updatedAt", "2026-03-18T09:00:00",
        "updatedBy", "tester-2")))));

    Try<Long> result = plugin.update(eq("id", 1L), record(
        "updatedAt", "2026-03-18T09:00:00",
        "updatedBy", "tester-2"), PluginTestContexts.withNext(tail));

    assertTrue(result.isSuccess());
    assertEquals(Long.valueOf(1L), result.get());
    verify(rawHistoryModel, never()).update(anyMap(), anyMap());
    verify(rawHistoryModel, never()).insert(anyMap());
  }

  @Test
  public void updateShouldCloseCurrentVersionAndInsertNewVersionWithoutPersistingDiffByDefault() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    DataModel historyModel = createHistoryModel(false);
    DataModel rawHistoryModel = wireRawHistoryModel(historyModel);
    Scd2HistoryDataModelPlugin plugin = createPlugin(sourceModel, historyModel, baseConfig());
    TailPlugin tail = new TailPlugin(
        query -> Try.failure(new UnsupportedOperationException()),
        data -> Try.failure(new UnsupportedOperationException()),
        (where, data) -> Try.success(1L),
        where -> Try.failure(new UnsupportedOperationException()));

    when(sourceModel.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(Collections.singletonList(record(
        "id", 1L,
        "name", "Alice",
        "updatedAt", "2026-03-18T08:00:00",
        "updatedBy", "tester-1")))));
    when(sourceModel.query(anyList())).thenReturn(Try.success(RecordList.of(Collections.singletonList(record(
        "id", 1L,
        "name", "Bob",
        "updatedAt", "2026-03-18T09:00:00",
        "updatedBy", "tester-2")))));

    Try<Long> result = plugin.update(eq("id", 1L), record(
        "name", "Bob",
        "updatedAt", "2026-03-18T09:00:00",
        "updatedBy", "tester-2"), PluginTestContexts.withNext(tail));

    assertTrue(result.isSuccess());

    ArgumentCaptor<Map<String, Object>> closeWhereCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Map<String, Object>> closeDataCaptor = ArgumentCaptor.forClass(Map.class);
    verify(rawHistoryModel).update(closeWhereCaptor.capture(), closeDataCaptor.capture());
    assertEquals(Long.valueOf(1L), closeWhereCaptor.getValue().get("businessKey"));
    assertEquals(Boolean.TRUE, closeWhereCaptor.getValue().get("isCurrent"));
    assertEquals(Boolean.FALSE, closeDataCaptor.getValue().get("isCurrent"));

    ArgumentCaptor<Map<String, Object>> insertCaptor = ArgumentCaptor.forClass(Map.class);
    verify(rawHistoryModel).insert(insertCaptor.capture());
    Map<String, Object> insertedVersion = insertCaptor.getValue();
    assertEquals("UPDATE", insertedVersion.get("opType"));
    assertEquals(OPERATOR, insertedVersion.get("operator"));
    assertEquals(Long.valueOf(1L), insertedVersion.get("businessKey"));
    assertEquals("Bob", insertedVersion.get("name"));
    assertTrue(!insertedVersion.containsKey("id"));
    assertTrue(!insertedVersion.containsKey("changedFields"));
    assertTrue(!insertedVersion.containsKey("changeSet"));
  }

  @Test
  public void updateShouldQuerySnapshotsThroughPluginFreeSourceModel() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    DataModel historyModel = createHistoryModel(false);
    wireRawHistoryModel(historyModel);
    DataModel pluginFreeView = mock(DataModel.class);
    DataModel scopedPluginView = mock(DataModel.class);
    Scd2HistoryDataModelPlugin plugin = createPlugin(sourceModel, historyModel, baseConfig());
    TailPlugin tail = new TailPlugin(
        query -> Try.failure(new UnsupportedOperationException()),
        data -> Try.failure(new UnsupportedOperationException()),
        (where, data) -> Try.success(1L),
        where -> Try.failure(new UnsupportedOperationException()));

    when(sourceModel.withoutPlugins()).thenReturn(pluginFreeView);
    when(sourceModel.withoutPlugins(Collections.singletonList("Scd2History"))).thenReturn(scopedPluginView);
    when(scopedPluginView.query(any(QueryStatement.class))).thenReturn(Try.failure(new AssertionError("snapshot query should not use a plugin-preserving view")));
    when(scopedPluginView.query(anyList())).thenReturn(Try.failure(new AssertionError("snapshot re-query should not use a plugin-preserving view")));
    when(pluginFreeView.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(Collections.singletonList(record(
        "id", 1L,
        "name", "Alice",
        "updatedAt", "2026-03-18T08:00:00",
        "updatedBy", "tester-1")))));
    when(pluginFreeView.query(anyList())).thenReturn(Try.success(RecordList.of(Collections.singletonList(record(
        "id", 1L,
        "name", "Bob",
        "updatedAt", "2026-03-18T09:00:00",
        "updatedBy", "tester-2")))));

    Try<Long> result = plugin.update(eq("id", 1L), record(
        "name", "Bob",
        "updatedAt", "2026-03-18T09:00:00",
        "updatedBy", "tester-2"), PluginTestContexts.withNext(tail));

    assertTrue(result.isSuccess());
    verify(sourceModel, org.mockito.Mockito.times(2)).withoutPlugins();
    verify(pluginFreeView).query(anyList());
  }

  @Test
  public void updateShouldRejectWhenMatchedRowsExceedConfiguredMaxRows() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    DataModel historyModel = createHistoryModel(false);
    DataModel rawHistoryModel = wireRawHistoryModel(historyModel);
    Scd2HistoryDataModelPlugin plugin = createPlugin(sourceModel, historyModel, configWithMaxRows(1));
    TailPlugin tail = new TailPlugin(
        query -> Try.failure(new UnsupportedOperationException()),
        data -> Try.failure(new UnsupportedOperationException()),
        (where, data) -> Try.failure(new AssertionError("update should not be called when maxRows is exceeded")),
        where -> Try.failure(new UnsupportedOperationException()));

    when(sourceModel.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(Arrays.asList(
        record("id", 1L, "name", "Alice", "updatedAt", "2026-03-18T08:00:00", "updatedBy", "tester-1"),
        record("id", 2L, "name", "Bob", "updatedAt", "2026-03-18T08:05:00", "updatedBy", "tester-1")))));

    Try<Long> result = plugin.update(eq("status", "ACTIVE"), record("name", "Renamed"), PluginTestContexts.withNext(tail));

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof com.ouroboros.data.exception.InvalidStatementException);
    verify(rawHistoryModel, never()).update(anyMap(), anyMap());
    verify(rawHistoryModel, never()).insert(anyMap());
  }

  @Test
  public void updateShouldPersistDiffFieldsWhenStoreDiffIsEnabled() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    DataModel historyModel = createHistoryModel(true);
    DataModel rawHistoryModel = wireRawHistoryModel(historyModel);
    Scd2HistoryDataModelPlugin plugin = createPlugin(sourceModel, historyModel, configWithStoredDiff());
    TailPlugin tail = new TailPlugin(
        query -> Try.failure(new UnsupportedOperationException()),
        data -> Try.failure(new UnsupportedOperationException()),
        (where, data) -> Try.success(1L),
        where -> Try.failure(new UnsupportedOperationException()));

    when(sourceModel.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(Collections.singletonList(record(
        "id", 1L,
        "name", "Alice",
        "updatedAt", "2026-03-18T08:00:00",
        "updatedBy", "tester-1")))));
    when(sourceModel.query(anyList())).thenReturn(Try.success(RecordList.of(Collections.singletonList(record(
        "id", 1L,
        "name", "Bob",
        "updatedAt", "2026-03-18T09:00:00",
        "updatedBy", "tester-2")))));

    Try<Long> result = plugin.update(eq("id", 1L), record(
        "name", "Bob",
        "updatedAt", "2026-03-18T09:00:00",
        "updatedBy", "tester-2"), PluginTestContexts.withNext(tail));

    assertTrue(result.isSuccess());

    ArgumentCaptor<Map<String, Object>> insertCaptor = ArgumentCaptor.forClass(Map.class);
    verify(rawHistoryModel).insert(insertCaptor.capture());
    Map<String, Object> insertedVersion = insertCaptor.getValue();
    assertTrue(String.valueOf(insertedVersion.get("changedFields")).contains("name"));
    assertTrue(String.valueOf(insertedVersion.get("changeSet")).contains("name"));
  }

  @Test
  public void deleteShouldWriteDeleteHistoryWithoutDiffColumnsByDefault() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    DataModel historyModel = createHistoryModel(false);
    DataModel rawHistoryModel = wireRawHistoryModel(historyModel);
    Scd2HistoryDataModelPlugin plugin = createPlugin(sourceModel, historyModel, baseConfig());
    TailPlugin tail = new TailPlugin(
        query -> Try.failure(new UnsupportedOperationException()),
        data -> Try.failure(new UnsupportedOperationException()),
        (where, data) -> Try.failure(new UnsupportedOperationException()),
        where -> Try.success(1L));

    when(sourceModel.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(Collections.singletonList(record(
        "id", 1L,
        "name", "Alice",
        "updatedAt", "2026-03-18T08:00:00",
        "updatedBy", "tester-1")))));

    Try<Long> result = plugin.delete(eq("id", 1L), PluginTestContexts.withNext(tail));

    assertTrue(result.isSuccess());
    ArgumentCaptor<Map<String, Object>> insertCaptor = ArgumentCaptor.forClass(Map.class);
    verify(rawHistoryModel).insert(insertCaptor.capture());
    assertEquals("DELETE", insertCaptor.getValue().get("opType"));
    assertEquals(OPERATOR, insertCaptor.getValue().get("operator"));
    assertTrue(!insertCaptor.getValue().containsKey("changedFields"));
    assertTrue(!insertCaptor.getValue().containsKey("changeSet"));
  }

  @Test
  public void insertShouldReturnSourceRecordInsteadOfHistoryRecord() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    DataModel historyModel = createHistoryModel(false);
    DataModel rawHistoryModel = wireRawHistoryModel(historyModel);
    Scd2HistoryDataModelPlugin plugin = createPlugin(sourceModel, historyModel, baseConfig());
    Record sourceInserted = Record.of(record(
        "id", 1L,
        "name", "Alice",
        "updatedAt", "2026-03-18T08:00:00",
        "updatedBy", "tester-1"));
    when(rawHistoryModel.insert(anyMap())).thenReturn(Try.success(Record.of(record(
        "id", 9L,
        "businessKey", 1L,
        "name", "Alice"))));
    TailPlugin tail = new TailPlugin(
        query -> Try.failure(new UnsupportedOperationException()),
        data -> Try.success(sourceInserted),
        (where, data) -> Try.failure(new UnsupportedOperationException()),
        where -> Try.failure(new UnsupportedOperationException()));

    Try<Record> result = plugin.insert(record(
        "id", 1L,
        "name", "Alice",
        "updatedAt", "2026-03-18T08:00:00",
        "updatedBy", "tester-1"), PluginTestContexts.withNext(tail));

    assertTrue(result.isSuccess());
    assertEquals(Long.valueOf(1L), result.get().get("id"));
    assertEquals("Alice", result.get().get("name"));
  }

  private Scd2HistoryDataModelPlugin createPlugin(DataModel sourceModel, DataModel historyModel, Map<String, Object> configMap) {
    return new Scd2HistoryDataModelPlugin(
        sourceModel,
        Scd2HistoryConfig.from(configMap).get(),
        FIXED_CLOCK,
        modelResolver(historyModel)
    );
  }

  private Function<String, Optional<DataModel>> modelResolver(DataModel historyModel) {
    return name -> Optional.of(historyModel);
  }

  private DataModel createSourceModelWithSinglePrimaryKey() {
    DataModel sourceModel = mock(DataModel.class);
    DataStation station = mock(DataStation.class);
    List<DataModelField> fields = Arrays.asList(
        field("id", new LongValue()),
        field("name", new StringValue()),
        field("updatedAt", new StringValue()),
        field("updatedBy", new StringValue())
    );

    when(sourceModel.getFullName()).thenReturn("demo.User");
    when(sourceModel.getRawName()).thenReturn("demo_user");
    when(sourceModel.getFields()).thenReturn(fields);
    when(sourceModel.getPrimaryKeys()).thenReturn(Collections.singletonList(fields.get(0)));
    when(sourceModel.withoutPlugins()).thenReturn(sourceModel);
    when(sourceModel.getDataStation()).thenReturn(station);

    fields.forEach(field -> when(sourceModel.getField(field.getName())).thenReturn(Optional.of(field)));
    return sourceModel;
  }

  private DataModel createSourceModelWithCompositePrimaryKey() {
    DataModel sourceModel = createSourceModelWithSinglePrimaryKey();
    List<DataModelField> fields = new ArrayList<DataModelField>(sourceModel.getFields());
    fields.add(field("tenantId", new LongValue()));
    when(sourceModel.getPrimaryKeys()).thenReturn(Arrays.asList(fields.get(0), fields.get(4)));
    when(sourceModel.getFields()).thenReturn(fields);
    when(sourceModel.getField("tenantId")).thenReturn(Optional.of(fields.get(4)));
    return sourceModel;
  }

  private DataModel createHistoryModel(boolean withDiffFields) {
    DataModel historyModel = mock(DataModel.class);
    DataStation station = mock(DataStation.class);
    List<DataModelField> fields = new ArrayList<DataModelField>(Arrays.asList(
        field("id", new LongValue()),
        field("name", new StringValue()),
        field("updatedAt", new StringValue()),
        field("updatedBy", new StringValue()),
        field("businessKey", new LongValue()),
        field("validFrom", new DateTimeValue()),
        field("validTo", new DateTimeValue()),
        field("isCurrent", new BooleanValue()),
        field("opType", new StringValue()),
        field("operator", new StringValue())
    ));
    if (withDiffFields) {
      fields.add(field("changedFields", new StringValue()));
      fields.add(field("changeSet", new StringValue()));
    }

    when(historyModel.getFullName()).thenReturn("demo.UserHistory");
    when(historyModel.getRawName()).thenReturn("demo_user_history");
    when(historyModel.getFields()).thenReturn(fields);
    when(historyModel.getDataStation()).thenReturn(station);
    when(historyModel.getExtraProp(Boolean.class, "enableScd2History")).thenReturn(Optional.empty());
    fields.forEach(field -> when(historyModel.getField(field.getName())).thenReturn(Optional.of(field)));

    return historyModel;
  }

  private DataModel wireRawHistoryModel(DataModel historyModel) {
    DataModel rawHistoryModel = mock(DataModel.class);
    List<DataModelField> historyFields = historyModel.getFields();
    String historyFullName = historyModel.getFullName();
    when(rawHistoryModel.getFields()).thenReturn(historyFields);
    when(rawHistoryModel.getFullName()).thenReturn(historyFullName);
    when(rawHistoryModel.getRawName()).thenReturn("demo_user_history");
    historyFields.forEach(field -> when(rawHistoryModel.getField(field.getName())).thenReturn(Optional.of(field)));
    when(historyModel.withoutPlugins()).thenReturn(rawHistoryModel);
    when(rawHistoryModel.update(anyMap(), anyMap())).thenReturn(Try.success(1L));
    when(rawHistoryModel.insert(anyMap())).thenAnswer(invocation -> Try.success(Record.of(invocation.getArgument(0, Map.class))));
    return rawHistoryModel;
  }

  private Map<String, Object> baseConfig() {
    Map<String, Object> config = new LinkedHashMap<String, Object>();
    config.put("historyModelFullName", "demo.UserHistory");
    return config;
  }

  private Map<String, Object> configWithoutRequiredColumn() {
    Map<String, Object> config = baseConfig();
    config.put("validFromField", "missingValidFrom");
    return config;
  }

  private Map<String, Object> configWithIgnoreFields() {
    Map<String, Object> config = baseConfig();
    config.put("ignoreFields", Arrays.asList("updatedAt", "updatedBy"));
    return config;
  }

  private Map<String, Object> configWithStoredDiff() {
    Map<String, Object> config = baseConfig();
    config.put("storeDiff", true);
    return config;
  }

  private Map<String, Object> configWithMaxRows(int maxRows) {
    Map<String, Object> config = baseConfig();
    config.put("maxRows", maxRows);
    return config;
  }

  private DataModelField field(String name, com.ouroboros.data.model.ValueType<?> valueType) {
    DataModelField field = mock(DataModelField.class);
    when(field.getName()).thenReturn(name);
    when(field.getRawName()).thenReturn(toRawName(name));
    when(field.getValueType()).thenReturn(castValueType(valueType));
    return field;
  }

  @SuppressWarnings("unchecked")
  private <T> com.ouroboros.data.model.ValueType<T> castValueType(com.ouroboros.data.model.ValueType<?> valueType) {
    return (com.ouroboros.data.model.ValueType<T>) valueType;
  }

  private static SExpression<Boolean> eq(String field, Object value) {
    return SExpression.create(Operators.EQ, SExpression.field(field), SExpression.constant(value));
  }

  private static Map<String, Object> record(Object... pairs) {
    Map<String, Object> data = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) {
      data.put((String) pairs[i], pairs[i + 1]);
    }
    return data;
  }

  private String toRawName(String fieldName) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < fieldName.length(); i++) {
      char current = fieldName.charAt(i);
      if (Character.isUpperCase(current) && i > 0) {
        builder.append('_');
      }
      builder.append(Character.toLowerCase(current));
    }
    return builder.toString();
  }

  private static class TailPlugin implements DataModelPlugin {
    private final Function<QueryStatement, Try<RecordList>> queryHandler;
    private final Function<Map<String, Object>, Try<Record>> insertHandler;
    private final java.util.function.BiFunction<SExpression<Boolean>, Map<String, Object>, Try<Long>> updateHandler;
    private final Function<SExpression<Boolean>, Try<Long>> deleteHandler;

    private TailPlugin(Function<QueryStatement, Try<RecordList>> queryHandler,
                       Function<Map<String, Object>, Try<Record>> insertHandler,
                       java.util.function.BiFunction<SExpression<Boolean>, Map<String, Object>, Try<Long>> updateHandler,
                       Function<SExpression<Boolean>, Try<Long>> deleteHandler) {
      this.queryHandler = queryHandler;
      this.insertHandler = insertHandler;
      this.updateHandler = updateHandler;
      this.deleteHandler = deleteHandler;
    }

    @Override
    public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
      return insertHandler.apply(data);
    }

    @Override
    public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
      return updateHandler.apply(where, data);
    }

    @Override
    public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
      return deleteHandler.apply(where);
    }

    @Override
    public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
      return queryHandler.apply(statement);
    }
  }
}
