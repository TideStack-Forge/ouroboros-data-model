package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;

class AbstractDataModelCoverageTest {

  @Test
  void abstractDataModelCoversIdOperationsAndInsertOrUpdateBranches() {
    var model = new StubModel(buildMeta());

    assertTrue(model.update(1L, mapOf("name", "a")).isSuccess());
    assertTrue(model.update(Arrays.<Object>asList(1L, 2L), mapOf("name", "a")).isSuccess());
    assertTrue(model.delete(1L).isSuccess());
    assertTrue(model.delete(Arrays.<Object>asList(1L, 2L)).isSuccess());

    var q1 = model.query(Arrays.asList("id"), mapOf("name", "a"));
    assertTrue(q1.isSuccess());
    var q2 = model.query(Arrays.asList("id"), mapOf("name", "a"), mapOf("id", "desc"));
    assertTrue(q2.isSuccess());
    var q3 = model.query(Arrays.asList("id"), mapOf("name", "a"), mapOf("id", "desc"), 0, 10);
    assertTrue(q3.isSuccess());

    var insertNoPk = model.insertOrUpdate(mapOf("name", "alice"));
    assertTrue(insertNoPk.isSuccess());

    var insertWithPkNotExists = model.insertOrUpdate(mapOf("id", 2L, "name", "bob"));
    assertTrue(insertWithPkNotExists.isSuccess());

    model.storage.put(3L, mapOf("id", 3L, "name", "carol"));
    var unchanged = model.insertOrUpdate(mapOf("id", 3L, "name", "carol"));
    assertTrue(unchanged.isSuccess());
    assertEquals("carol", unchanged.get().get("name"));

    model.storage.put(4L, mapOf("id", 4L, "name", "before"));
    var updated = model.insertOrUpdate(mapOf("id", 4L, "name", "after"));
    assertTrue(updated.isSuccess());
    assertEquals("after", updated.get().get("name"));
  }

  @Test
  void abstractDataModelCoversBatchInsertOrUpdateAndRecordBuilders() {
    var model = new StubModel(buildMeta());
    model.storage.put(10L, mapOf("id", 10L, "name", "old"));

    var merged = model.batchInsertOrUpdate(Arrays.asList(
        mapOf("id", 10L, "name", "new"),
        mapOf("id", 11L, "name", "inserted"),
        mapOf("name", "no-id")
    ));
    assertTrue(merged.isSuccess());
    assertFalse(merged.get().isEmpty());

    var insertRecord = model.exposeBuildInsertRecord(mapOf("id", 20L, "name", "x"));
    assertTrue(insertRecord.isSuccess());
    assertEquals(20L, insertRecord.get().get("id"));

    var updateRecord = model.exposeBuildUpdateRecord(mapOf("name", "y", "virtualOnly", "z"));
    assertTrue(updateRecord.isSuccess());
    assertEquals("y", updateRecord.get().get("name"));
  }

  @Test
  void snowflakePrimaryKeyUsesStringRuntimeValueAndLongPersistentValue() {
    var model = new StubModel(buildSnowflakeMeta(), record -> "9007199254740993");

    var submittedRecord = model.exposeBuildInsertRecord(mapOf("id", "9007199254740993", "name", "x"));
    assertTrue(submittedRecord.isSuccess());
    assertEquals(9007199254740993L, submittedRecord.get().get("id"));

    var generatedRecord = model.exposeBuildInsertRecord(mapOf("name", "generated"));
    assertTrue(generatedRecord.isSuccess());
    assertEquals(9007199254740993L, generatedRecord.get().get("id"));

    var idPredicate = model.exposeBuildIdPredicate("9007199254740993");
    assertTrue(idPredicate.isSuccess());
    assertEquals(9007199254740993L, idPredicate.get().get("id"));

    var snowflakeValueType = model.getPrimaryKeys().get(0).getValueType();
    assertEquals("Snowflake", snowflakeValueType.getName());
    assertEquals("9007199254740993", snowflakeValueType.convert(9007199254740993L));
  }

  private static DataModelMeta buildMeta() {
    var id = new DataModelFieldMeta();
    id.setName("id");
    id.setType("Long");
    id.setIsNullable(false);

    var name = new DataModelFieldMeta();
    name.setName("name");
    name.setType("String");
    name.setRules(Collections.singletonList("notNull"));

    var virtual = new DataModelFieldMeta();
    virtual.setName("virtualOnly");
    virtual.setType("Model");

    var meta = new DataModelMeta();
    meta.setNamespace("demo");
    meta.setName("user");
    meta.setFields(Arrays.asList(id, name, virtual));
    meta.setPrimaryKeys(Collections.singletonList("id"));
    return meta;
  }

