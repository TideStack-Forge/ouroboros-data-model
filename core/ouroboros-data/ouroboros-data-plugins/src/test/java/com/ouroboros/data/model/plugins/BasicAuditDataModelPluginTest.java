package com.ouroboros.data.model.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;

class BasicAuditDataModelPluginTest {

  @AfterEach
  void cleanup() {
    setInitUpdatedAt(false);
  }

  @Test
  void insert_setsCreateAuditFieldsFromDataOperationIdentityProvider() {
    setInitUpdatedAt(false);

    BasicAuditDataModelPlugin plugin = new BasicAuditDataModelPlugin(new BasicAuditDataModelPlugin.Config());
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<Record> result = plugin.insert(linkedMap("id", "order-1"), context);

    assertTrue(result.isSuccess());
    assertNotNull(nextPlugin.forwardedInsertData);
    assertEquals(TestDataOperationIdentityProvider.OPERATOR, nextPlugin.forwardedInsertData.get("createdBy"));
    assertNotNull(nextPlugin.forwardedInsertData.get("createdAt"));
    assertTrue(nextPlugin.forwardedInsertData.get("createdAt") instanceof LocalDateTime);
    assertTrue(!nextPlugin.forwardedInsertData.containsKey("updatedBy"));
    assertTrue(!nextPlugin.forwardedInsertData.containsKey("updatedAt"));
  }

  @Test
  void batchInsert_setsCreateAuditFieldsFromDataOperationIdentityProvider() {
    setInitUpdatedAt(false);

    BasicAuditDataModelPlugin plugin = new BasicAuditDataModelPlugin(new BasicAuditDataModelPlugin.Config());
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    List<Map<String, Object>> input = new ArrayList<Map<String, Object>>();
    input.add(linkedMap("id", "order-1"));
    input.add(linkedMap("id", "order-2"));

    Try<RecordList> result = plugin.batchInsert(input, context);

    assertTrue(result.isSuccess());
    assertEquals(2, nextPlugin.forwardedBatchInsertDataList.size());
    for (Map<String, Object> row : nextPlugin.forwardedBatchInsertDataList) {
      assertEquals(TestDataOperationIdentityProvider.OPERATOR, row.get("createdBy"));
      assertNotNull(row.get("createdAt"));
      assertTrue(row.get("createdAt") instanceof LocalDateTime);
      assertTrue(!row.containsKey("updatedBy"));
      assertTrue(!row.containsKey("updatedAt"));
    }
  }

  @Test
  void update_setsUpdateAuditFieldsAndStripsCreateAuditFields() {
    setInitUpdatedAt(false);

    BasicAuditDataModelPlugin plugin = new BasicAuditDataModelPlugin(new BasicAuditDataModelPlugin.Config());
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    SExpression<Boolean> where = SExpression.create(
        Operators.EQ,
        SExpression.field("id"),
        SExpression.constant("order-1"));
    Map<String, Object> data = linkedMap(
        "status", "PAID",
        "createdBy", "caller",
        "createdAt", LocalDateTime.parse("2026-07-09T10:00:00")
    );

    Try<Long> result = plugin.update(where, data, context);

    assertTrue(result.isSuccess());
    assertEquals(where, nextPlugin.forwardedUpdateWhere);
    assertNotNull(nextPlugin.forwardedUpdateData);
    assertEquals(TestDataOperationIdentityProvider.OPERATOR, nextPlugin.forwardedUpdateData.get("updatedBy"));
    assertNotNull(nextPlugin.forwardedUpdateData.get("updatedAt"));
    assertTrue(nextPlugin.forwardedUpdateData.get("updatedAt") instanceof LocalDateTime);
    assertTrue(!nextPlugin.forwardedUpdateData.containsKey("createdBy"));
    assertTrue(!nextPlugin.forwardedUpdateData.containsKey("createdAt"));
  }

  @Test
  void insert_withInitUpdatedAtTrue_setsUpdateAuditFieldsToo() {
    setInitUpdatedAt(true);

    BasicAuditDataModelPlugin plugin = new BasicAuditDataModelPlugin(new BasicAuditDataModelPlugin.Config());
    RecordingTailPlugin nextPlugin = new RecordingTailPlugin();
    DataModelPluginContext context = PluginTestContexts.withNext(nextPlugin);

    Try<Record> result = plugin.insert(linkedMap("id", "order-1"), context);

    assertTrue(result.isSuccess());
    assertNotNull(nextPlugin.forwardedInsertData);
    assertEquals(TestDataOperationIdentityProvider.OPERATOR, nextPlugin.forwardedInsertData.get("createdBy"));
    assertEquals(TestDataOperationIdentityProvider.OPERATOR, nextPlugin.forwardedInsertData.get("updatedBy"));
    assertNotNull(nextPlugin.forwardedInsertData.get("createdAt"));
    assertNotNull(nextPlugin.forwardedInsertData.get("updatedAt"));
    assertNotEquals(nextPlugin.forwardedInsertData.get("createdAt"), "legacy-principal");
  }

  private static void setInitUpdatedAt(boolean value) {
    try {
      Field field = BasicAuditDataModelPlugin.class.getDeclaredField("initUpdatedAt");
      field.setAccessible(true);
      field.setBoolean(null, value);
    }
    catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  private static Map<String, Object> linkedMap(Object... pairs) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put((String) pairs[i], pairs[i + 1]);
    }
    return map;
  }

  private static final class RecordingTailPlugin implements DataModelPlugin {
    private Map<String, Object> forwardedInsertData;
    private List<Map<String, Object>> forwardedBatchInsertDataList = new ArrayList<Map<String, Object>>();
    private SExpression<Boolean> forwardedUpdateWhere;
    private Map<String, Object> forwardedUpdateData;

    @Override
    public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
      forwardedInsertData = new LinkedHashMap<String, Object>(data);
      return Try.success(null);
    }

    @Override
    public Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
      forwardedBatchInsertDataList = dataList.stream().map(LinkedHashMap<String, Object>::new)
          .collect(Collectors.toList());
      return Try.success(RecordList.of(new ArrayList<Record>()));
    }

    @Override
    public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
      forwardedUpdateWhere = where;
      forwardedUpdateData = new LinkedHashMap<String, Object>(data);
      return Try.success(1L);
    }
  }
}
