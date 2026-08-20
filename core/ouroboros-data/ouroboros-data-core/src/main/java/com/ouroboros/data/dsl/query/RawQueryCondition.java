package com.ouroboros.data.dsl.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RawQueryCondition implements QueryCondition {
  private final Map<String, Object> rawCondition;

  private RawQueryCondition(Map<String, Object> rawCondition) {
    this.rawCondition = rawCondition;
  }

  static QueryCondition fieldValue(String field, Object value) {
    if (value instanceof QueryExpression<?>) {
      return fieldOperator(field, "$eq", value);
    }
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put(field, value);
    return new RawQueryCondition(raw);
  }

  static QueryCondition fieldOperator(String field, String operator, Object value) {
    Map<String, Object> operation = new LinkedHashMap<>();
    operation.put(operator, QueryRawValues.normalizeExpressionValue(value));

    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put(field, operation);
    return new RawQueryCondition(raw);
  }

  static QueryCondition combine(String operator, Collection<? extends QueryCondition> conditions) {
    List<Object> rawConditions = new ArrayList<>();
    for (QueryCondition condition : conditions) {
      rawConditions.add(condition.toRawCondition());
    }

    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put(operator, rawConditions);
    return new RawQueryCondition(raw);
  }

  @Override
  public Map<String, Object> toRawCondition() {
    return QueryRawValues.normalizeWhereMap(rawCondition);
  }
}
