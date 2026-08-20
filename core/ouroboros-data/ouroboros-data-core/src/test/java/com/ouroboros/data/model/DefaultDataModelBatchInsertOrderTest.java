package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.transpile.TranspileContext;

class DefaultDataModelBatchInsertOrderTest {

  @Test
  void batchInsertShouldReturnGeneratedPrimaryKeyRecordsInInputOrder() {
    DefaultDataModel model = modelWithSinglePrimaryKey();
    DataAdapter adapter = model.getDataStation().getDataAdapter();
    when(adapter.batchInsert(any(), any(TranspileContext.class)))
        .thenReturn(Try.success(Arrays.asList("101", "102", "103")));
    when(adapter.query(any(QueryStatement.class), any(TranspileContext.class)))
        .thenReturn(Try.success(RecordList.of(Arrays.asList(
            row("ID", "103", "name", "Carol"),
            row("ID", "101", "name", "Alice"),
            row("ID", "102", "name", "Bob")
        ))));

    Try<RecordList> result = model.batchInsert(Arrays.asList(
        Collections.singletonMap("name", "Alice"),
        Collections.singletonMap("name", "Bob"),
        Collections.singletonMap("name", "Carol")
    ));

    assertTrue(result.isSuccess(), () -> result.isFailure() ? result.getCause().toString() : "");
    assertEquals(Arrays.asList("Alice", "Bob", "Carol"), names(result.get()));
  }

  @Test
  void batchInsertShouldReturnSubmittedPrimaryKeyRecordsInInputOrder() {
    DefaultDataModel model = modelWithSinglePrimaryKey();
    DataAdapter adapter = model.getDataStation().getDataAdapter();
    when(adapter.batchInsert(any(), any(TranspileContext.class)))
        .thenReturn(Try.success(Collections.emptyList()));
    when(adapter.query(any(QueryStatement.class), any(TranspileContext.class)))
        .thenReturn(Try.success(RecordList.of(Arrays.asList(
            row("id", "B", "name", "Bob"),
            row("id", "C", "name", "Carol"),
            row("id", "A", "name", "Alice")
        ))));

    Try<RecordList> result = model.batchInsert(Arrays.asList(
        row("id", "A", "name", "Alice"),
        row("id", "B", "name", "Bob"),
        row("id", "C", "name", "Carol")
    ));

    assertTrue(result.isSuccess(), () -> result.isFailure() ? result.getCause().toString() : "");
    assertEquals(Arrays.asList("Alice", "Bob", "Carol"), names(result.get()));
  }

  private DefaultDataModel modelWithSinglePrimaryKey() {
    DataStation<?> dataStation = mock(DataStation.class);
    DataAdapter adapter = mock(DataAdapter.class);
    when(dataStation.getDataAdapter()).thenReturn(adapter);
    return new DefaultDataModel(singlePrimaryKeyMeta(), dataStation);
  }

  private DataModelMeta singlePrimaryKeyMeta() {
    DataModelFieldMeta id = new DataModelFieldMeta();
    id.setName("id");
    id.setType("String");
    id.setIsAutoIncrement(true);

    DataModelFieldMeta name = new DataModelFieldMeta();
    name.setName("name");
    name.setType("String");

    DataModelMeta meta = new DataModelMeta();
    meta.setName("Employee");
    meta.setRawName("employee");
    meta.setFields(Arrays.asList(id, name));
    meta.setPrimaryKeys(Collections.singletonList("id"));
    return meta;
  }

  private List<Object> names(RecordList records) {
    return records.stream()
        .map(record -> record.get("name"))
        .toList();
  }

  private Map<String, Object> row(String key1, Object value1, String key2, Object value2) {
    Map<String, Object> row = new HashMap<>();
    row.put(key1, value1);
    row.put(key2, value2);
    return row;
  }
}
