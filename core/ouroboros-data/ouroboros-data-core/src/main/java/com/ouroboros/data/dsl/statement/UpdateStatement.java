/*
 * @Author: Song Mingxu mingxu.song@gmail.com
 */
package com.ouroboros.data.dsl.statement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;

public class UpdateStatement extends DMLStatement {

  protected UpdateStatement(Map<String, ?> statement) {
    super(statement);
  }

  public static UpdateStatement of(String entityName, Map<String, ?> values) {
    var map = new HashMap<String, Object>();
    map.put(Keyword.UPDATE.toString(), entityName);
    map.put(Keyword.SET.toString(), values);
    map.put(Keyword.WHERE.toString(), SExpression.empty(Boolean.class));
    return new UpdateStatement(map);
  }

  public static UpdateStatement of(String entityName, Map<String, ?> values, SExpression<Boolean> where) {
    var map = new HashMap<String, Object>();
    map.put(Keyword.UPDATE.toString(), entityName);
    map.put(Keyword.SET.toString(), values);
    map.put(Keyword.WHERE.toString(), where);
    return new UpdateStatement(map);
  }

  public String getEntityName() {
    var name = get(Keyword.UPDATE.toString());
    return String.valueOf(name);
  }

  @SuppressWarnings("unchecked")
  public Map<String, ?> getValues() {
    var values = get(Keyword.SET.toString());
    if (values == null) {
      return Collections.emptyMap();
    }
    return (Map<String, ?>) values;
  }

  @SuppressWarnings("unchecked")
  public SExpression<Boolean> getWhere() {
    var where = getOrDefault(Keyword.WHERE.toString(), SExpression.empty(Boolean.class));
    return (SExpression<Boolean>) where;
  }
}
