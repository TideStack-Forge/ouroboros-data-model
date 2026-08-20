package com.ouroboros.data.dsl.query;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.querydsl.core.types.Operator;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.normalize.OperatorAliasResolver;

/**
 * Small raw-value helper for query facade objects.
 */
final class QueryRawValues {

  private QueryRawValues() {
    throw new UnsupportedOperationException("Utility class");
  }

  static Object normalize(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof QueryExpression<?> expression) {
      return normalize(expression.toRawValue());
    }
    if (value instanceof QueryCondition condition) {
      return normalize(condition.toRawCondition());
    }
    if (value instanceof QuerySource source) {
      return normalize(source.toRawFrom());
    }
    if (value instanceof Enum<?> enumValue) {
      return enumValue.name();
    }
    if (value instanceof Map<?, ?> map) {
      return normalizeMap(map);
    }
    if (value instanceof Collection<?> collection) {
      return normalizeCollection(collection);
    }
    if (value.getClass().isArray()) {
      return normalizeArray(value);
    }
    return value;
  }

  static Object normalizeExpressionValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof QueryExpression<?> expression) {
      return normalizeExpressionRawValue(expression.toRawValue());
    }
    if (value instanceof QueryCondition condition) {
      return normalizeWhere(condition.toRawCondition());
    }
    if (value instanceof Enum<?> enumValue) {
      return enumValue.name();
    }
    if (value instanceof Map<?, ?> map) {
      return normalizeExpressionMap(map);
    }
    if (value instanceof Collection<?> collection) {
      return normalizeExpressionCollection(collection);
    }
    if (value.getClass().isArray()) {
      return normalizeExpressionArray(value);
    }
    return value;
  }

  static Object normalizeWhere(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof QueryCondition condition) {
      return normalizeWhere(condition.toRawCondition());
    }
    if (value instanceof Map<?, ?> map) {
      return normalizeWhereMap(map);
    }
    if (value instanceof Collection<?> collection) {
      return normalizeWhereCollection(collection);
    }
    if (value.getClass().isArray()) {
      return normalizeWhereArray(value);
    }
    return normalize(value);
  }

  static Map<String, Object> normalizeMap(Map<?, ?> map) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String stringKey = requireStringKey(entry.getKey());
      normalized.put(stringKey, normalize(entry.getValue()));
    }
    return normalized;
  }

  static Map<String, Object> normalizeWhereMap(Map<?, ?> map) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String key = requireStringKey(entry.getKey());
      Object value = entry.getValue();
      if (isLogicalOperatorKey(key)) {
        normalized.put(key, normalizeWhere(value));
      } else if (key.startsWith("$")) {
        normalized.put(key, normalizeExpressionValue(value));
      } else {
        normalized.put(key, normalizeFieldConditionValue(value));
      }
    }
    return normalized;
  }

  private static List<Object> normalizeCollection(Collection<?> collection) {
    List<Object> normalized = new ArrayList<>(collection.size());
    for (Object item : collection) {
      normalized.add(normalize(item));
    }
    return List.copyOf(normalized);
  }

  private static List<Object> normalizeArray(Object array) {
    int length = Array.getLength(array);
    List<Object> normalized = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      normalized.add(normalize(Array.get(array, i)));
    }
    return List.copyOf(normalized);
  }

  private static Object normalizeExpressionRawValue(Object rawValue) {
    if (rawValue instanceof CharSequence fieldPath) {
      return fieldExpression(fieldPath.toString());
    }
    return normalizeExpressionValue(rawValue);
  }

  private static List<String> fieldExpression(String fieldPath) {
    List<String> expression = new ArrayList<>();
    expression.add("FIELD");
    for (String segment : fieldPath.split("\\.")) {
      String trimmed = segment.trim();
      if (!trimmed.isEmpty()) {
        expression.add(trimmed);
      }
    }
    if (expression.size() == 1) {
      throw new IllegalArgumentException("field expression must not be blank");
    }
    return List.copyOf(expression);
  }

  private static Map<String, Object> normalizeExpressionMap(Map<?, ?> map) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String stringKey = requireStringKey(entry.getKey());
      normalized.put(stringKey, normalizeExpressionValue(entry.getValue()));
    }
    return normalized;
  }

  private static List<Object> normalizeExpressionCollection(Collection<?> collection) {
    List<Object> normalized = new ArrayList<>(collection.size());
    for (Object item : collection) {
      normalized.add(normalizeExpressionValue(item));
    }
    return List.copyOf(normalized);
  }

  private static List<Object> normalizeExpressionArray(Object array) {
    int length = Array.getLength(array);
    List<Object> normalized = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      normalized.add(normalizeExpressionValue(Array.get(array, i)));
    }
    return List.copyOf(normalized);
  }

  private static Object normalizeFieldConditionValue(Object value) {
    if (value instanceof QueryExpression<?>) {
      Map<String, Object> operation = new LinkedHashMap<>();
      operation.put("$eq", normalizeExpressionValue(value));
      return operation;
    }
    if (value instanceof QueryCondition condition) {
      return normalizeWhere(condition.toRawCondition());
    }
    if (value instanceof Map<?, ?> map) {
      return normalizeFieldConditionMap(map);
    }
    return normalize(value);
  }

  private static Map<String, Object> normalizeFieldConditionMap(Map<?, ?> map) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String key = requireStringKey(entry.getKey());
      Object value = entry.getValue();
      if (isLogicalOperatorKey(key)) {
        normalized.put(key, normalizeWhere(value));
      } else if (isOperatorKey(key)) {
        normalized.put(key, normalizeExpressionValue(value));
      } else {
        normalized.put(key, normalizeFieldConditionValue(value));
      }
    }
    return normalized;
  }

  private static List<Object> normalizeWhereCollection(Collection<?> collection) {
    List<Object> normalized = new ArrayList<>(collection.size());
    for (Object item : collection) {
      normalized.add(normalizeWhere(item));
    }
    return List.copyOf(normalized);
  }

  private static List<Object> normalizeWhereArray(Object array) {
    int length = Array.getLength(array);
    List<Object> normalized = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      normalized.add(normalizeWhere(Array.get(array, i)));
    }
    return List.copyOf(normalized);
  }

  private static boolean isLogicalOperatorKey(String key) {
    return tryResolveOperatorKey(key)
        .filter(Operators::isLogicCombinationOperator)
        .isPresent();
  }

  private static boolean isOperatorKey(String key) {
    return tryResolveOperatorKey(key).isPresent();
  }

  private static Optional<Operator> tryResolveOperatorKey(String key) {
    String normalized = key.startsWith("$") ? key.substring(1) : key;
    return OperatorAliasResolver.tryResolveOperator(normalized);
  }

  private static String requireStringKey(Object key) {
    if (!(key instanceof String stringKey) || stringKey.isBlank()) {
      throw new IllegalArgumentException("raw map keys must be non-blank strings");
    }
    return stringKey;
  }
}
