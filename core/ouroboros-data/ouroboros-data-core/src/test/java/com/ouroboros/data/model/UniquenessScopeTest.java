package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.util.DataJson;

class UniquenessScopeTest {

  @Test
  void missingExtraPropDefaultsToAllRecords() {
    var parsed = DataJson.tryToBean(DataModelMeta.class, "{\"name\":\"user\"}");

    assertTrue(parsed.isSuccess());
    assertEquals(UniquenessScope.ALL_RECORDS, scopeOf(parsed.get()));
  }

  @Test
  void enumExtraPropRoundTripsThroughJsonCopiesModelsAndProxy() {
    var meta = buildMeta();
    meta.setExtraProp(UniquenessScope.EXTRA_PROP_NAME, UniquenessScope.ACTIVE_RECORDS);

    var json = DataJson.toJsonString(meta);
    var parsed = DataJson.tryToBean(DataModelMeta.class, json);
    var copied = meta.deepCopy();
    var immutable = new ImmutableDataModelMeta(meta);
    var patch = new DataModelMetaPatch(meta);
    patch.setExtraProp(UniquenessScope.EXTRA_PROP_NAME, UniquenessScope.ACTIVE_RECORDS);
    var model = new StubModel(meta);
    var proxy = new EnhancedDataModelProxy(meta, StubModel::new);

    assertTrue(parsed.isSuccess());
    assertEquals(UniquenessScope.ACTIVE_RECORDS, scopeOf(parsed.get()));
    assertEquals(UniquenessScope.ACTIVE_RECORDS, scopeOf(copied));
    assertEquals(UniquenessScope.ACTIVE_RECORDS, scopeOf(immutable));
    assertEquals(UniquenessScope.ACTIVE_RECORDS, scopeOf(patch));
    assertEquals(UniquenessScope.ACTIVE_RECORDS, scopeOf(model));
    assertEquals(UniquenessScope.ACTIVE_RECORDS, scopeOf(proxy));
    assertThrows(UnsupportedOperationException.class,
        () -> immutable.setExtraProp(UniquenessScope.EXTRA_PROP_NAME, UniquenessScope.ALL_RECORDS));
  }

  @Test
  void stringScopeIsCaseInsensitiveAndUnknownValuesFallbackToAllRecords() {
    assertEquals(UniquenessScope.ACTIVE_RECORDS, UniquenessScope.fromExtraProp("active_records"));
    assertEquals(UniquenessScope.ACTIVE_RECORDS, UniquenessScope.fromExtraProp(" AcTiVe_ReCoRdS "));
    assertEquals(UniquenessScope.ALL_RECORDS, UniquenessScope.fromExtraProp("all_records"));
    assertEquals(UniquenessScope.ALL_RECORDS, UniquenessScope.fromExtraProp("missing"));
    assertEquals(UniquenessScope.ALL_RECORDS, UniquenessScope.fromExtraProp(null));
    assertEquals(UniquenessScope.ALL_RECORDS, UniquenessScope.fromExtraProp(1));
    assertEquals(UniquenessScope.ALL_RECORDS, UniquenessScope.fromExtraProp(true));
    assertEquals(UniquenessScope.ALL_RECORDS, UniquenessScope.fromExtraProp(List.of("ACTIVE_RECORDS")));
    assertEquals(UniquenessScope.ALL_RECORDS, UniquenessScope.fromExtraProp(Map.of("type", "ACTIVE_RECORDS")));

    var meta = buildMeta();
    meta.setExtraProp(UniquenessScope.EXTRA_PROP_NAME, "UNKNOWN");

    var model = new StubModel(meta);

    assertEquals(UniquenessScope.ALL_RECORDS, scopeOf(model));
  }

  private static UniquenessScope scopeOf(DataModelMeta meta) {
    return UniquenessScope.fromExtraProp(meta.getExtraProp(UniquenessScope.EXTRA_PROP_NAME).orElse(null));
  }

  private static UniquenessScope scopeOf(DataModel model) {
    return UniquenessScope.fromExtraProp(model.getExtraProp(UniquenessScope.EXTRA_PROP_NAME).orElse(null));
  }

  private static DataModelMeta buildMeta() {
    var id = new DataModelFieldMeta();
    id.setName("id");
    id.setType("Long");

    var meta = new DataModelMeta();
    meta.setNamespace("demo");
    meta.setName("user");
    meta.setFields(List.of(id));
    meta.setPrimaryKeys(Collections.singletonList("id"));
    return meta;
  }

  private static final class StubModel extends AbstractDataModel {
    private StubModel(DataModelMeta meta) {
      super(meta);
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
      return Try.success(Record.of(data));
    }

    @Override
    public Try<RecordList> batchInsert(List<Map<String, Object>> dataList) {
      return Try.success(RecordList.of(dataList));
    }

    @Override
    public Try<Long> update(Map<String, Object> where, Map<String, Object> data) {
      return Try.success(1L);
    }

    @Override
    public Try<Long> delete(Map<String, Object> where) {
      return Try.success(1L);
    }

    @Override
    public Try<Long> count(Map<String, Object> where) {
      return Try.success(0L);
    }

    @Override
    public Try<RecordList> query(Map<String, Object> statement) {
      return Try.success(RecordList.of(Collections.emptyList()));
    }

    @Override
    public Try<RecordList> query(QueryStatement statement) {
      return Try.success(RecordList.of(Collections.emptyList()));
    }
  }
}
