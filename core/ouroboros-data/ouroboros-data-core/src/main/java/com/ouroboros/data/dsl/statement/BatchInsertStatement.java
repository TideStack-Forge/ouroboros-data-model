package com.ouroboros.data.dsl.statement;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ouroboros.data.dsl.Keyword;

public class BatchInsertStatement extends DMLStatement {

  public BatchInsertStatement(Map<String, Object> statement) {
    super(statement);
  }

  public static BatchInsertStatement of(String entityName, List<Map<String, ?>> dataList) {
    var map = new HashMap<String, Object>();
    map.put(Keyword.INSERT.toString(), entityName);
    map.put(Keyword.VALUES.toString(), dataList);
    return new BatchInsertStatement(map);
  }

  public String getEntityName() {
    var name = get(Keyword.INSERT.toString());
    return String.valueOf(name);
  }

  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> getValuesList() {
    var values = get(Keyword.VALUES.toString());
    if (values == null) {
      return Collections.emptyList();
    }
    return (List<Map<String, Object>>) values;
  }
}
