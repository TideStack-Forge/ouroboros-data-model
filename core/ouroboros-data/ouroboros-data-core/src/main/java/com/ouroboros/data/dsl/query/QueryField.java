package com.ouroboros.data.dsl.query;

import java.util.Arrays;
import java.util.List;

/**
 * Field expression with comparison helpers.
 *
 * @param <T> field value type
 */
public final class QueryField<T> implements QueryExpression<T> {
  private final String path;

  QueryField(String path) {
    this.path = path;
  }

  @Override
  public Object toRawValue() {
    return path;
  }

  public QueryCondition eq(Object value) {
    return RawQueryCondition.fieldValue(path, value);
  }

  public QueryCondition ne(Object value) {
    return operator("$ne", value);
  }

  public QueryCondition gt(Object value) {
    return operator("$gt", value);
  }

  public QueryCondition gte(Object value) {
    return operator("$gte", value);
  }

  public QueryCondition lt(Object value) {
    return operator("$lt", value);
  }

  public QueryCondition lte(Object value) {
    return operator("$lte", value);
  }

  public QueryCondition in(Object... values) {
    return operator("$in", Arrays.asList(values));
  }

  public QueryCondition contains(Object value) {
    return operator("$contains", value);
  }

  public QueryCondition startsWith(Object value) {
    return operator("$startsWith", value);
  }

  public QueryCondition endsWith(Object value) {
    return operator("$endsWith", value);
  }

  public QueryCondition between(Object lower, Object upper) {
    return operator("$between", List.of(lower, upper));
  }

  private QueryCondition operator(String operator, Object value) {
    return RawQueryCondition.fieldOperator(path, operator, value);
  }
}
