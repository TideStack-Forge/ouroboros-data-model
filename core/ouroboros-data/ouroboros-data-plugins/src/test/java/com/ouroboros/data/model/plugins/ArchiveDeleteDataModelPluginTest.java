package com.ouroboros.data.model.plugins;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelCenter;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.EnhancedDataModelProxy;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.model.deletepolicy.ArchiveRecordAssembler;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.util.DataJson;

public class ArchiveDeleteDataModelPluginTest {

  @Test
  public void testAssembleArchiveRecordShouldIncludeSourceFieldsAndMetadata() {
    DataModel sourceModel = mockModel("demo.User", "default", Arrays.asList("id", "name"), Collections.singletonList("id"));
    Date deletedAt = new Date();
    Record sourceRecord = Record.of(linkedMap("id", "A1", "name", "Alice"));

    Map<String, Object> assembled = ArchiveRecordAssembler.assemble(sourceModel, sourceRecord, deletedAt, null, "op-1");

    assertEquals("A1", assembled.get("id"));
    assertEquals("Alice", assembled.get("name"));
    assertEquals("demo.User", assembled.get("sourceModel"));
    assertEquals("default", assembled.get("sourceDataStation"));
    assertEquals(DataJson.toJsonString(Collections.singletonMap("id", "A1")), assembled.get("sourcePrimaryKey"));
    assertSame(deletedAt, assembled.get("deletedAt"));
    assertNull(assembled.get("deletedBy"));
    assertEquals("op-1", assembled.get("deleteOperationId"));
  }

  @Test
  public void testAssembleArchiveRecordShouldReadSourceFieldsIgnoringCase() {
    DataModel sourceModel = mockModel("demo.User", "default", Arrays.asList("id", "name"), Collections.singletonList("id"));
    Date deletedAt = new Date();
    Record sourceRecord = Record.of(linkedMap("ID", "A1", "NAME", "Alice"));

    Map<String, Object> assembled = ArchiveRecordAssembler.assemble(sourceModel, sourceRecord, deletedAt, null, "op-2");

    assertEquals("A1", assembled.get("id"));
    assertEquals("Alice", assembled.get("name"));
    assertEquals(DataJson.toJsonString(Collections.singletonMap("id", "A1")), assembled.get("sourcePrimaryKey"));
  }

