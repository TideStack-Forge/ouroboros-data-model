package com.ouroboros.data.dsl.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;

/**
 * Fluent populate entry used by the query facade to render the existing raw POPULATE DSL.
 */
public final class PopulateSpec {
  private final String fieldName;
  private final Map<String, Object> options = new LinkedHashMap<>();

  PopulateSpec(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) {
      throw new IllegalArgumentException("populate fieldName must not be blank");
    }
    this.fieldName = fieldName;
  }

  public PopulateSpec select(String... fields) {
    options.put(Keyword.SELECT.toString(), copyStrings("fields", fields));
    return this;
  }

  public PopulateSpec select(Collection<?> select) {
    options.put(Keyword.SELECT.toString(), requireClause("select", select));
    return this;
  }

  public PopulateSpec select(Map<String, ?> select) {
    options.put(Keyword.SELECT.toString(), copyRaw(select));
    return this;
  }

  public PopulateSpec select(SExpression<?> select) {
    options.put(Keyword.SELECT.toString(), requireClause("select", select));
    return this;
  }

  public PopulateSpec omit(String... fields) {
    options.put(Keyword.OMIT.toString(), copyStrings("fields", fields));
    return this;
  }

  public PopulateSpec omit(String omit) {
    if (omit == null || omit.isBlank()) {
      throw new IllegalArgumentException("omit must not be blank");
    }
    options.put(Keyword.OMIT.toString(), omit);
    return this;
  }

  public PopulateSpec omit(Collection<?> omit) {
    options.put(Keyword.OMIT.toString(), requireClause("omit", omit));
    return this;
  }

  public PopulateSpec where(QueryCondition... conditions) {
    options.put(Keyword.WHERE.toString(), QueryConditions.combineAnd(conditions));
    return this;
  }

  public PopulateSpec where(Collection<?> where) {
    options.put(Keyword.WHERE.toString(), whereFromCollection(where));
    return this;
  }

  public PopulateSpec where(Map<String, ?> where) {
    options.put(Keyword.WHERE.toString(), requireWhereClause(where));
    return this;
  }

  public PopulateSpec where(SExpression<Boolean> where) {
    options.put(Keyword.WHERE.toString(), requireWhereClause(where));
    return this;
  }

  public PopulateSpec limit(Integer limit) {
    options.put(Keyword.LIMIT.toString(), requireNonNegative("limit", limit));
    return this;
  }

  public PopulateSpec offset(Integer offset) {
    options.put(Keyword.OFFSET.toString(), requireNonNegative("offset", offset));
    return this;
  }

  public PopulateSpec populate(String... fields) {
    options.put(Keyword.POPULATE.toString(), copyStrings("fields", fields));
    return this;
  }

  public PopulateSpec populate(Collection<?> populate) {
    options.put(Keyword.POPULATE.toString(), copyRaw(populate));
    return this;
  }

  public PopulateSpec populate(Map<String, ?> populate) {
    options.put(Keyword.POPULATE.toString(), copyRaw(populate));
    return this;
  }

  public PopulateSpec populate(String populate) {
    if (populate == null || populate.isBlank()) {
      throw new IllegalArgumentException("populate must not be blank");
    }
    options.put(Keyword.POPULATE.toString(), populate);
    return this;
  }

  public PopulateSpec populate(PopulateSpec... populates) {
    options.put(Keyword.POPULATE.toString(), toRawMap(populates));
    return this;
  }

  String fieldName() {
    return fieldName;
  }

  Object toRawOptions() {
    return options.isEmpty() ? null : new LinkedHashMap<>(options);
  }

  static Map<String, Object> toRawMap(PopulateSpec... populates) {
    if (populates == null) {
      throw new IllegalArgumentException("populates must not be null");
    }
    Map<String, Object> raw = new LinkedHashMap<>();
    for (PopulateSpec populate : populates) {
      if (populate == null) {
        throw new IllegalArgumentException("populates must not contain null");
      }
      raw.put(populate.fieldName(), populate.toRawOptions());
    }
    return raw;
  }

  static Object copyRaw(Object raw) {
    return QueryRawValues.normalize(raw);
  }

  private static List<String> copyStrings(String argumentName, String... values) {
    if (values == null) {
      throw new IllegalArgumentException(argumentName + " must not be null");
    }
    List<String> copy = new ArrayList<>();
    for (String value : values) {
      copy.add(requireText(argumentName, value));
    }
    return copy;
  }

  private static List<String> copyStrings(String argumentName, Collection<String> values) {
    if (values == null) {
      throw new IllegalArgumentException(argumentName + " must not be null");
    }
    List<String> copy = new ArrayList<>();
    for (String value : values) {
      copy.add(requireText(argumentName, value));
    }
    return copy;
  }

  private static String requireText(String argumentName, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(argumentName + " must not contain blank values");
    }
    return value;
  }

  private static Integer requireNonNegative(String argumentName, Integer value) {
    Objects.requireNonNull(value, argumentName + " must not be null");
    if (value < 0) {
      throw new IllegalArgumentException(argumentName + " must not be negative");
    }
    return value;
  }

  private static Object requireClause(String argumentName, Object value) {
    Objects.requireNonNull(value, argumentName + " must not be null");
    return copyRaw(value);
  }

  private static Object requireWhereClause(Object value) {
    Objects.requireNonNull(value, "where must not be null");
    return QueryRawValues.normalizeWhere(value);
  }

  private static Object whereFromCollection(Collection<?> where) {
    if (where == null) {
      throw new IllegalArgumentException("where must not be null");
    }
    if (where.isEmpty()) {
      return new LinkedHashMap<String, Object>();
    }

    List<QueryCondition> conditions = new ArrayList<>();
    boolean allConditions = true;
    for (Object item : where) {
      if (item instanceof QueryCondition condition) {
        conditions.add(condition);
      } else {
        allConditions = false;
      }
    }
    if (allConditions) {
      return QueryConditions.combineAnd(conditions);
    }

    List<Object> items = new ArrayList<>();
    for (Object item : where) {
      if (item == null) {
        throw new IllegalArgumentException("where must not contain null");
      }
      items.add(item instanceof QueryCondition condition
          ? condition.toRawCondition()
          : QueryRawValues.normalizeWhere(item));
    }
    return List.copyOf(items);
  }
}
