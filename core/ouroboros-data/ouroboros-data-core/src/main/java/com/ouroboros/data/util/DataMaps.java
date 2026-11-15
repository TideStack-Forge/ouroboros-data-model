package com.ouroboros.data.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.vavr.Function3;
import io.vavr.Tuple;
import io.vavr.control.Try;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

public final class DataMaps {
  private static final ClassValue<Map<String, JavaType>> BEAN_PROPERTY_TYPES = new ClassValue<>() {
    @Override protected Map<String, JavaType> computeValue(Class<?> type) {
      return Try.of(() -> java.util.Collections.unmodifiableMap(inspectBeanPropertyTypes(type))).get();
    }
  };

  private DataMaps() {
  }

  public static boolean isStringKeyMap(Map<?, ?> map) {
    return map != null && !map.isEmpty() && map.keySet().stream().allMatch(CharSequence.class::isInstance);
  }

  public static <K1, K, V> Map<K, V> remap(Map<K1, V> map, java.util.function.Function<K1, K> keyMapper) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(keyMapper, "keyMapper cannot be null");
    return map.entrySet().stream()
        .map(entry -> Tuple.of(keyMapper.apply(entry.getKey()), entry.getValue()))
        .collect(LinkedHashMap::new, (result, tuple) -> result.put(tuple._1(), tuple._2()), Map::putAll);
  }

  public static <K1, K, V1, V> Map<K, V> remap(
      Map<K1, V1> map,
      java.util.function.Function<K1, K> keyMapper,
      java.util.function.Function<V1, V> valueMapper
  ) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(keyMapper, "keyMapper cannot be null");
    Objects.requireNonNull(valueMapper, "valueMapper cannot be null");
    return map.entrySet().stream()
        .map(entry -> Tuple.of(keyMapper.apply(entry.getKey()), valueMapper.apply(entry.getValue())))
        .collect(LinkedHashMap::new, (result, tuple) -> result.put(tuple._1(), tuple._2()), Map::putAll);
  }

  public static <K, V> Map<K, V> merge(Map<K, V>... sources) {
    return mergeTo(new LinkedHashMap<>(), sources);
  }

  public static <K, V> Map<K, V> merge(Supplier<Map<K, V>> supplier, Map<K, V>... sources) {
    return mergeTo(supplier.get(), sources);
  }

  public static <K, V> Map<K, V> merge(Function3<K, V, V, V> valueMerger, Map<K, V>... sources) {
    return mergeTo(new LinkedHashMap<>(), valueMerger, sources);
  }

  public static <K, V> Map<K, V> mergeTo(Map<K, V> target, Map<K, V>... sources) {
    return mergeTo(target, (key, left, right) -> right, sources);
  }

  public static <K, V> Map<K, V> mergeTo(Map<K, V> target, Function3<K, V, V, V> valueMerger, Map<K, V>... sources) {
    Objects.requireNonNull(target, "target cannot be null");
    Objects.requireNonNull(valueMerger, "valueMerger cannot be null");
    for (var source : sources) {
      if (source == null) {
        continue;
      }
      source.forEach((key, value) -> target.merge(key, value, (left, right) -> valueMerger.apply(key, left, right)));
    }
    return target;
  }

  public static <T extends Map<K, V>, K, V> T deepClone(T map) {
    return deepClone(map, () -> instantiateMap(map));
  }

  public static <T extends Map<K, V>, K, V> T deepClone(T map, Supplier<T> supplier) {
    var result = supplier.get();
    for (var entry : map.entrySet()) {
      result.put(entry.getKey(), cloneValue(entry.getValue()));
    }
    return result;
  }

  public static <K, V> Map<K, V> omit(Map<K, V> map, Predicate<K> predicate) {
    var result = new LinkedHashMap<K, V>();
    map.forEach((key, value) -> {
      if (!predicate.test(key)) {
        result.put(key, value);
      }
    });
    return result;
  }

  public static <K, V> Map<K, V> remove(Map<K, V> map, K... keys) {
    for (var key : keys) {
      map.remove(key);
    }
    return map;
  }

  public static <T> T toBean(Class<T> type, Map<String, Object> map) {
    return tryToBean(type, map).getOrNull();
  }

  public static <T> Try<T> tryToBean(Class<T> type, Map<String, Object> map) {
    return Try.of(() -> DataJson.OBJECT_MAPPER.convertValue(normalizeJsonStringValues(type, map), type));
  }

  public static Map<String, Object> fromBean(Object bean) {
    if (bean instanceof Map<?, ?> map) {
      var result = new LinkedHashMap<String, Object>();
      map.forEach((key, value) -> result.put(String.valueOf(key), value));
      return result;
    }
    var javaType = DataJson.OBJECT_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class);
    return DataJson.OBJECT_MAPPER.convertValue(bean, javaType);
  }

  private static <V> V cloneValue(V value) {
    if (value instanceof Map<?, ?> nestedMap) {
      return (V) deepClone(toStringKeyMap(nestedMap));
    }
    if (value instanceof Collection<?> collection) {
      var cloned = new ArrayList<>();
      collection.forEach(item -> cloned.add(cloneValue(item)));
      return (V) cloned;
    }
    return value;
  }

  private static Map<Object, Object> toStringKeyMap(Map<?, ?> map) {
    var result = new LinkedHashMap<Object, Object>();
    map.forEach(result::put);
    return result;
  }

  private static <T extends Map<K, V>, K, V> T instantiateMap(T source) {
    if (source instanceof java.util.SortedMap<?, ?> sortedMap) {
      return (T) new java.util.TreeMap<>(((java.util.SortedMap<K, V>) sortedMap).comparator());
    }
    return (T) new LinkedHashMap<K, V>();
  }

  private static Map<String, Object> normalizeJsonStringValues(Class<?> type, Map<String, Object> map)
      throws IntrospectionException, IOException {
    if (map == null || map.isEmpty()) {
      return map;
    }
    var result = new LinkedHashMap<>(map);
    for (var propertyType : BEAN_PROPERTY_TYPES.get(type).entrySet()) {
      var propertyName = propertyType.getKey();
      if (!result.containsKey(propertyName)) {
        continue;
      }
      result.put(propertyName, parseJsonStringValue(result.get(propertyName), propertyType.getValue()));
    }
    return result;
  }

  private static Map<String, JavaType> inspectBeanPropertyTypes(Class<?> type) throws IntrospectionException {
    var typeFactory = DataJson.OBJECT_MAPPER.getTypeFactory();
    var propertyTypes = new LinkedHashMap<String, JavaType>();

    for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
      for (var field : current.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          propertyTypes.putIfAbsent(field.getName(), typeFactory.constructType(field.getGenericType()));
        }
      }
    }

    var beanInfo = Introspector.getBeanInfo(type, Object.class);
    for (var descriptor : beanInfo.getPropertyDescriptors()) {
      Type propertyType = null;
      if (descriptor.getWriteMethod() != null) {
        propertyType = descriptor.getWriteMethod().getGenericParameterTypes()[0];
      } else if (descriptor.getReadMethod() != null) {
        propertyType = descriptor.getReadMethod().getGenericReturnType();
      } else if (descriptor.getPropertyType() != null) {
        propertyType = descriptor.getPropertyType();
      }
      if (propertyType != null) {
        propertyTypes.put(descriptor.getName(), typeFactory.constructType(propertyType));
      }
    }
    return propertyTypes;
  }

  private static Object parseJsonStringValue(Object value, JavaType targetType) throws IOException {
    if (!(value instanceof CharSequence textValue) || !requiresJsonStringParsing(targetType)) {
      return value;
    }
    String text = textValue.toString().trim();
    if (!looksLikeJsonValue(text)) {
      return value;
    }
    return DataJson.OBJECT_MAPPER.readValue(text, targetType);
  }

  private static boolean requiresJsonStringParsing(JavaType targetType) {
    if (targetType.isContainerType() || targetType.isMapLikeType() || targetType.isTypeOrSubTypeOf(JsonNode.class)) {
      return true;
    }
    return !isSimpleStringTarget(targetType.getRawClass());
  }

  private static boolean looksLikeJsonValue(String text) {
    return text.equals("null") || text.startsWith("{") || text.startsWith("[");
  }

  private static boolean isSimpleStringTarget(Class<?> rawClass) {
    return rawClass.isPrimitive()
        || rawClass.isEnum()
        || CharSequence.class.isAssignableFrom(rawClass)
        || Number.class.isAssignableFrom(rawClass)
        || Boolean.class == rawClass
        || Character.class == rawClass
        || Date.class.isAssignableFrom(rawClass)
        || Temporal.class.isAssignableFrom(rawClass);
  }
}
