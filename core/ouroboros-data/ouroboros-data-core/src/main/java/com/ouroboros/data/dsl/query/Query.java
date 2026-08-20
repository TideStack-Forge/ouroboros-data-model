package com.ouroboros.data.dsl.query;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Static entry points for the query facade.
 */
public final class Query {

  private Query() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static DataModelQuery from(String modelName) {
    if (modelName == null || modelName.isBlank()) {
      throw new IllegalArgumentException("modelName must not be blank");
    }
    return DefaultDataModelQuery.standalone(modelName);
  }

  public static DataModelQuery from(QuerySource source) {
    return DefaultDataModelQuery.standalone(
        Objects.requireNonNull(source, "source must not be null").toRawFrom());
  }

  public static PopulateSpec populate(String fieldName) {
    return new PopulateSpec(fieldName);
  }

  public static QueryField<Object> field(String firstSegment, String... moreSegments) {
    if (firstSegment == null || firstSegment.isBlank()) {
      throw new IllegalArgumentException("field segment must not be blank");
    }
    String suffix = Arrays.stream(Objects.requireNonNull(moreSegments, "moreSegments must not be null"))
        .peek(Query::requireSegment)
        .collect(Collectors.joining("."));
    return suffix.isEmpty()
        ? new QueryField<>(firstSegment)
        : new QueryField<>(firstSegment + "." + suffix);
  }

  public static QueryCondition and(QueryCondition... conditions) {
    return RawQueryCondition.combine("$and", QueryConditions.requireConditions(conditions));
  }

  public static QueryCondition or(QueryCondition... conditions) {
    return RawQueryCondition.combine("$or", QueryConditions.requireConditions(conditions));
  }

  private static void requireSegment(String segment) {
    if (segment == null || segment.isBlank()) {
      throw new IllegalArgumentException("field segment must not be blank");
    }
  }
}
