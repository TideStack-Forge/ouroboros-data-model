package com.ouroboros.data.expression;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility for building expression contexts without depending on a concrete expression engine.
 */
public final class DataExpressionContext {
  private DataExpressionContext() {
  }

  public static Map<String, Object> wrap(Map<String, Object> context, Map<String, Object> wrapper) {
    return new WrappedContext(context, wrapper);
  }

  private static class WrappedContext implements Map<String, Object> {
    private final Map<String, Object> context;
    private final Map<String, Object> wrapper;

    private WrappedContext(Map<String, Object> context, Map<String, Object> wrapper) {
      this.context = context == null ? Collections.emptyMap() : context;
      this.wrapper = wrapper == null ? Collections.emptyMap() : wrapper;
    }

    @Override
    public int size() {
      return wrapper.size() + context.size();
    }

    @Override
    public boolean isEmpty() {
      return wrapper.isEmpty() && context.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return wrapper.containsKey(key) || context.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return wrapper.containsValue(value) || context.containsValue(value);
    }

    @Override
    public Object get(Object key) {
      return wrapper.getOrDefault(key, context.get(key));
    }

    @Override
    public Set<String> keySet() {
      return Stream.concat(wrapper.keySet().stream(), context.keySet().stream())
          .collect(Collectors.toSet());
    }

    @Override
    public Collection<Object> values() {
      return Stream.concat(wrapper.values().stream(), context.values().stream())
          .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      return Stream.concat(wrapper.entrySet().stream(), context.entrySet().stream())
          .collect(Collectors.toSet());
    }

    @Override
    public Object put(String key, Object value) {
      throw new UnsupportedOperationException("Expression context is read-only");
    }

    @Override
    public Object remove(Object key) {
      throw new UnsupportedOperationException("Expression context is read-only");
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
      throw new UnsupportedOperationException("Expression context is read-only");
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException("Expression context is read-only");
    }
  }
}
