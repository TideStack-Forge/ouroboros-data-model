package com.ouroboros.data.dsl.statement;

import java.util.HashMap;
import java.util.Map;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;

public class DeleteStatement extends DMLStatement {

  protected DeleteStatement(Map<String, Object> statement) {
    super(statement);
  }

  public static DeleteStatement of(String entityName, SExpression<Boolean> condition) {
    var map = new HashMap<String, Object>();
    map.put(Keyword.DELETE.toString(), entityName);
    map.put(Keyword.WHERE.toString(), condition);
    return new DeleteStatement(map);
  }

  public String getEntityName() {
    var entityName = get(Keyword.DELETE.toString());
    if (entityName != null) {
      return entityName.toString();
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public SExpression<Boolean> getWhere() {
    var where = getOrDefault(Keyword.WHERE.toString(), SExpression.empty(Boolean.class));
    return (SExpression<Boolean>) where;
  }
}
