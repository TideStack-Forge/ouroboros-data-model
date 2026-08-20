package com.ouroboros.data.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.ouroboros.data.dsl.statement.QueryStatement.JoinEntry;
import com.ouroboros.data.record.RecordList;

public class QueryPatcher {
  private List<JoinEntry> joinEntries;
  private Map<String, String> fieldMapping;
  private Consumer<RecordList> resultWatcher;
  private Consumer<Map<String, Object>> rowPatcher;

  public QueryPatcher(List<JoinEntry> joinEntries, Map<String, String> fieldMapping, Consumer<Map<String, Object>> rowPatcher, Consumer<RecordList> resultWatcher) {
    this.joinEntries = joinEntries;
    this.fieldMapping = fieldMapping;
    this.rowPatcher = rowPatcher;
    this.resultWatcher = resultWatcher;
  }

  public QueryPatcher(JoinEntry joinEntry, Map<String, String> fieldMapping) {
    this(Collections.singletonList(joinEntry), fieldMapping, m -> {
    }, r -> {
    });
  }

  public QueryPatcher(JoinEntry joinEntry, Map<String, String> fieldMapping, Consumer<Map<String, Object>> rowPatcher) {
    this(Collections.singletonList(joinEntry), fieldMapping, rowPatcher, r -> {
    });
  }

  public QueryPatcher(JoinEntry joinEntry, Map<String, String> fieldMapping, Consumer<Map<String, Object>> rowPatcher, Consumer<RecordList> resultWatcher) {
    this(Collections.singletonList(joinEntry), fieldMapping, rowPatcher, resultWatcher);
  }

  public QueryPatcher() {
    this(Collections.emptyList(), Collections.emptyMap(), m -> {
    }, r -> {
    });
  }

  public List<JoinEntry> getJoinEntries() {
    return joinEntries;
  }

  public Map<String, String> getFieldMapping() {
    return fieldMapping;
  }

  public Consumer<RecordList> getResultWatcher() {
    return resultWatcher;
  }

  public Consumer<Map<String, Object>> getRowPatcher() {
    return rowPatcher;
  }
}