  @Test
  public void testAssembleArchiveRecordShouldApplySourceFieldMappings() {
    DataModel sourceModel = mockModel("demo.User", "default", Arrays.asList("id", "name"), Collections.singletonList("id"));
    Date deletedAt = new Date();
    Record sourceRecord = Record.of(linkedMap("id", "A1", "name", "Alice"));

    Map<String, Object> assembled = ArchiveRecordAssembler.assemble(
        sourceModel,
        sourceRecord,
        deletedAt,
        null,
        "op-3",
        Collections.singletonMap("id", "sourceId")
    );

    assertFalse(assembled.containsKey("id"));
    assertEquals("A1", assembled.get("sourceId"));
    assertEquals("Alice", assembled.get("name"));
    assertEquals(DataJson.toJsonString(Collections.singletonMap("id", "A1")), assembled.get("sourcePrimaryKey"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteShouldQueryCopyAndDeleteByBatches() {
    DataModel sourceModel = mock(DataModel.class);
    DataModel sourceView = mock(DataModel.class);
    DataModel archiveModel = mock(DataModel.class);
    DataModelPlugin nextPlugin = mock(DataModelPlugin.class);
    DataStation dataStation = mockDataStation("sql-source");
    DataStation archiveDataStation = mockDataStation("sql-archive");
    List<DataModelField> sourceFields = mockFields(Arrays.asList("id", "name"));
    List<DataModelField> sourcePrimaryKeys = mockFields(Collections.singletonList("id"));
    List<DataModelField> archiveFields = mockFields(Arrays.asList(
        "id",
        "name",
        "sourceModel",
        "sourceDataStation",
        "sourcePrimaryKey",
        "deletedAt",
        "deletedBy",
        "deleteOperationId"
    ));
    List<Record> records = Arrays.<Record>asList(
        Record.of(linkedMap("id", "A1", "name", "Alice")),
        Record.of(linkedMap("id", "A2", "name", "Bob"))
    );
    when(sourceModel.withoutPlugins()).thenReturn(sourceView);
    when(sourceModel.getFullName()).thenReturn("demo.User");
    when(sourceModel.getRawName()).thenReturn("demo_user");
    when(sourceModel.getDataStation()).thenReturn(dataStation);
    when(sourceModel.getFields()).thenReturn(sourceFields);
    when(sourceModel.getPrimaryKeys()).thenReturn(sourcePrimaryKeys);
    when(sourceView.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(records)));
    when(archiveModel.getDataStation()).thenReturn(archiveDataStation);
    when(archiveModel.getFields()).thenReturn(archiveFields);
    when(archiveModel.batchInsert(anyList())).thenReturn(Try.success(RecordList.empty()));
    when(nextPlugin.delete(any(SExpression.class), any(DataModelPluginContext.class))).thenReturn(Try.success(1L));

    ArchiveDeleteDataModelPlugin plugin = new ArchiveDeleteDataModelPlugin(sourceModel, archiveModel, 1);
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    SExpression<Boolean> where = eq("status", "archived");
    Try<Long> result = plugin.delete(where, context);

    assertTrue(result.isSuccess());
    assertEquals(2L, result.get().longValue());

    ArgumentCaptor<QueryStatement> queryCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    ArgumentCaptor<List<Map<String, Object>>> insertCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<SExpression> deleteCaptor = ArgumentCaptor.forClass(SExpression.class);
    InOrder inOrder = inOrder(sourceModel, sourceView, archiveModel, nextPlugin);
    inOrder.verify(sourceModel).withoutPlugins();
    inOrder.verify(sourceView).query(queryCaptor.capture());
    inOrder.verify(archiveModel).batchInsert(insertCaptor.capture());
    inOrder.verify(nextPlugin).delete(deleteCaptor.capture(), any(DataModelPluginContext.class));
    inOrder.verify(archiveModel).batchInsert(insertCaptor.capture());
    inOrder.verify(nextPlugin).delete(deleteCaptor.capture(), any(DataModelPluginContext.class));

    assertEquals(where, queryCaptor.getValue().getWhere());
    assertTrue(containsConstant(deleteCaptor.getAllValues().get(0), "A1"));
    assertTrue(containsConstant(deleteCaptor.getAllValues().get(1), "A2"));
    assertEquals("A1", insertCaptor.getAllValues().get(0).get(0).get("id"));
    assertEquals("A2", insertCaptor.getAllValues().get(1).get(0).get("id"));
    assertEquals("demo.User", insertCaptor.getAllValues().get(0).get(0).get("sourceModel"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteShouldBuildBatchDeleteWhereIgnoringQueryResultCase() {
    DataModel sourceModel = mock(DataModel.class);
    DataModel sourceView = mock(DataModel.class);
    DataModel archiveModel = mock(DataModel.class);
    DataModelPlugin nextPlugin = mock(DataModelPlugin.class);
    DataStation dataStation = mockDataStation("sql-source");
    DataStation archiveDataStation = mockDataStation("sql-archive");
    List<DataModelField> sourceFields = mockFields(Arrays.asList("id", "name"));
    List<DataModelField> sourcePrimaryKeys = mockFields(Collections.singletonList("id"));
    List<DataModelField> archiveFields = mockFields(Arrays.asList(
        "id",
        "name",
        "sourceModel",
        "sourceDataStation",
        "sourcePrimaryKey",
        "deletedAt",
        "deletedBy",
        "deleteOperationId"
    ));
    List<Record> records = Arrays.<Record>asList(
        Record.of(linkedMap("ID", "A1", "NAME", "Alice")),
        Record.of(linkedMap("ID", "A2", "NAME", "Bob"))
    );
    when(sourceModel.withoutPlugins()).thenReturn(sourceView);
    when(sourceModel.getFullName()).thenReturn("demo.User");
    when(sourceModel.getRawName()).thenReturn("demo_user");
    when(sourceModel.getDataStation()).thenReturn(dataStation);
    when(sourceModel.getFields()).thenReturn(sourceFields);
    when(sourceModel.getPrimaryKeys()).thenReturn(sourcePrimaryKeys);
    when(sourceView.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(records)));
    when(archiveModel.getDataStation()).thenReturn(archiveDataStation);
    when(archiveModel.getFields()).thenReturn(archiveFields);
    when(archiveModel.batchInsert(anyList())).thenReturn(Try.success(RecordList.empty()));
    when(nextPlugin.delete(any(SExpression.class), any(DataModelPluginContext.class))).thenReturn(Try.success(2L));

    ArchiveDeleteDataModelPlugin plugin = new ArchiveDeleteDataModelPlugin(sourceModel, archiveModel, 2);
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<Long> result = plugin.delete(eq("status", "archived"), context);

    assertTrue(result.isSuccess());
    ArgumentCaptor<SExpression> deleteCaptor = ArgumentCaptor.forClass(SExpression.class);
    ArgumentCaptor<List<Map<String, Object>>> insertCaptor = ArgumentCaptor.forClass(List.class);
    org.mockito.Mockito.verify(archiveModel).batchInsert(insertCaptor.capture());
    org.mockito.Mockito.verify(nextPlugin).delete(deleteCaptor.capture(), any(DataModelPluginContext.class));
    assertTrue(containsConstant(deleteCaptor.getValue(), "A1"));
    assertTrue(containsConstant(deleteCaptor.getValue(), "A2"));
    assertEquals("A1", insertCaptor.getValue().get(0).get("id"));
    assertEquals("A2", insertCaptor.getValue().get(1).get("id"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteShouldInferArchiveFieldMappingFromArchiveModel() {
    DataModel sourceModel = mock(DataModel.class);
    DataModel sourceView = mock(DataModel.class);
    DataModel archiveModel = mock(DataModel.class);
    DataModelPlugin nextPlugin = mock(DataModelPlugin.class);
    DataStation dataStation = mockDataStation("sql-source");
    DataStation archiveDataStation = mockDataStation("sql-archive");
    List<DataModelField> sourceFields = mockFields(Arrays.asList("id", "name"));
    List<DataModelField> sourcePrimaryKeys = mockFields(Collections.singletonList("id"));
    List<DataModelField> archiveFields = mockFields(Arrays.asList(
        "id",
        "sourceId",
        "name",
        "sourceModel",
        "sourceDataStation",
        "sourcePrimaryKey",
        "deletedAt",
        "deletedBy",
        "deleteOperationId"
    ));
    List<Record> records = Collections.singletonList(Record.of(linkedMap("id", "A1", "name", "Alice")));
    when(sourceModel.withoutPlugins()).thenReturn(sourceView);
    when(sourceModel.getFullName()).thenReturn("demo.User");
    when(sourceModel.getRawName()).thenReturn("demo_user");
    when(sourceModel.getDataStation()).thenReturn(dataStation);
    when(sourceModel.getFields()).thenReturn(sourceFields);
    when(sourceModel.getPrimaryKeys()).thenReturn(sourcePrimaryKeys);
    when(sourceView.query(any(QueryStatement.class))).thenReturn(Try.success(RecordList.of(records)));
    when(archiveModel.getDataStation()).thenReturn(archiveDataStation);
    when(archiveModel.getFields()).thenReturn(archiveFields);
    when(archiveModel.batchInsert(anyList())).thenReturn(Try.success(RecordList.empty()));
    when(nextPlugin.delete(any(SExpression.class), any(DataModelPluginContext.class))).thenReturn(Try.success(1L));

    ArchiveDeleteDataModelPlugin plugin = new ArchiveDeleteDataModelPlugin(sourceModel, archiveModel, 1);
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<Long> result = plugin.delete(eq("status", "archived"), context);

    assertTrue(result.isSuccess());
    ArgumentCaptor<List<Map<String, Object>>> insertCaptor = ArgumentCaptor.forClass(List.class);
    org.mockito.Mockito.verify(archiveModel).batchInsert(insertCaptor.capture());
    Map<String, Object> insertedArchiveRow = insertCaptor.getValue().get(0);
    assertEquals("A1", insertedArchiveRow.get("sourceId"));
    assertFalse(insertedArchiveRow.containsKey("id"));
    assertEquals("Alice", insertedArchiveRow.get("name"));
  }

  private static SExpression<Boolean> eq(String field, Object value) {
    return SExpression.create(Operators.EQ, SExpression.field(field), SExpression.constant(value));
  }

  private static boolean containsConstant(SExpression<?> expression, Object expected) {
    final boolean[] found = new boolean[] {false};
    expression.walk((node, context) -> {
      if (node.getOperator() == Operators.CONSTANT
          && !node.getParams().isEmpty()
          && matchesConstant(node.getParam(0), expected)) {
        found[0] = true;
      }
    });
    return found[0];
  }

  private static boolean matchesConstant(Object actual, Object expected) {
    if (java.util.Objects.equals(actual, expected)) {
      return true;
    }
    return actual instanceof Iterable<?> values
        && java.util.stream.StreamSupport.stream(values.spliterator(), false)
            .anyMatch(value -> java.util.Objects.equals(value, expected));
  }

  @Test
  public void testPluginDescriptorShouldDeferArchiveModelResolutionUntilDelete() throws Exception {
    Field field = DataModelCenter.class.getDeclaredField("dataModelMap");
    field.setAccessible(true);
    Object original = field.get(null);
    field.set(null, null);

    try {
      DataModel coreDataModel = mock(DataModel.class);
      when(coreDataModel.getFullName()).thenReturn("demo.User");

      assertDoesNotThrow(() -> new EnhancedDataModelProxy(
          coreDataModel,
          Collections.singletonList(new PluginDescriptor("ArchiveDelete", Collections.singletonMap("archiveModel", "archive.UserArchive")))
      ));
    } finally {
      field.set(null, original);
    }
  }

  @Test
  public void testBuilderShouldRejectLegacyPluginName() {
    assertTrue(new ArchiveDeleteDataModelPlugin.Builder().support("ArchiveDelete"));
    assertFalse(new ArchiveDeleteDataModelPlugin.Builder().support("RecycleBinDelete"));
  }

  @Test
  public void testBuilderShouldRequireArchiveModelConfig() {
    ArchiveDeleteDataModelPlugin.Builder builder = new ArchiveDeleteDataModelPlugin.Builder();
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> builder.build(mock(DataModel.class), Collections.emptyMap())
    );
    assertTrue(exception.getMessage().contains("archiveModel"));
  }

  @Test
  public void testBuilderShouldIgnoreUnknownConfigKeys() {
    ArchiveDeleteDataModelPlugin.Builder builder = new ArchiveDeleteDataModelPlugin.Builder();
    Map<String, Object> config = new LinkedHashMap<String, Object>();
    config.put("archiveModel", "archive.UserArchive");
    config.put("unknown", "ignored");

    Optional<DataModelPlugin> plugin = builder.build(mock(DataModel.class), config);

    assertTrue(plugin.isPresent());
  }

  private DataModel mockModel(String fullName, String dataStationName, List<String> fieldNames, List<String> primaryKeyNames) {
    DataModel model = mock(DataModel.class);
    DataStation dataStation = mockDataStation(dataStationName);
    List<DataModelField> fields = mockFields(fieldNames);
    List<DataModelField> primaryKeys = mockFields(primaryKeyNames);
    when(model.getFullName()).thenReturn(fullName);
    when(model.getDataStation()).thenReturn(dataStation);
    when(model.getFields()).thenReturn(fields);
    when(model.getPrimaryKeys()).thenReturn(primaryKeys);
    return model;
  }

  @SuppressWarnings("rawtypes")
  private DataStation mockDataStation(String dataStationName) {
    DataStation dataStation = mock(DataStation.class);
    when(dataStation.getName()).thenReturn(dataStationName);
    return dataStation;
  }

  private List<DataModelField> mockFields(List<String> fieldNames) {
    List<DataModelField> fields = new ArrayList<DataModelField>();
    for (String fieldName : fieldNames) {
      DataModelField field = mock(DataModelField.class);
      when(field.getName()).thenReturn(fieldName);
      fields.add(field);
    }
    return fields;
  }

  private Map<String, Object> linkedMap(String key1, Object value1, String key2, Object value2) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put(key1, value1);
    map.put(key2, value2);
    return map;
  }
}
