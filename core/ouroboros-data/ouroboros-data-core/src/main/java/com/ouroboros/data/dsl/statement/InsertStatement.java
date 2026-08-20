package com.ouroboros.data.dsl.statement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ouroboros.data.dsl.Keyword;

public class InsertStatement extends DMLStatement {

  protected InsertStatement(Map<String, Object> statement) {
    super(statement);
  }

  public static InsertStatement of(String entityName, Map<String, ?> values) {
    var map = new HashMap<String, Object>();
    map.put(Keyword.INSERT.toString(), entityName);
    map.put(Keyword.VALUES.toString(), values);
    return new InsertStatement(map);
  }

  public String getEntityName() {
    var name = get(Keyword.INSERT.toString());
    return String.valueOf(name);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getValues() {
    var values = get(Keyword.VALUES.toString());
    if (values == null) {
      return Collections.emptyMap();
    }
    return (Map<String, Object>) values;
  }

}