  private static DataModelMeta buildSnowflakeMeta() {
    var meta = buildMeta();
    meta.getFields().get(0).setType("Snowflake");
    meta.setPrimaryKeyGenerator("snowflake");
    return meta;
  }

  private static Map<String, Object> mapOf(Object... kv) {
    var m = new LinkedHashMap<String, Object>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put((String) kv[i], kv[i + 1]);
    }
    return m;
  }

  private static final class StubModel extends AbstractDataModel {
    Map<Object, Map<String, Object>> storage = new HashMap<Object, Map<String, Object>>();
    private final PrimaryKeyGenerator<?> primaryKeyGenerator;

    private StubModel(DataModelMeta meta) {
      this(meta, null);
    }

    private StubModel(DataModelMeta meta, PrimaryKeyGenerator<?> primaryKeyGenerator) {
      super(meta);
      this.primaryKeyGenerator = primaryKeyGenerator;
    }

    @Override
    public PrimaryKeyGenerator<?> getPrimaryKeyGenerator() {
      return primaryKeyGenerator == null ? super.getPrimaryKeyGenerator() : primaryKeyGenerator;
    }

    @Override
    public DataAdapter getAdapter() {
      return null;
    }

    @Override
    public com.ouroboros.data.station.DataStation<?> getDataStation() {
      return null;
    }

    @Override
    public Try<Record> insert(Map<String, Object> data) {
      var copied = new LinkedHashMap<String, Object>(data);
      var id = copied.get("id");
      if (id != null) {
        storage.put(id, copied);
      }
      return Try.success(Record.of(copied));
    }

    @Override
    public Try<RecordList> batchInsert(List<Map<String, Object>> dataList) {
      var copied = new ArrayList<Map<String, Object>>();
      for (Map<String, Object> data : dataList) {
        var item = new LinkedHashMap<String, Object>(data);
        copied.add(item);
        var id = item.get("id");
        if (id != null) {
          storage.put(id, item);
        }
      }
      return Try.success(RecordList.of(copied));
    }

    @Override
    public Try<Long> update(Map<String, Object> where, Map<String, Object> data) {
      var id = where.get("id");
      if (id != null && storage.containsKey(id)) {
        storage.put(id, new LinkedHashMap<String, Object>(data));
      }
      if (id instanceof List<?>) {
        for (Object one : (List<?>) id) {
          if (storage.containsKey(one)) {
            storage.put(one, new LinkedHashMap<String, Object>(data));
          }
        }
      }
      return Try.success(1L);
    }

    @Override
    public Try<Long> delete(Map<String, Object> where) {
      var id = where.get("id");
      if (id instanceof List<?>) {
        for (Object one : (List<?>) id) {
          storage.remove(one);
        }
      } else {
        storage.remove(id);
      }
      return Try.success(1L);
    }

    @Override
    public Try<Long> count(Map<String, Object> where) {
      return Try.success((long) storage.size());
    }

    @Override
    public Try<RecordList> query(Map<String, Object> statement) {
      var whereRaw = statement.get("where");
      if (!(whereRaw instanceof Map<?, ?>)) {
        return Try.success(RecordList.of(new ArrayList<Map<String, Object>>()));
      }
      var id = ((Map<?, ?>) whereRaw).get("id");
      if (id instanceof List<?>) {
        var out = new ArrayList<Map<String, Object>>();
        for (Object one : (List<?>) id) {
          if (storage.containsKey(one)) {
            out.add(storage.get(one));
          }
        }
        return Try.success(RecordList.of(out));
      }
      if (storage.containsKey(id)) {
        return Try.success(RecordList.of(Collections.singletonList(storage.get(id))));
      }
      return Try.success(RecordList.of(new ArrayList<Map<String, Object>>()));
    }

    @Override
    public Try<RecordList> query(QueryStatement statement) {
      return query(new HashMap<String, Object>());
    }

    private Try<Record> exposeBuildInsertRecord(Map<String, Object> data) {
      return buildInsertRecord(data);
    }

    private Try<Map<String, Object>> exposeBuildIdPredicate(Object id) {
      return buildIdPredicate(id);
    }

    private Try<Record> exposeBuildUpdateRecord(Map<String, Object> data) {
      return buildUpdateRecord(data);
    }
  }
}
