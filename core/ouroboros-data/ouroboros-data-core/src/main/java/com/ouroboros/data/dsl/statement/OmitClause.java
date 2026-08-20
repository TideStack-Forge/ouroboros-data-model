package com.ouroboros.data.dsl.statement;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class OmitClause implements Serializable {
  private static final long serialVersionUID = 1L;

  private final Set<String> fields;

  private OmitClause(Set<String> fields) {
    this.fields = fields;
  }

  public static OmitClause fromRaw(Object rawValue) {
    if (rawValue instanceof CharSequence cs) {
      return new OmitClause(Arrays.stream(cs.toString().split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .collect(Collectors.toSet()));
    }
    if (rawValue instanceof Collection<?> coll) {
      return new OmitClause(coll.stream()
          .filter(CharSequence.class::isInstance)
          .map(Object::toString)
          .map(String::trim)
          .collect(Collectors.toSet()));
    }
    return new OmitClause(Collections.emptySet());
  }

  public Set<String> getFields() {
    return fields;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof OmitClause other)) return false;
    return Objects.equals(fields, other.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fields);
  }
}
