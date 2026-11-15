package com.ouroboros.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Effective unique constraint consumed at runtime.
 */
public final class DataModelUniqueConstraint {
  public static final String NULLS_DISTINCT_EXTRA_PROP_NAME = "nullsDistinct";

  private final String name;
  private final List<String> fields;
  private final UniquenessScope scope;
  private final Source source;
  private final String label;
  private final boolean nullsDistinct;

  DataModelUniqueConstraint(
      String name,
      List<String> fields,
      UniquenessScope scope,
      Source source,
      String label
  ) {
    this(name, fields, scope, source, label, false);
  }

  DataModelUniqueConstraint(
      String name,
      List<String> fields,
      UniquenessScope scope,
      Source source,
      String label,
      boolean nullsDistinct
  ) {
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.fields = Collections.unmodifiableList(new ArrayList<>(
        Objects.requireNonNull(fields, "fields must not be null")
    ));
    this.scope = Objects.requireNonNull(scope, "scope must not be null");
    this.source = Objects.requireNonNull(source, "source must not be null");
    this.label = Objects.requireNonNull(label, "label must not be null");
    this.nullsDistinct = nullsDistinct;
  }

  public String getName() {
    return name;
  }

  public List<String> getFields() {
    return fields;
  }

  public UniquenessScope getScope() {
    return scope;
  }

  public Source getSource() {
    return source;
  }

  public String getLabel() {
    return label;
  }

  public boolean isNullsDistinct() {
    return nullsDistinct;
  }

  public enum Source {
    FIELD,
    MODEL
  }
}
