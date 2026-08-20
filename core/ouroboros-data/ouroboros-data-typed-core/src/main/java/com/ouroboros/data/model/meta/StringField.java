package com.ouroboros.data.model.meta;

import com.ouroboros.data.dsl.query.Query;
import com.ouroboros.data.dsl.query.QueryCondition;

/**
 * Typed string field path.
 *
 * @param <OWNER> owner meta type
 */
public final class StringField<OWNER extends TypedModelMeta<?, ?>>
    extends TypedField<String, OWNER> {

  StringField(TypedModelMeta<?, ?> owner, String fieldName) {
    super(owner, fieldName);
  }

  public QueryCondition contains(String value) {
    return Query.field(getPath()).contains(value);
  }

  public QueryCondition startsWith(String value) {
    return Query.field(getPath()).startsWith(value);
  }

  public QueryCondition endsWith(String value) {
    return Query.field(getPath()).endsWith(value);
  }
}
