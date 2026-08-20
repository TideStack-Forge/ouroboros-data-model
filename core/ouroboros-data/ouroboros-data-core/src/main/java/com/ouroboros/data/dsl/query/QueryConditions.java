package com.ouroboros.data.dsl.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

final class QueryConditions {

  private QueryConditions() {
    throw new UnsupportedOperationException("Utility class");
  }

  static List<QueryCondition> requireConditions(QueryCondition... conditions) {
    if (conditions == null) {
      throw new IllegalArgumentException("conditions must not be null");
    }
    return requireConditions(Arrays.asList(conditions));
  }

  static List<QueryCondition> requireConditions(Collection<? extends QueryCondition> conditions) {
    if (conditions == null) {
      throw new IllegalArgumentException("conditions must not be null");
    }
    for (QueryCondition condition : conditions) {
      if (condition == null) {
        throw new IllegalArgumentException("conditions must not contain null");
      }
    }
    return List.copyOf(conditions);
  }

  static Object combineAnd(QueryCondition... conditions) {
    return combineAnd(requireConditions(conditions));
  }

  static Object combineAnd(Collection<? extends QueryCondition> conditions) {
    List<QueryCondition> required = requireConditions(conditions);
    if (required.isEmpty()) {
      return new LinkedHashMap<String, Object>();
    }
    if (required.size() == 1) {
      return required.get(0).toRawCondition();
    }
    return RawQueryCondition.combine("$and", required).toRawCondition();
  }
}
