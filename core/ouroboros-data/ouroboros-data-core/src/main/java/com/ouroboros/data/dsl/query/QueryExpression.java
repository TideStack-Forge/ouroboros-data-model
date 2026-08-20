package com.ouroboros.data.dsl.query;

import java.util.Collections;

/**
 * Query facade expression that can render itself to the existing raw map query language.
 *
 * @param <T> expression value type
 */
public interface QueryExpression<T> {

  Object toRawValue();

  default QueryExpression<T> as(String alias) {
    if (alias == null || alias.isBlank()) {
      throw new IllegalArgumentException("alias must not be blank");
    }
    return () -> Collections.singletonMap(alias, toRawValue());
  }
}
