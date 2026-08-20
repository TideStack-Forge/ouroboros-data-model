package com.ouroboros.data.model.meta;

import java.util.List;
import java.util.Objects;

import com.ouroboros.data.dsl.query.Query;
import com.ouroboros.data.dsl.query.QueryCondition;
import com.ouroboros.data.dsl.query.QueryExpression;

/**
 * Base typed field path used by generated model meta classes.
 *
 * @param <T>     field value type
 * @param <OWNER> owner meta type
 */
public class TypedField<T, OWNER extends TypedModelMeta<?, ?>>
    implements QueryExpression<T> {
  private final TypedModelMeta<?, ?> owner;
  private final String fieldName;

  protected TypedField(TypedModelMeta<?, ?> owner, String fieldName) {
    this.owner = Objects.requireNonNull(owner, "owner must not be null");
    if (fieldName == null || fieldName.isBlank()) {
      throw new IllegalArgumentException("fieldName must not be blank");
    }
    this.fieldName = fieldName;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getPath() {
    return owner.qualify(fieldName);
  }

  @Override
  public Object toRawValue() {
    return getPath();
  }

  public QueryCondition eq(T value) {
    return Query.field(getPath()).eq(value);
  }

  public QueryCondition ne(T value) {
    return Query.field(getPath()).ne(value);
  }

  public QueryCondition gt(T value) {
    return Query.field(getPath()).gt(value);
  }

  public QueryCondition gte(T value) {
    return Query.field(getPath()).gte(value);
  }

  public QueryCondition lt(T value) {
    return Query.field(getPath()).lt(value);
  }

  public QueryCondition lte(T value) {
    return Query.field(getPath()).lte(value);
  }

  @SafeVarargs
  public final QueryCondition in(T... values) {
    return Query.field(getPath()).in((Object[]) values);
  }

  public QueryCondition between(T lower, T upper) {
    return Query.field(getPath()).between(lower, upper);
  }

  public QueryCondition in(List<? extends T> values) {
    return Query.field(getPath()).in(values.toArray());
  }
}
