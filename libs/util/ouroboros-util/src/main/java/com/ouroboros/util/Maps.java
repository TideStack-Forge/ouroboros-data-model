package com.ouroboros.util;

import static io.vavr.Predicates.instanceOf;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vavr.Function3;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.control.Either;
import io.vavr.control.Try;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import com.ouroboros.util.map.*;

/**
 * Map 工具类
 * <p>提供各种Map操作的实用工具方法，包括：</p>
 * <ul>
 *   <li>基础检查和判断</li>
 *   <li>键值重映射和转换</li>
 *   <li>Map合并和深度操作</li>
 *   <li>数据过滤和提取</li>
 *   <li>Bean与Map互转</li>
 *   <li>嵌套Map结构操作</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class Maps {
  private static final Collection<MapDeserializer> MAP_DESERIALIZERS = MapDeserializer.getAllFromSPI();
  private static final Collection<MapSerializer> MAP_SERIALIZERS = MapSerializer.getAllFromSPI();
  private static final ClassValue<Constructor<?>> CONSTRUCTORS = new ClassValue<>() {
    @Override
    protected Constructor<?> computeValue(Class<?> type) {
      try {
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor;
      }
      catch (NoSuchMethodException e) {
        throw new IllegalStateException("No default constructor found for " + type.getName(), e);
      }
    }
  };
  private static final ClassValue<List<BeanProperty>> BEAN_PROPERTIES = new ClassValue<>() {
    @Override
    protected List<BeanProperty> computeValue(Class<?> type) {
      try {
        return Collections.unmodifiableList(Arrays.stream(Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors())
            .filter(property -> !shouldSkipBeanProperty(property))
            .map(property -> new BeanProperty(type, property))
            .collect(Collectors.toList()));
      }
      catch (Exception e) {
        throw new IllegalStateException("Failed to inspect bean properties for " + type.getName(), e);
      }
    }
  };
  private static final ClassValue<Map<TypeVariable<?>, Type>> TYPE_VARIABLES = new ClassValue<>() {
    @Override
    protected Map<TypeVariable<?>, Type> computeValue(Class<?> type) {
      Map<TypeVariable<?>, Type> resolvedTypes = new LinkedHashMap<>();
      collectTypeVariables(type, resolvedTypes, new HashSet<>());
      return Collections.unmodifiableMap(resolvedTypes);
    }
  };
  private static final ClassValue<List<Method>> ENUM_FACTORY_METHODS = new ClassValue<>() {
    @Override
    protected List<Method> computeValue(Class<?> type) {
      if (!type.isEnum()) {
        return Collections.emptyList();
      }
      return Collections.unmodifiableList(Arrays.stream(type.getDeclaredMethods())
          .filter(method -> Modifier.isStatic(method.getModifiers()))
          .filter(method -> type.isAssignableFrom(method.getReturnType()))
          .filter(method -> method.getParameterCount() == 1)
          .filter(method -> method.getParameterTypes()[0].isAssignableFrom(String.class))
          .filter(method -> method.isAnnotationPresent(JsonCreator.class) || "fromValue".equals(method.getName()))
          .sorted(Comparator
              .comparing((Method method) -> !method.isAnnotationPresent(JsonCreator.class))
              .thenComparing(method -> !"fromValue".equals(method.getName())))
          .peek(method -> method.setAccessible(true))
          .collect(Collectors.toList()));
    }
  };
  private static final ClassValue<List<Method>> ENUM_VALUE_METHODS = new ClassValue<>() {
    @Override
    protected List<Method> computeValue(Class<?> type) {
      if (!type.isEnum()) {
        return Collections.emptyList();
      }
      return Collections.unmodifiableList(Arrays.stream(type.getDeclaredMethods())
          .filter(method -> !Modifier.isStatic(method.getModifiers()))
          .filter(method -> method.getParameterCount() == 0)
          .filter(method -> method.isAnnotationPresent(JsonValue.class) || "getValue".equals(method.getName()))
          .sorted(Comparator.comparing((Method method) -> !method.isAnnotationPresent(JsonValue.class)))
          .peek(method -> method.setAccessible(true))
          .collect(Collectors.toList()));
    }
  };

  /**
   * 安全检查 Map 是否为 String 键类型的 Map
   *
   * @param map 要判断的Map，可以为null
   * @return 如果Map不为null且非空，并且所有键都是CharSequence的实例，则返回true；否则返回false
   * @example <pre>
   * Map&lt;String, Object&gt; stringKeyMap = Map.of("key1", "value1");
   * Map&lt;Integer, String&gt; intKeyMap = Map.of(1, "value1");
   *
   * assert Maps.isStringKeyMap(stringKeyMap) == true;
   * assert Maps.isStringKeyMap(intKeyMap) == false;
   * assert Maps.isStringKeyMap(null) == false;
   * assert Maps.isStringKeyMap(Collections.emptyMap()) == false;
   * </pre>
   */
  public static boolean isStringKeyMap(Map<?, ?> map) {
    if (map == null || map.isEmpty()) {
      return false;
    }
    return map.keySet().stream().allMatch(CharSequence.class::isInstance);
  }

  /**
   * 返回一个判断指定键是否存在的 Predicate
   *
   * @param <K> 键的类型
   * @param key 要判断的key，不能为null
   * @return Predicate&lt;Map&lt;K, ?&gt;&gt; 用于判断Map是否包含指定键的断言
   * @throws NullPointerException 如果key为null
   * @example <pre>
   * Predicate&lt;Map&lt;String, ?&gt;&gt; hasName = Maps.buildKeyPredicate("name");
   * Map&lt;String, Object&gt; map = Map.of("name", "John", "age", 30);
   *
   * assert hasName.test(map) == true;
   * assert hasName.test(Map.of("age", 30)) == false;
   * </pre>
   */
  public static <K> Predicate<Map<K, ?>> keyExistsPredicate(K key) {
    Objects.requireNonNull(key, "key cannot be null");
    return map -> containsKey(map, key);
  }

  /**
   * 安全判断 Map 是否包含指定 Key
   *
   * @param <K> 键的类型
   * @param map 目标Map，可以为null
   * @param key 要检查的键值，可以为null
   * @return 如果map不为null且包含指定key则返回true，否则返回false
   * @example <pre>
   * Map&lt;String, Object&gt; map = Map.of("key1", "value1");
   *
   * assert Maps.containsKey(map, "key1") == true;
   * assert Maps.containsKey(map, "key2") == false;
   * assert Maps.containsKey(null, "key1") == false;
   * </pre>
   */
  public static <K> boolean containsKey(Map<K, ?> map, K key) {
    return map != null && map.containsKey(key);
  }

  /**
   * 检查Map是否为空（null或无元素）
   *
   * @param map 要检查的Map
   * @return 如果map为null或无元素返回true，否则返回false
   * @example <pre>
   * assert Maps.isEmpty(null) == true;
   * assert Maps.isEmpty(Collections.emptyMap()) == true;
   * assert Maps.isEmpty(Map.of("key", "value")) == false;
   * </pre>
   */
  public static boolean isEmpty(Map<?, ?> map) {
    return map == null || map.isEmpty();
  }

  /**
   * 检查Map是否不为空（不为null且有元素）
   *
   * @param map 要检查的Map
   * @return 如果map不为null且有元素返回true，否则返回false
   */
  public static boolean isNotEmpty(Map<?, ?> map) {
    return !isEmpty(map);
  }

  /**
   * 重映射 Map 的键值
   *
   * @param <K1>      源Map的键类型
   * @param <K>       目标Map的键类型
   * @param <V>       值的类型
   * @param map       源 map，不能为null
   * @param keyMapper 键映射函数，不能为null
   * @return 改变过键的新 Map（值为浅拷贝）
   * @throws NullPointerException 如果map或keyMapper为null
   * @example <pre>
   * Map&lt;Integer, String&gt; intMap = Map.of(1, "one", 2, "two");
   * Map&lt;String, String&gt; stringMap = Maps.remap(intMap, Object::toString);
   * // 结果: {"1": "one", "2": "two"}
   * </pre>
   */
  public static <K1, K, V> Map<K, V> remap(Map<K1, V> map, Function<K1, K> keyMapper) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(keyMapper, "keyMapper cannot be null");

    return map.entrySet().stream()
        .map(entry -> Tuple.of(keyMapper.apply(entry.getKey()), entry.getValue()))
        .collect(LinkedHashMap::new, (m, t) -> m.put(t._1(), t._2()), Map::putAll);
  }

  /**
   * 重映射 Map 的键值（支持基于键值对的映射）
   *
   * @param <K1>      源Map的键类型
   * @param <K>       目标Map的键类型
   * @param <V>       值的类型
   * @param map       源 map，不能为null
   * @param keyMapper 键映射函数，接收键和值作为参数，不能为null
   * @return 改变过键的新 Map（值为浅拷贝）
   * @throws NullPointerException 如果map或keyMapper为null
   * @example <pre>
   * Map&lt;String, Integer&gt; map = Map.of("a", 1, "b", 2);
   * Map&lt;String, Integer&gt; result = Maps.remap(map, (key, value) -&gt; key + "_" + value);
   * // 结果: {"a_1": 1, "b_2": 2}
   * </pre>
   */
  public static <K1, K, V> Map<K, V> remap(Map<K1, V> map, BiFunction<K1, V, K> keyMapper) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(keyMapper, "keyMapper cannot be null");

    return map.entrySet().stream()
        .map(entry -> Tuple.of(keyMapper.apply(entry.getKey(), entry.getValue()), entry.getValue()))
        .collect(LinkedHashMap::new, (m, t) -> m.put(t._1(), t._2()), Map::putAll);
  }

  /**
   * 重映射 Map 的键值
   *
   * @param <K1>        源Map的键类型
   * @param <K>         目标Map的键类型
   * @param <V1>        源Map的值类型
   * @param <V>         目标Map的值类型
   * @param map         源 map，不能为null
   * @param keyMapper   键映射函数，不能为null
   * @param valueMapper 值映射函数，不能为null
   * @return 新Map
   * @throws NullPointerException 如果任何参数为null
   * @example <pre>
   * Map&lt;Integer, String&gt; map = Map.of(1, "one", 2, "two");
   * Map&lt;String, Integer&gt; result = Maps.remap(map, Object::toString, String::length);
   * // 结果: {"1": 3, "2": 3}
   * </pre>
   */
  public static <K1, K, V1, V> Map<K, V> remap(Map<K1, V1> map, Function<K1, K> keyMapper, Function<V1, V> valueMapper) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(keyMapper, "keyMapper cannot be null");
    Objects.requireNonNull(valueMapper, "valueMapper cannot be null");

    return map.entrySet().stream()
        .map(entry -> Tuple.of(keyMapper.apply(entry.getKey()), valueMapper.apply(entry.getValue())))
        .collect(LinkedHashMap::new, (m, t) -> m.put(t._1(), t._2()), Map::putAll);
  }

  /**
   * 重映射 Map 的键值（基于键值对的完全重映射）
   *
   * @param <K1>        源Map的键类型
   * @param <K>         目标Map的键类型
   * @param <V1>        源Map的值类型
   * @param <V>         目标Map的值类型
   * @param map         源 map，不能为null
   * @param keyMapper   键映射函数，接收键和值作为参数，不能为null
   * @param valueMapper 值映射函数，接收键和值作为参数，不能为null
   * @return 新Map
   * @throws NullPointerException 如果任何参数为null
   */
  public static <K1, K, V1, V> Map<K, V> remap(Map<K1, V1> map, BiFunction<K1, V1, K> keyMapper, BiFunction<K1, V1, V> valueMapper) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(keyMapper, "keyMapper cannot be null");
    Objects.requireNonNull(valueMapper, "valueMapper cannot be null");

    return map.entrySet().stream()
        .map(entry -> Tuple.of(
            keyMapper.apply(entry.getKey(), entry.getValue()),
            valueMapper.apply(entry.getKey(), entry.getValue())
        ))
        .collect(LinkedHashMap::new, (m, t) -> m.put(t._1(), t._2()), Map::putAll);
  }

  /**
   * 合并多个Map到目标Map中
   *
   * @param <K>         键的类型
   * @param <V>         值的类型
   * @param target      目标 map，源 Map 将直接在此修改，不能为null
   * @param sources     源 map数组，可以包含null元素
   * @param valueMerger 值冲突时的重映射函数，不能为null
   * @return 目标 map（已修改）
   * @throws NullPointerException 如果target、sources或valueMerger为null
   * @example <pre>
   * Map&lt;String, Integer&gt; target = new HashMap&lt;&gt;();
   * target.put("a", 1);
   *
   * Map&lt;String, Integer&gt; source1 = Map.of("b", 2, "c", 3);
   * Map&lt;String, Integer&gt; source2 = Map.of("c", 4, "d", 5);
   *
   * Maps.mergeTo(target, (key, oldVal, newVal) -&gt; oldVal + newVal, source1, source2);
   * // 结果: target = {a=1, b=2, c=7, d=5}
   * </pre>
   */
  @SafeVarargs
  public static <K, V> Map<K, V> mergeTo(Map<K, V> target, Function3<K, V, V, V> valueMerger, Map<K, V>... sources) {
    Objects.requireNonNull(target, "target map cannot be null");
    Objects.requireNonNull(sources, "sources array cannot be null");
    Objects.requireNonNull(valueMerger, "valueMerger function cannot be null");

    for (var source : sources) {
      if (source != null) {
        source.forEach((key, value) -> {
          var oldValue = target.get(key);
          var newValue = oldValue != null ? valueMerger.apply(key, oldValue, value) : value;
          target.put(key, newValue);
        });
      }
    }
    return target;
  }

  /**
   * 合并多个Map到目标Map中
   *
   * @param <K>         键的类型
   * @param <V>         值的类型
   * @param target      目标 map，源 Map 将直接在此修改，不能为null
   * @param sources     源 map数组，可以包含null元素
   * @param valueMerger 值冲突时的重映射函数，不能为null
   * @return 目标 map（已修改）
   * @throws NullPointerException 如果target、sources或valueMerger为null
   * @example <pre>
   * Map&lt;String, Integer&gt; target = new HashMap&lt;&gt;();
   * target.put("a", 1);
   *
   * Map&lt;String, Integer&gt; source1 = Map.of("b", 2, "c", 3);
   * Map&lt;String, Integer&gt; source2 = Map.of("c", 4, "d", 5);
   *
   * Maps.mergeTo(target, (oldVal, newVal) -&gt; oldVal + newVal, source1, source2);
   * // 结果: target = {a=1, b=2, c=7, d=5}
   * </pre>
   */
  @SafeVarargs
  public static <K, V> Map<K, V> mergeTo(Map<K, V> target, BiFunction<V, V, V> valueMerger, Map<K, V>... sources) {
    return mergeTo(target, (K k, V v1, V v2) -> valueMerger.apply(v1, v2), sources);
  }

  /**
   * 合并多个Map到目标Map中，当 target 中存在 source 中相同键的值时，以 source 中的值替换
   *
   * @param <K>     键的类型
   * @param <V>     值的类型
   * @param target  目标 map，源 Map 将直接在此修改，不能为null
   * @param sources 源 map数组，可以包含null元素
   * @return 目标 map（已修改）
   * @throws NullPointerException 如果target或sources为null
   * @example <pre>
   * Map&lt;String, String&gt; target = new HashMap&lt;&gt;();
   * target.put("name", "old");
   *
   * Maps.mergeTo(target, Map.of("name", "new", "age", "25"));
   * // 结果: target = {name=new, age=25}
   * </pre>
   */
  @SafeVarargs
  public static <K, V> Map<K, V> mergeTo(Map<K, V> target, Map<K, V>... sources) {
    return mergeTo(target, (k, v1, v2) -> v2, sources);
  }

  /**
   * 合并多个 Map，返回一个新的 Map
   *
   * @param <K>     键的类型
   * @param <V>     值的类型
   * @param sources 源 map数组，可以包含null元素
   * @return 合并后的新Map（LinkedHashMap实现）
   * @throws NullPointerException 如果sources为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> merge(Map<K, V>... sources) {
    return mergeTo(new LinkedHashMap<>(), sources);
  }

  /**
   * 合并多个 Map，返回一个新的 Map（支持自定义Map实现）
   *
   * @param <K>      键的类型
   * @param <V>      值的类型
   * @param supplier Map构造函数，不能为null
   * @param sources  源 map数组，可以包含null元素
   * @return 合并后的新Map
   * @throws NullPointerException 如果supplier或sources为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> merge(Supplier<Map<K, V>> supplier, Map<K, V>... sources) {
    Objects.requireNonNull(supplier, "supplier cannot be null");
    return mergeTo(supplier.get(), sources);
  }

  /**
   * 合并多个 Map，返回一个新的 Map（支持自定义合并策略）
   *
   * @param <K>         键的类型
   * @param <V>         值的类型
   * @param valueMerger 值冲突时的重映射函数，不能为null
   * @param sources     源 map数组，可以包含null元素
   * @return 合并后的新Map（LinkedHashMap实现）
   * @throws NullPointerException 如果valueMerger或sources为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> merge(Function3<K, V, V, V> valueMerger, Map<K, V>... sources) {
    return mergeTo(new LinkedHashMap<>(), valueMerger, sources);
  }

  /**
   * 合并多个 Map，返回一个新的 Map（支持自定义合并策略）
   *
   * @param <K>         键的类型
   * @param <V>         值的类型
   * @param valueMerger 值冲突时的重映射函数，不能为null
   * @param sources     源 map数组，可以包含null元素
   * @return 合并后的新Map（LinkedHashMap实现）
   * @throws NullPointerException 如果valueMerger或sources为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> merge(BiFunction<V, V, V> valueMerger, Map<K, V>... sources) {
    return mergeTo(new LinkedHashMap<>(), (K k, V v1, V v2) -> valueMerger.apply(v1, v2), sources);
  }

  /**
   * 合并多个 Map，返回一个新的 Map（支持自定义Map实现和合并策略）
   *
   * @param <K>         键的类型
   * @param <V>         值的类型
   * @param supplier    Map构造函数，不能为null
   * @param valueMerger 值冲突时的重映射函数，不能为null
   * @param sources     源 map数组，可以包含null元素
   * @return 合并后的新Map
   * @throws NullPointerException 如果supplier、valueMerger或sources为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> merge(Supplier<Map<K, V>> supplier, Function3<K, V, V, V> valueMerger, Map<K, V>... sources) {
    Objects.requireNonNull(supplier, "supplier cannot be null");
    return mergeTo(supplier.get(), valueMerger, sources);
  }


  @SafeVarargs
  public static <K, V> Map<K, V> merge(Supplier<Map<K, V>> supplier, BiFunction<V, V, V> valueMerger, Map<K, V>... sources) {
    Objects.requireNonNull(supplier, "supplier cannot be null");
    return mergeTo(supplier.get(), (K k, V v1, V v2) -> valueMerger.apply(v1, v2), sources);
  }

  /**
   * 深度合并多个Map到target中，返回target
   * <p>对于嵌套的Map值，会递归进行深度合并</p>
   *
   * @param <K>     键的类型
   * @param <V>     值的类型
   * @param target  目标 map，源 Map 将直接在此修改，不能为null
   * @param sources 源 map数组，可以包含null元素
   * @return 目标 map（已修改）
   * @throws NullPointerException 如果target或sources为null
   * @example <pre>
   * Map&lt;String, Object&gt; target = new HashMap&lt;&gt;();
   * target.put("user", Map.of("name", "John", "age", 30));
   *
   * Map&lt;String, Object&gt; source = Map.of("user", Map.of("city", "NYC", "age", 31));
   * Maps.deepMergeTo(target, source);
   * // 结果: target = {user={name=John, age=31, city=NYC}}
   * </pre>
   */
  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> deepMergeTo(Map<K, V> target, Map<K, V>... sources) {
    Function3<K, V, V, V> merger = (k, v1, v2) -> {
      if (v1 instanceof Map && v2 instanceof Map) {
        try {
          // 创建新Map避免修改原始数据
          Map<K, V> mergedMap = new LinkedHashMap<>((Map<K, V>) v1);
          return (V) deepMergeTo(mergedMap, (Map<K, V>) v2);
        } catch (ClassCastException e) {
          // 如果类型转换失败，返回新值
          return v2;
        }
      }
      return v2;
    };
    return mergeTo(target, merger, sources);
  }

  /**
   * 深度合并多个 Map，返回一个新的 Map
   *
   * @param <K>     键的类型
   * @param <V>     值的类型
   * @param sources 源 map数组，可以包含null元素
   * @return 深度合并后的新Map（LinkedHashMap实现）
   * @throws NullPointerException 如果sources为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> deepMerge(Map<K, V>... sources) {
    return deepMergeTo(new LinkedHashMap<>(), sources);
  }

  /**
   * 深度合并多个 Map，返回一个新的 Map（支持自定义Map实现）
   *
   * @param <K>      键的类型
   * @param <V>      值的类型
   * @param supplier Map构造函数，不能为null
   * @param sources  源 map数组，可以包含null元素
   * @return 深度合并后的新Map
   * @throws NullPointerException 如果supplier或sources为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> deepMerge(Supplier<Map<K, V>> supplier, Map<K, V>... sources) {
    Objects.requireNonNull(supplier, "supplier cannot be null");
    return deepMergeTo(supplier.get(), sources);
  }

  /**
   * 深度克隆Map（默认使用LinkedHashMap）
   *
   * @param <T> Map的具体类型
   * @param <K> 键的类型
   * @param <V> 值的类型
   * @param map 要克隆的Map，可以为null
   * @return 深度克隆的Map，如果输入为null则返回空的LinkedHashMap
   */
  @SuppressWarnings("unchecked")
  public static <T extends Map<K, V>, K, V> T deepClone(T map) {
    return (T) deepClone(map, LinkedHashMap::new);
  }

  /**
   * 深度克隆Map
   * <p>递归克隆所有嵌套的Map和Collection</p>
   *
   * @param <T>      Map的具体类型
   * @param <K>      键的类型
   * @param <V>      值的类型
   * @param map      要克隆的Map，可以为null
   * @param supplier Map构造函数，不能为null
   * @return 深度克隆的Map
   * @throws NullPointerException 如果supplier为null
   * @example <pre>
   * Map&lt;String, Object&gt; original = new HashMap&lt;&gt;();
   * original.put("nested", Map.of("key", "value"));
   *
   * Map&lt;String, Object&gt; cloned = Maps.deepClone(original, HashMap::new);
   * // cloned是original的深度拷贝，修改cloned不会影响original
   * </pre>
   */
  @SuppressWarnings("unchecked")
  public static <T extends Map<K, V>, K, V> T deepClone(T map, Supplier<T> supplier) {
    Objects.requireNonNull(supplier, "supplier cannot be null");

    if (map == null) {
      return supplier.get();
    }

    T target = supplier.get();
    map.forEach((key, value) -> {
      if (value == null) {
        target.put(key, null);
      } else if (value instanceof Map<?, ?>) {
        // 修复类型转换问题
        try {
          @SuppressWarnings("unchecked")
          Map<K, V> nestedMap = (Map<K, V>) value;
          target.put(key, (V) deepClone(nestedMap, LinkedHashMap::new));
        } catch (ClassCastException e) {
          // 如果类型转换失败，进行浅拷贝
          target.put(key, value);
        }
      } else if (value instanceof Collection<?> collection) {
        target.put(key, (V) Lists.deepClone(collection));
      } else {
        target.put(key, value);
      }
    });

    return target;
  }

  /**
   * 深度克隆为不可变Map（暂时实现为只读包装）
   * <p>TODO: 未来将实现递归克隆成不可变map</p>
   *
   * @param <K> 键的类型
   * @param <V> 值的类型
   * @param map 要克隆的Map
   * @return 不可变的Map
   */
  public static <K, V> Map<K, V> deepCloneUnmodifiable(Map<K, V> map) {
    // TODO: 此处暂时先这样，有空实现，递归克隆成不可变map
    return Collections.unmodifiableMap(map != null ? map : Collections.emptyMap());
  }

  /**
   * 提取 Map 中的符合条件的键值对，返回新Map
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       源Map，不能为null
   * @param predicate 筛选条件，接收键和值作为参数，不能为null
   * @param supplier  Map构造函数，不能为null
   * @return 包含符合条件键值对的新Map
   * @throws NullPointerException 如果任何参数为null
   */
  public static <K, V> Map<K, V> pick(Map<K, V> map, BiPredicate<K, V> predicate, Supplier<Map<K, V>> supplier) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(predicate, "predicate cannot be null");
    Objects.requireNonNull(supplier, "supplier cannot be null");

    return map.entrySet().stream()
        .filter(entry -> predicate.test(entry.getKey(), entry.getValue()))
        .collect(supplier, (m, t) -> m.put(t.getKey(), t.getValue()), Map::putAll);
  }

  /**
   * 提取 Map 中的符合条件的键值对，返回新Map（默认使用LinkedHashMap）
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       源Map，不能为null
   * @param predicate 筛选条件，接收键和值作为参数，不能为null
   * @return 包含符合条件键值对的新Map（LinkedHashMap实现）
   * @throws NullPointerException 如果map或predicate为null
   * @example <pre>
   * Map&lt;String, Integer&gt; map = Map.of("a", 1, "b", 2, "c", 3);
   * Map&lt;String, Integer&gt; result = Maps.pick(map, (k, v) -&gt; v &gt; 1);
   * // 结果: {b=2, c=3}
   * </pre>
   */
  public static <K, V> Map<K, V> pick(Map<K, V> map, BiPredicate<K, V> predicate) {
    return pick(map, predicate, LinkedHashMap::new);
  }

  /**
   * 提取 Map 符合条件的键值对，返回新Map
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       源Map，不能为null
   * @param predicate 键的筛选条件，不能为null
   * @param supplier  Map构造函数，不能为null
   * @return 包含符合条件键值对的新Map
   * @throws NullPointerException 如果任何参数为null
   */
  public static <K, V> Map<K, V> pick(Map<K, V> map, Predicate<K> predicate, Supplier<Map<K, V>> supplier) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(predicate, "predicate cannot be null");
    Objects.requireNonNull(supplier, "supplier cannot be null");

    return map.entrySet().stream()
        .filter(entry -> predicate.test(entry.getKey()))
        .collect(supplier, (m, t) -> m.put(t.getKey(), t.getValue()), Map::putAll);
  }

  /**
   * 提取 Map 符合条件的键值对，返回新Map（默认使用LinkedHashMap）
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       源Map，不能为null
   * @param predicate 键的筛选条件，不能为null
   * @return 包含符合条件键值对的新Map（LinkedHashMap实现）
   * @throws NullPointerException 如果map或predicate为null
   * @example <pre>
   * Map&lt;String, Integer&gt; map = Map.of("apple", 1, "banana", 2, "apricot", 3);
   * Map&lt;String, Integer&gt; result = Maps.pick(map, key -&gt; key.startsWith("ap"));
   * // 结果: {apple=1, apricot=3}
   * </pre>
   */
  public static <K, V> Map<K, V> pick(Map<K, V> map, Predicate<K> predicate) {
    return pick(map, predicate, LinkedHashMap::new);
  }

  /**
   * 提取 Map 指定Key的 Entry，返回新Map
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  源Map，不能为null
   * @param keys 要提取的键集合，不能为null
   * @return 包含指定键的新Map
   * @throws NullPointerException 如果map或keys为null
   * @example <pre>
   * Map&lt;String, Integer&gt; map = Map.of("a", 1, "b", 2, "c", 3, "d", 4);
   * Map&lt;String, Integer&gt; result = Maps.pick(map, Arrays.asList("a", "c"));
   * // 结果: {a=1, c=3}
   * </pre>
   */
  public static <K, V> Map<K, V> pick(Map<K, V> map, Collection<K> keys) {
    Objects.requireNonNull(keys, "keys cannot be null");
    return pick(map, keys::contains);
  }

  /**
   * 提取 Map 指定Key的 Entry，返回新Map
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  源Map，不能为null
   * @param keys 要提取的键数组
   * @return 包含指定键的新Map
   * @throws NullPointerException 如果map为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> pick(Map<K, V> map, K... keys) {
    if (keys == null || keys.length == 0) {
      return new LinkedHashMap<>();
    }
    return pick(map, new HashSet<>(Arrays.asList(keys)));
  }

  /**
   * 剔除 Map 符合条件的键值对，返回新Map
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       源Map，不能为null
   * @param predicate 键的筛选条件（要剔除的条件），不能为null
   * @return 剔除指定条件后的新Map
   * @throws NullPointerException 如果map或predicate为null
   * @example <pre>
   * Map&lt;String, Integer&gt; map = Map.of("a", 1, "b", 2, "c", 3);
   * Map&lt;String, Integer&gt; result = Maps.omit(map, key -&gt; key.equals("b"));
   * // 结果: {a=1, c=3}
   * </pre>
   */
  public static <K, V> Map<K, V> omit(Map<K, V> map, Predicate<K> predicate) {
    Objects.requireNonNull(predicate, "predicate cannot be null");
    return pick(map, predicate.negate());
  }

  /**
   * 剔除 Map 指定Key的 Entry，返回新Map
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  源Map，不能为null
   * @param keys 要剔除的键集合，不能为null
   * @return 剔除指定键后的新Map
   * @throws NullPointerException 如果map或keys为null
   */
  public static <K, V> Map<K, V> omit(Map<K, V> map, Collection<K> keys) {
    Objects.requireNonNull(keys, "keys cannot be null");
    return omit(map, keys::contains);
  }

  /**
   * 剔除 Map 指定Key的 Entry，返回新Map
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  源Map，不能为null
   * @param keys 要剔除的键数组
   * @return 剔除指定键后的新Map
   * @throws NullPointerException 如果map为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> omit(Map<K, V> map, K... keys) {
    if (keys == null || keys.length == 0) {
      return new LinkedHashMap<>(map);
    }
    return omit(map, Arrays.asList(keys));
  }

  /**
   * 剔除 Map 符合条件的键值对，返回新Map
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       源Map，不能为null
   * @param predicate 筛选条件，接收键和值作为参数（要剔除的条件），不能为null
   * @return 剔除指定条件后的新Map
   * @throws NullPointerException 如果map或predicate为null
   */
  public static <K, V> Map<K, V> omit(Map<K, V> map, BiPredicate<K, V> predicate) {
    Objects.requireNonNull(predicate, "predicate cannot be null");
    return pick(map, predicate.negate());
  }

  /**
   * 删除 Map 中不符合条件的键值对，返回原Map
   * <p>注意：这个方法会修改原Map</p>
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       要修改的Map，不能为null
   * @param predicate 保留条件，接收键和值作为参数，不能为null
   * @return 原Map（已修改）
   * @throws NullPointerException 如果map或predicate为null
   */
  public static <K, V> Map<K, V> removeExcept(Map<K, V> map, BiPredicate<K, V> predicate) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(predicate, "predicate cannot be null");

    map.entrySet().removeIf(entry -> !predicate.test(entry.getKey(), entry.getValue()));
    return map;
  }

  /**
   * 删除 Map 中不符合条件的键值对，返回原Map
   * <p>注意：这个方法会修改原Map</p>
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       要修改的Map，不能为null
   * @param predicate 保留条件，接收键作为参数，不能为null
   * @return 原Map（已修改）
   * @throws NullPointerException 如果map或predicate为null
   */
  public static <K, V> Map<K, V> removeExcept(Map<K, V> map, Predicate<K> predicate) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(predicate, "predicate cannot be null");

    map.entrySet().removeIf(entry -> !predicate.test(entry.getKey()));
    return map;
  }

  /**
   * 删除 Map 中指定Key的 Entry，返回原Map
   * <p>注意：这个方法会修改原Map</p>
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  要修改的Map，不能为null
   * @param keys 要保留的键集合，不能为null
   * @return 原Map（已修改）
   * @throws NullPointerException 如果map或keys为null
   */
  public static <K, V> Map<K, V> removeExcept(Map<K, V> map, Collection<K> keys) {
    Objects.requireNonNull(keys, "keys cannot be null");
    var keySet = new HashSet<>(keys);
    return removeExcept(map, keySet::contains);
  }

  /**
   * 删除 Map 中指定Key的 Entry，返回原Map
   * <p>注意：这个方法会修改原Map</p>
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  要修改的Map，不能为null
   * @param keys 要保留的键数组
   * @return 原Map（已修改）
   * @throws NullPointerException 如果map为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> removeExcept(Map<K, V> map, K... keys) {
    if (keys == null || keys.length == 0) {
      map.clear();
      return map;
    }
    return removeExcept(map, new HashSet<>(Arrays.asList(keys)));
  }

  /**
   * 删除 Map 中符合条件的键值对，返回原Map
   * <p>注意：这个方法会修改原Map</p>
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       要修改的Map，不能为null
   * @param predicate 删除条件，接收键和值作为参数，不能为null
   * @return 原Map（已修改）
   * @throws NullPointerException 如果map或predicate为null
   */
  public static <K, V> Map<K, V> remove(Map<K, V> map, BiPredicate<K, V> predicate) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(predicate, "predicate cannot be null");

    map.entrySet().removeIf(entry -> predicate.test(entry.getKey(), entry.getValue()));
    return map;
  }

  /**
   * 删除 Map 中符合条件的键值对，返回原Map
   * <p>注意：这个方法会修改原Map</p>
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       要修改的Map，不能为null
   * @param predicate 删除条件，接收键作为参数，不能为null
   * @return 原Map（已修改）
   * @throws NullPointerException 如果map或predicate为null
   */
  public static <K, V> Map<K, V> remove(Map<K, V> map, Predicate<K> predicate) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(predicate, "predicate cannot be null");

    map.entrySet().removeIf(entry -> predicate.test(entry.getKey()));
    return map;
  }

  /**
   * 删除 Map 中指定Key的 Entry，返回原Map
   * <p>注意：这个方法会修改原Map</p>
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  要修改的Map，不能为null
   * @param keys 要删除的键集合，不能为null
   * @return 原Map（已修改）
   * @throws NullPointerException 如果map或keys为null
   */
  public static <K, V> Map<K, V> remove(Map<K, V> map, Collection<K> keys) {
    Objects.requireNonNull(keys, "keys cannot be null");
    var keySet = new HashSet<>(keys);
    return remove(map, keySet::contains);
  }

  /**
   * 删除 Map 中指定Key的 Entry，返回原Map
   * <p>注意：这个方法会修改原Map</p>
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  要修改的Map，不能为null
   * @param keys 要删除的键数组
   * @return 原Map（已修改）
   * @throws NullPointerException 如果map为null
   */
  @SafeVarargs
  public static <K, V> Map<K, V> remove(Map<K, V> map, K... keys) {
    if (keys == null || keys.length == 0) {
      return map;
    }
    return remove(map, new HashSet<>(Arrays.asList(keys)));
  }

  /**
   * 获取并转换类型
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param <R>  目标类型
   * @param map  源Map，可以为null
   * @param key  键，可以为null
   * @param type 目标类型的Class，不能为null
   * @return 如果值存在且类型匹配则返回转换后的值，否则返回Optional.empty()
   * @throws NullPointerException 如果type为null
   * @example <pre>
   * Map&lt;String, Object&gt; map = Map.of("name", "John", "age", 30);
   * Optional&lt;String&gt; name = Maps.getAndCast(map, "name", String.class);
   * Optional&lt;Integer&gt; age = Maps.getAndCast(map, "age", Integer.class);
   * Optional&lt;Long&gt; invalid = Maps.getAndCast(map, "age", Long.class);
   * // name = Optional[John], age = Optional[30], invalid = Optional.empty()
   * </pre>
   */
  public static <K, V> Optional<V> getAs(Map<K, ?> map, K key, Class<V> type) {
    Objects.requireNonNull(type, "type cannot be null");

    if (map == null || key == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(map.get(key))
        .filter(type::isInstance)
        .map(type::cast);
  }

  public static <K, V> V getAsOrDefault(Map<K, ?> map, K key, Class<V> type, V defaultValue) {
    return getAs(map, key, type).orElse(defaultValue);
  }

  /**
   * 批量putIfAbsent操作
   *
   * @param <K>    键的类型
   * @param <V>    值的类型
   * @param target 目标Map，不能为null
   * @param source 源Map，可以为null
   * @return 目标Map（已修改）
   * @throws NullPointerException 如果target为null
   * @example <pre>
   * Map&lt;String, String&gt; target = new HashMap&lt;&gt;();
   * target.put("existing", "old");
   *
   * Map&lt;String, String&gt; source = Map.of("existing", "new", "new", "value");
   * Maps.putIfAbsentAll(target, source);
   * // 结果: target = {existing=old, new=value}
   * </pre>
   */
  public static <K, V> Map<K, V> putIfAbsentAll(Map<K, V> target, Map<K, V> source) {
    Objects.requireNonNull(target, "target map cannot be null");

    if (source != null) {
      source.forEach(target::putIfAbsent);
    }
    return target;
  }

  /**
   * 条件性put操作
   *
   * @param <K>       键的类型
   * @param <V>       值的类型
   * @param map       目标Map，不能为null
   * @param key       键
   * @param value     值
   * @param condition 条件判断，接收值作为参数，不能为null
   * @return 目标Map（已修改）
   * @throws NullPointerException 如果map或condition为null
   * @example <pre>
   * Map&lt;String, String&gt; config = new HashMap&lt;&gt;();
   * Maps.putIf(config, "debug", "true", Objects::nonNull);
   * Maps.putIf(config, "empty", "", s -&gt; !s.isEmpty());
   * // 结果: config = {debug=true}
   * </pre>
   */
  public static <K, V> Map<K, V> putIf(Map<K, V> map, K key, V value, Predicate<V> condition) {
    Objects.requireNonNull(map, "map cannot be null");
    Objects.requireNonNull(condition, "condition cannot be null");

    if (condition.test(value)) {
      map.put(key, value);
    }
    return map;
  }

  /**
   * 同时转换key和value
   *
   * @param <K1>        源键类型
   * @param <V1>        源值类型
   * @param <K2>        目标键类型
   * @param <V2>        目标值类型
   * @param source      源Map，可以为null
   * @param keyMapper   键转换函数，不能为null
   * @param valueMapper 值转换函数，不能为null
   * @return 转换后的新Map
   * @throws NullPointerException 如果keyMapper或valueMapper为null
   * @example <pre>
   * Map&lt;Integer, String&gt; source = Map.of(1, "one", 2, "two");
   * Map&lt;String, Integer&gt; result = Maps.transform(source, Object::toString, String::length);
   * // 结果: {"1": 3, "2": 3}
   * </pre>
   */
  public static <K1, V1, K2, V2> Map<K2, V2> transform(
      Map<K1, V1> source,
      Function<K1, K2> keyMapper,
      Function<V1, V2> valueMapper) {

    Objects.requireNonNull(keyMapper, "keyMapper cannot be null");
    Objects.requireNonNull(valueMapper, "valueMapper cannot be null");

    if (source == null) {
      return new LinkedHashMap<>();
    }

    return source.entrySet().stream()
        .collect(
            LinkedHashMap::new,
            (map, entry) -> map.put(
                keyMapper.apply(entry.getKey()),
                valueMapper.apply(entry.getValue())
            ),
            Map::putAll
        );
  }

  /**
   * Map的交集（相同key且相同value的项）
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map1 第一个Map，可以为null
   * @param map2 第二个Map，可以为null
   * @return 交集Map，如果任一输入为null则返回空Map
   * @example <pre>
   * Map&lt;String, Integer&gt; map1 = Map.of("a", 1, "b", 2, "c", 3);
   * Map&lt;String, Integer&gt; map2 = Map.of("b", 2, "c", 4, "d", 5);
   * Map&lt;String, Integer&gt; result = Maps.intersection(map1, map2);
   * // 结果: {b=2} (只有b的键和值都相同)
   * </pre>
   */
  public static <K, V> Map<K, V> intersection(Map<K, V> map1, Map<K, V> map2) {
    if (map1 == null || map2 == null) {
      return new LinkedHashMap<>();
    }

    return map1.entrySet().stream()
        .filter(entry -> Objects.equals(entry.getValue(), map2.get(entry.getKey())))
        .collect(
            LinkedHashMap::new,
            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
            Map::putAll
        );
  }

  /**
   * Map的差集（在map1中但不在map2中，或值不同的项）
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map1 第一个Map，可以为null
   * @param map2 第二个Map，可以为null
   * @return 差集Map，如果map1为null则返回空Map，如果map2为null则返回map1的拷贝
   * @example <pre>
   * Map&lt;String, Integer&gt; map1 = Map.of("a", 1, "b", 2, "c", 3);
   * Map&lt;String, Integer&gt; map2 = Map.of("b", 2, "c", 4, "d", 5);
   * Map&lt;String, Integer&gt; result = Maps.difference(map1, map2);
   * // 结果: {a=1, c=3} (a不在map2中，c的值不同)
   * </pre>
   */
  public static <K, V> Map<K, V> difference(Map<K, V> map1, Map<K, V> map2) {
    if (map1 == null) {
      return new LinkedHashMap<>();
    }
    if (map2 == null) {
      return new LinkedHashMap<>(map1);
    }

    return map1.entrySet().stream()
        .filter(entry -> !Objects.equals(entry.getValue(), map2.get(entry.getKey())))
        .collect(
            LinkedHashMap::new,
            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
            Map::putAll
        );
  }

  /**
   * Map 转为 Bean
   *
   * @param <T>   目标Bean类型
   * @param clazz 目标Bean的Class，不能为null
   * @param map   源Map，不能为null
   * @return 转换后的Bean实例
   * @throws NullPointerException 如果clazz或map为null
   * @throws RuntimeException     如果转换失败
      * @deprecated 建议使用 {@link #tryToBean(Class, Map)} 以获得更好的错误处理
   */
  @Deprecated
  public static <T> T toBean(Class<T> clazz, Map<String, Object> map) {
    Objects.requireNonNull(clazz, "clazz cannot be null");
    Objects.requireNonNull(map, "map cannot be null");
    return tryToBean(clazz, map).get();
  }

  /**
   * Map 转为 Bean（安全版本）
   *
   * @param <T>   目标Bean类型
   * @param clazz 目标Bean的Class，不能为null
   * @param map   源Map，不能为null
   * @return Try包装的转换结果，成功时包含Bean实例，失败时包含异常
   * @throws NullPointerException 如果clazz或map为null
   * @example <pre>
   * Map&lt;String, Object&gt; map = Map.of("name", "John", "age", 30);
   * Try&lt;User&gt; result = Maps.tryToBean(User.class, map);
   *
   * if (result.isSuccess()) {
   *     User user = result.get();
   *     // 处理成功情况
   * } else {
   *     Throwable error = result.getCause();
   *     // 处理错误情况
   * }
   * </pre>
   */
  public static <T> Try<T> tryToBean(Class<T> clazz, Map<String, Object> map) {
    Objects.requireNonNull(clazz, "clazz cannot be null");
    Objects.requireNonNull(map, "map cannot be null");
    return Try.of(() -> {
      T bean = instantiate(clazz);
      for (BeanProperty property : BEAN_PROPERTIES.get(clazz)) {
        if (!property.canWrite() || !property.hasValue(map)) {
          continue;
        }
        property.write(bean, convertMapValue(property.getWriteType(), property.readValue(map)));
      }
      return bean;
    });
  }

  /**
   * Bean 转为 Map
   *
   * @param bean 源Bean对象，不能为null
   * @return 转换后的Map（LinkedHashMap实现）
   * @throws NullPointerException 如果bean为null
   * @example <pre>
   * User user = new User("John", 30);
   * Map&lt;String, Object&gt; map = Maps.fromBean(user);
   * // 结果: {"name": "John", "age": 30}
   * </pre>
   */
  public static Map<String, Object> fromBean(Object bean) {
    Objects.requireNonNull(bean, "bean cannot be null");
    return fromBean(bean, newIdentityPath());
  }

  private static Map<String, Object> fromBean(Object bean, Set<Object> visiting) {
    if (!visiting.add(bean)) {
      return null;
    }

    Map<String, Object> map = new LinkedHashMap<>();
    try {
      for (BeanProperty property : BEAN_PROPERTIES.get(bean.getClass())) {
        if (!property.canRead()) {
          continue;
        }
        map.put(property.getName(), convertBeanValue(property.getReadType(), property.read(bean), visiting));
      }
      return map;
    }
    catch (Exception e) {
      return Try.<Map<String, Object>>failure(e).get();
    }
    finally {
      visiting.remove(bean);
    }
  }

  /**
   * 获取第一个非null值（支持多个候选key）
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  源Map，不能为null
   * @param keys 候选键数组
   * @return 第一个非null的值，如果都不存在或为null则返回Optional.empty()
   * @throws NullPointerException 如果map为null
   */
  @SafeVarargs
  @Deprecated
  public static <K, V> Optional<V> firstNonNull(Map<K, V> map, K... keys) {
    for (K key : keys) {
      V value = map.get(key);
      if (value != null) {
        return Optional.of(value);
      }
    }
    return Optional.empty();
  }

  /**
   * 获取第一个映射的值（包括null值）
   *
   * @param <K>  键的类型
   * @param <V>  值的类型
   * @param map  源Map，不能为null
   * @param keys 候选键数组，按优先级顺序
   * @return 第一个存在映射的值（可能为null），如果都不存在则返回null
   * @throws NullPointerException 如果map为null
   * @example <pre>
   * Map&lt;String, String&gt; map = new HashMap&lt;&gt;();
   * map.put("key1", null);
   * map.put("key2", "value");
   *
   * String result = Maps.firstMapped(map, "key1", "key2", "key3");
   * // 结果: null (key1存在但值为null)
   * </pre>
   */
  @SafeVarargs
  public static <K, V> V firstMapped(Map<K, V> map, K... keys) {
    Objects.requireNonNull(map, "map cannot be null");

    if (keys == null) {
      return null;
    }

    for (K k : keys) {
      if (map.containsKey(k)) {
        return map.get(k);
      }
    }
    return null;
  }

  /**
   * 层级化 Map 操作类
   * <p>层级化 Map 即 value 可以是 Map 也可以是 List，支持通过路径访问深层数据</p>
   *
   */
  public static class Hierarchical {

    /**
     * 预编译的路径分割正则表达式，避免重复编译
     */
    private static final Pattern PATH_PATTERN = Pattern.compile("\\[?([\\w\\-!$><=~]+|\\d+)]?");

    /**
     * 默认路径分割函数
     * <p>支持以下格式的路径：</p>
     * <ul>
     *   <li>user.name - 对象属性访问</li>
     *   <li>items[0] - 数组/列表索引访问</li>
     *   <li>user.items[0].name - 混合访问</li>
     * </ul>
     *
     * @param path 路径字符串，不能为null
     * @return 分割后的路径组件列表
       */
    public static List<String> defaultPathSplitter(String path) {
      if (path == null) {
        return Collections.emptyList();
      }

      var matcher = PATH_PATTERN.matcher(path);
      var pos = 0;
      var list = new ArrayList<String>();
      while (matcher.find(pos)) {
        list.add(matcher.group(1));
        pos = matcher.end();
      }
      return list;
    }

    /**
     * 构建嵌套Map
     * <p>将扁平化的Map转换为嵌套结构，支持点号和数组索引语法</p>
     *
     * @param flattenMap 构建源Map，代表深层值的key格式为 "keyA.keyA1" 或者 "keyB[0].keyA2"，可以为null
     * @return 嵌套的Map（LinkedHashMap实现），如果输入为null则返回空Map
     * @example <pre>
     * Map&lt;String, Object&gt; flatMap = Map.of(
     *     "user.name", "John",
     *     "user.age", 30,
     *     "user.tags[0]", "developer",
     *     "user.tags[1]", "java"
     * );
     *
     * Map&lt;String, Object&gt; nested = Maps.Hierarchical.build(flatMap);
     * // 结果: {user={name=John, age=30, tags=[developer, java]}}
     * </pre>
       */
    public static Map<String, Object> build(Map<String, Object> flattenMap) {
      return build(flattenMap, LinkedHashMap::new, Hierarchical::defaultPathSplitter);
    }

    /**
     * 构建嵌套Map（支持自定义Map实现）
     *
     * @param flattenMap  构建源Map，代表深层值的key格式为 "keyA.keyA1" 或者 "keyB[0].keyA2"，可以为null
     * @param mapSupplier 自定义Map的构造函数，不能为null
     * @return 嵌套的Map，如果输入为null则返回空Map
     * @throws NullPointerException 如果mapSupplier为null
       */
    public static Map<String, Object> build(Map<String, Object> flattenMap, Supplier<Map<String, Object>> mapSupplier) {
      return build(flattenMap, mapSupplier, Hierarchical::defaultPathSplitter);
    }

    /**
     * 构建嵌套Map（支持自定义路径分割器）
     *
     * @param flattenMap   构建源Map，可以为null
     * @param pathSplitter 自定义Key的Path分割函数，不能为null
     * @return 嵌套的Map（LinkedHashMap实现），如果输入为null则返回空Map
     * @throws NullPointerException 如果pathSplitter为null
       */
    public static Map<String, Object> build(Map<String, Object> flattenMap, Function<String, List<String>> pathSplitter) {
      return build(flattenMap, LinkedHashMap::new, pathSplitter);
    }

    /**
     * 构建嵌套Map（完全自定义版本）
     *
     * @param flattenMap   构建源Map，可以为null
     * @param mapSupplier  自定义Map的构造函数，不能为null
     * @param pathSplitter 自定义Key的Path分割函数，不能为null
     * @return 嵌套的Map，如果输入为null则返回空Map
     * @throws NullPointerException 如果mapSupplier或pathSplitter为null
       */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map<String, Object> build(
        Map<String, Object> flattenMap,
        Supplier<Map<String, Object>> mapSupplier,
        Function<String, List<String>> pathSplitter) {

      Objects.requireNonNull(mapSupplier, "mapSupplier cannot be null");
      Objects.requireNonNull(pathSplitter, "pathSplitter cannot be null");

      if (flattenMap == null) {
        return mapSupplier.get();
      }

      var map = mapSupplier.get();
      flattenMap.forEach((key, value) -> {
        var pathParts = pathSplitter.apply(key);

        if (value instanceof Map valueMap) {
          value = deepClone(valueMap, LinkedHashMap::new);

          // 修复逻辑错误：检查是否已存在值需要合并
          var oldValue = getValue(map, pathParts);
          if (oldValue.isPresent() && oldValue.get() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existingMap = (Map<String, Object>) oldValue.get();
            if (value instanceof Map) {
              // 正确的合并方向：将新值合并到已存在的Map中
              existingMap.putAll((Map<String, Object>) value);
              return;
            }
          }
        } else if (value instanceof Collection<?> coll) {
          value = Lists.deepClone(coll);
        }
        setValue(map, pathParts, value);
      });
      return map;
    }

    /**
     * 将嵌套的Map扁平化
     * <p>将多层嵌套的Map和List结构转换为扁平的Map，使用点号和数组索引表示路径</p>
     *
     * @param hierarchicalMap 嵌套形式的Map，可以为null
     * @return 扁平化的Map（LinkedHashMap实现），如果输入为null则返回空Map
     * @example <pre>
     * Map&lt;String, Object&gt; nested = Map.of(
     *     "user", Map.of(
     *         "name", "John",
     *         "tags", List.of("developer", "java")
     *     )
     * );
     *
     * Map&lt;String, Object&gt; flat = Maps.Hierarchical.flatten(nested);
     * // 结果: {user.name=John, user.tags[0]=developer, user.tags[1]=java}
     * </pre>
       */
    public static Map<String, Object> flatten(Map<String, Object> hierarchicalMap) {
      if (hierarchicalMap == null) {
        return new LinkedHashMap<>();
      }

      return hierarchicalMap.entrySet().stream()
          .flatMap(entry -> {
            var key = entry.getKey();
            var value = entry.getValue();
            return flatItem(key, value);
          }).collect(LinkedHashMap::new, (map, tuple) -> map.put(tuple._1(), tuple._2()), LinkedHashMap::putAll);
    }


    /**
     * 将嵌套的Map扁平化（仅展开Map，保留List不变）
     * <p>将多层嵌套的Map结构转换为扁平的Map，但保持List作为完整的值不进行展开</p>
     *
     * @param hierarchicalMap 嵌套形式的Map，可以为null
     * @return 扁平化的Map（LinkedHashMap实现），其中List保持完整，如果输入为null则返回空Map
     * @example <pre>
     * Map&lt;String, Object&gt; nested = Map.of(
     *     "user", Map.of(
     *         "name", "John",
     *         "tags", List.of("developer", "java"),
     *         "profile", Map.of(
     *             "email", "john@example.com",
     *             "hobbies", List.of("coding", "reading")
     *         )
     *     )
     * );
     *
     * Map&lt;String, Object&gt; flat = Maps.Hierarchical.flattenMapsOnly(nested);
     * // 结果: {
     * //   user.name=John,
     * //   user.tags=[developer, java],
     * //   user.profile.email=john@example.com,
     * //   user.profile.hobbies=[coding, reading]
     * // }
     * </pre>
       */
    public static Map<String, Object> flattenMapsOnly(Map<String, Object> hierarchicalMap) {
      if (hierarchicalMap == null) {
        return new LinkedHashMap<>();
      }

      return hierarchicalMap.entrySet().stream()
          .flatMap(entry -> {
            var key = entry.getKey();
            var value = entry.getValue();
            return flatItemMapsOnly(key, value);
          }).collect(LinkedHashMap::new, (map, tuple) -> map.put(tuple._1(), tuple._2()), LinkedHashMap::putAll);
    }

    /**
     * 设置嵌套Map的值（使用自定义路径分割器）
     *
     * @param map          嵌套Map，不能为null
     * @param path         路径，不能为null
     * @param pathSplitter 自定义Key的Path分割函数，不能为null
     * @param value        值
     * @throws NullPointerException 如果map、path或pathSplitter为null
       */
    public static void setValue(Map<String, Object> map, String path, Function<String, List<String>> pathSplitter, Object value) {
      Objects.requireNonNull(map, "map cannot be null");
      Objects.requireNonNull(path, "path cannot be null");
      Objects.requireNonNull(pathSplitter, "pathSplitter cannot be null");

      var pathParts = pathSplitter.apply(path);
      setValue(map, pathParts, value);
    }

    /**
     * 设置嵌套Map的值
     *
     * @param map   嵌套Map，不能为null
     * @param path  路径，不能为null
     * @param value 值
     * @throws NullPointerException 如果map或path为null
     * @example <pre>
     * Map&lt;String, Object&gt; map = new LinkedHashMap&lt;&gt;();
     * Maps.Hierarchical.setValue(map, "user.profile.name", "John");
     * Maps.Hierarchical.setValue(map, "user.tags[0]", "developer");
     * // 结果: {user={profile={name=John}, tags=[developer]}}
     * </pre>
       */
    public static void setValue(Map<String, Object> map, String path, Object value) {
      setValue(map, path, Hierarchical::defaultPathSplitter, value);
    }

    /**
     * 设置嵌套Map的值（使用路径组件列表）
     *
     * @param map   嵌套Map，不能为null
     * @param paths 路径组件列表，不能为null
     * @param value 值
     * @throws NullPointerException 如果map或paths为null
       */
    @SuppressWarnings("unchecked")
    public static void setValue(Map<String, Object> map, List<String> paths, Object value) {
      Objects.requireNonNull(map, "map cannot be null");
      Objects.requireNonNull(paths, "paths cannot be null");

      if (paths.isEmpty()) {
        return;
      }

      AtomicReference<Either<Map<String, Object>, List<Object>>> currentContainer = new AtomicReference<>(Either.left(map));
      for (var i = 0; i < paths.size(); i++) {
        var currentPath = paths.get(i);
        if (i == paths.size() - 1) {
          var __ = currentPath.matches("\\d+")
              ? currentContainer.get().peek(lst -> Lists.setValue(lst, Integer.parseInt(currentPath), value))
              : currentContainer.get().peekLeft((Map<String, Object> m) -> m.put(currentPath, value));
          return;
        }

        // 确保下级容器存在，不存在则创建
        var next = paths.get(i + 1);
        Tuple3<Supplier<Object>, Predicate<Object>, Consumer<Object>> fnTuple =
            next.matches("\\d+")
                // 如果下一个容器是List，则把函数组设为针对List的函数
                ? Tuple.of(
                ArrayList::new,
                instanceOf(List.class),
                obj -> currentContainer.set(Either.right((List<Object>) obj))
            )
                // 否则，函数组设为针对Map的函数
                : Tuple.of(
                LinkedHashMap::new,
                instanceOf(Map.class),
                obj -> currentContainer.set(Either.left((Map<String, Object>) obj))
            );

        currentContainer.get()
            .peekLeft(m -> {
              var container = m.get(currentPath);
              if (fnTuple._2().negate().test(container)) {
                container = fnTuple._1().get();
                m.put(currentPath, container);
              }
              fnTuple._3().accept(container);
            })
            .peek(list -> {
              var index = Integer.parseInt(currentPath);
              var container = list.size() > index ? list.get(index) : null;
              if (fnTuple._2().negate().test(container)) {
                container = fnTuple._1().get();
                Lists.setValue(list, index, container);
              }
              fnTuple._3().accept(container);
            });
      }
    }

    /**
     * 获取嵌套Map的值（使用自定义路径分割器）
     *
     * @param map          嵌套Map，可以为null
     * @param path         路径，可以为null
     * @param pathSplitter 自定义Key的Path分割函数，不能为null
     * @return 值的Optional包装，如果路径不存在则返回Optional.empty()
     * @throws NullPointerException 如果pathSplitter为null
       */
    public static Optional<Object> getValue(Map<String, Object> map, String path, Function<String, List<String>> pathSplitter) {
      Objects.requireNonNull(pathSplitter, "pathSplitter cannot be null");

      if (map == null || path == null) {
        return Optional.empty();
      }

      var pathParts = pathSplitter.apply(path);
      return getValue(map, pathParts);
    }

    /**
     * 获取嵌套Map的值
     *
     * @param map  嵌套Map，可以为null
     * @param path 路径，可以为null
     * @return 值的Optional包装，如果路径不存在则返回Optional.empty()
     * @example <pre>
     * Map&lt;String, Object&gt; map = Map.of(
     *     "user", Map.of("name", "John", "tags", List.of("dev", "java"))
     * );
     *
     * Optional&lt;Object&gt; name = Maps.Hierarchical.getValue(map, "user.name");
     * Optional&lt;Object&gt; tag = Maps.Hierarchical.getValue(map, "user.tags[0]");
     * // name = Optional[John], tag = Optional[dev]
     * </pre>
       */
    public static Optional<Object> getValue(Map<String, Object> map, String path) {
      return getValue(map, path, Hierarchical::defaultPathSplitter);
    }

    /**
     * 获取嵌套Map的值（使用路径组件列表）
     *
     * @param map   嵌套Map，可以为null
     * @param paths 路径组件列表，可以为null
     * @return 值的Optional包装，如果路径不存在则返回Optional.empty()
       */
    @SuppressWarnings("unchecked")
    public static Optional<Object> getValue(Map<String, Object> map, List<String> paths) {
      if (map == null || paths == null || paths.isEmpty()) {
        return Optional.empty();
      }

      AtomicReference<Either<Map<String, Object>, List<Object>>> currentContainer = new AtomicReference<>(Either.left(map));
      var lastIndex = paths.size() - 1;
      for (var i = 0; i < paths.size(); i++) {
        var currentPath = paths.get(i);
        var nextNode = currentContainer.get()
            .fold(m -> m.get(currentPath), lst -> {
              if (!currentPath.matches("\\d+")) {
                return null;
              }
              var index = Integer.parseInt(currentPath);
              return lst.size() > index ? lst.get(index) : null;
            });
        if (nextNode == null) {
          return Optional.empty();
        }
        else if (i == lastIndex) {
          return Optional.of(nextNode);
        }
        // 切换下级容器，若不存在则直接返回空
        else if (nextNode instanceof Map<?, ?>) {
          currentContainer.set(Either.left((Map<String, Object>) nextNode));
        }
        else if (nextNode instanceof List<?>) {
          currentContainer.set(Either.right((List<Object>) nextNode));
        }
        else {
          return Optional.empty();
        }
      }
      return Optional.empty();
    }

    /**
     * 扁平化递归函数
     */
    private static Stream<Tuple2<String, Object>> flatItem(String key, Object value) {
      if (value instanceof Map<?, ?> mapValue) {
        @SuppressWarnings("unchecked")
        Map<String, Object> stringObjectMap = (Map<String, Object>) mapValue;
        return flatten(stringObjectMap)
            .entrySet()
            .stream()
            .map((e) -> Tuple.of(key + "." + e.getKey(), e.getValue()));
      }
      if (value instanceof List<?> listValue) {
        List<Stream<Tuple2<String, Object>>> allStream = new ArrayList<>(listValue.size());
        for (int i = 0; i < listValue.size(); i++) {
          var indexStr = key + "[" + i + "]";
          allStream.add(flatItem(indexStr, listValue.get(i)));
        }
        return allStream.stream().flatMap(Function.identity());
      }
      return Stream.of(Tuple.of(key, value));
    }

    /**
     * 扁平化递归函数（仅处理Map，保留List）
     */
    private static Stream<Tuple2<String, Object>> flatItemMapsOnly(String key, Object value) {
      if (value instanceof Map<?, ?> mapValue) {
        @SuppressWarnings("unchecked")
        Map<String, Object> stringObjectMap = (Map<String, Object>) mapValue;
        return flattenMapsOnly(stringObjectMap)
            .entrySet()
            .stream()
            .map((e) -> Tuple.of(key + "." + e.getKey(), e.getValue()));
      }
      // 对于List和其他类型，直接返回不进行展开
      return Stream.of(Tuple.of(key, value));
    }
  }


  /**
   * 嵌套Map操作类（兼容性保留）
   * <p>继承自Hierarchical类，保持与旧代码的兼容性</p>
   *
      * @deprecated 建议使用 {@link Hierarchical}、{@link MutableHierarchicalMap} 或 {@link ImmutableHierarchicalMap}
   */
  @Deprecated
  public static final class Nested extends Hierarchical {

    /**
     * 私有构造函数，防止实例化
     */
    private Nested() {
      super();
    }

    // 所有方法都继承自Hierarchical，无需重新实现
  }

  /**
   * Map构建器
   * <p>提供流式API来构建Map</p>
   *
   * @param <K> 键的类型
   * @param <V> 值的类型
   */
  public static class MapBuilder<K, V> {
    private final Map<K, V> map;

    /**
     * 构造函数
     *
     * @param map 底层Map实现，不能为null
     * @throws NullPointerException 如果map为null
     */
    public MapBuilder(Map<K, V> map) {
      this.map = Objects.requireNonNull(map, "map cannot be null");
    }

    /**
     * 添加键值对
     *
     * @param key   键
     * @param value 值
     * @return 当前构建器实例（支持链式调用）
     */
    public MapBuilder<K, V> put(K key, V value) {
      map.put(key, value);
      return this;
    }

    /**
     * 添加另一个Map的所有键值对
     *
     * @param map 要添加的Map，可以为null
     * @return 当前构建器实例（支持链式调用）
     */
    public MapBuilder<K, V> putAll(Map<K, V> map) {
      if (map != null) {
        this.map.putAll(map);
      }
      return this;
    }

    /**
     * 构建最终的Map
     *
     * @return 构建的Map
     */
    public Map<K, V> build() {
      return map;
    }
  }

  /**
   * 创建Map构建器（支持自定义Map实现）
   *
   * @param <K>         键的类型
   * @param <V>         值的类型
   * @param mapSupplier Map构造函数，不能为null
   * @return Map构建器实例
   * @throws NullPointerException 如果mapSupplier为null
   * @example <pre>
   * Map&lt;String, Integer&gt; map = Maps.builder(HashMap::new)
   *     .put("a", 1)
   *     .put("b", 2)
   *     .putAll(Map.of("c", 3))
   *     .build();
   * </pre>
   */
  public static <K, V> MapBuilder<K, V> builder(Supplier<Map<K, V>> mapSupplier) {
    Objects.requireNonNull(mapSupplier, "mapSupplier cannot be null");
    var map = mapSupplier.get();
    return new MapBuilder<>(map);
  }

  /**
   * 创建Map构建器（默认使用LinkedHashMap）
   *
   * @param <K> 键的类型
   * @param <V> 值的类型
   * @return Map构建器实例
   */
  public static <K, V> MapBuilder<K, V> builder() {
    return builder(LinkedHashMap::new);
  }

  /**
   * 创建可修改的层级化Map门面类
   *
   * @param map 要包装的Map，不能为null
   * @return 可修改的层级化Map门面类
   * @throws NullPointerException 如果map为null
   * @example <pre>
   * Map&lt;String, Object&gt; map = new HashMap&lt;&gt;();
   * MutableHierarchicalMap hierarchical = Maps.hierarchical(map);
   * hierarchical.setValue("user.profile.name", "John");
   * </pre>
   */
  public static MutableHierarchicalMap hierarchical(Map<String, Object> map) {
    return new MutableHierarchicalMap(map);
  }

  /**
   * 创建不可修改的层级化Map门面类
   *
   * @param map 要包装的Map，不能为null
   * @return 不可修改的层级化Map门面类
   * @throws NullPointerException 如果map为null
   * @example <pre>
   * Map&lt;String, Object&gt; map = Map.of("user", Map.of("name", "John"));
   * ImmutableHierarchicalMap hierarchical = Maps.immutableHierarchical(map);
   * Optional&lt;Object&gt; name = hierarchical.getValue("user.name");
   * </pre>
   */
  public static ImmutableHierarchicalMap immutableHierarchical(Map<String, Object> map) {
    return new ImmutableHierarchicalMap(map);
  }

  /**
   * 创建空的可修改层级化Map门面类
   *
   * @return 包装空LinkedHashMap的可修改门面类
   */
  public static MutableHierarchicalMap newHierarchical() {
    return new MutableHierarchicalMap(new LinkedHashMap<>());
  }

  private static <T> T instantiate(Class<T> clazz) throws Exception {
    return clazz.cast(CONSTRUCTORS.get(clazz).newInstance());
  }

  private static Object convertMapValue(Type targetType, Object value) {
    if (value == null) {
      return null;
    }

    Class<?> targetClass = Types.rawClass(targetType);
    if (targetClass == Object.class) {
      return value;
    }
    MapDeserializer deserializer = MAP_DESERIALIZERS.stream()
        .filter(s -> s.support(targetType))
        .findFirst()
        .orElse(null);
    if (deserializer != null) {
      return deserializer.deserialize(targetType, value);
    }

    if (value instanceof Collection<?> collection && Collection.class.isAssignableFrom(targetClass)) {
      if (!(targetType instanceof ParameterizedType)) {
        return value;
      }
      return convertCollection(targetType, targetClass, collection);
    }
    if (value instanceof Map<?, ?> valueMap) {
      if (Map.class.isAssignableFrom(targetClass)) {
        if (!(targetType instanceof ParameterizedType)) {
          return value;
        }
        return convertMap(targetType, valueMap);
      }
      if (isBeanClass(targetClass)) {
        return toBean(targetClass, toStringObjectMap(valueMap));
      }
    }

    if (isAssignableValue(targetClass, value)) {
      return value;
    }

    return convertSimpleValue(targetClass, value);
  }

  private static Map<?, ?> convertMap(Type targetType, Map<?, ?> map) {
    Type keyType = Object.class;
    Type valueType = Object.class;
    if (targetType instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length >= 2) {
      keyType = parameterizedType.getActualTypeArguments()[0];
      valueType = parameterizedType.getActualTypeArguments()[1];
    }

    Type finalKeyType = keyType;
    Type finalValueType = valueType;
    return map.entrySet().stream()
        .map(entry -> Tuple.of(convertMapValue(finalKeyType, entry.getKey()), convertMapValue(finalValueType, entry.getValue())))
        .collect(LinkedHashMap::new, (m, t) -> m.put(t._1(), t._2()), LinkedHashMap::putAll);
  }

  private static Collection<?> convertCollection(Type targetType, Class<?> targetClass, Collection<?> collection) {
    Type itemType = Object.class;
    if (targetType instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length > 0) {
      itemType = parameterizedType.getActualTypeArguments()[0];
    }

    Type finalItemType = itemType;
    List<Object> converted = collection.stream()
        .map(item -> convertMapValue(finalItemType, item))
        .collect(Collectors.toList());

    if (Set.class.isAssignableFrom(targetClass)) {
      return new LinkedHashSet<>(converted);
    }
    return converted;
  }

  private static Object convertSimpleValue(Class<?> targetClass, Object value) {
    Class<?> wrappedTarget = wrapPrimitive(targetClass);
    if (wrappedTarget == String.class) {
      return Converter.toString(value);
    }
    if (wrappedTarget == Byte.class) {
      return Converter.toByte(value);
    }
    if (wrappedTarget == Short.class) {
      return Converter.toShort(value);
    }
    if (wrappedTarget == Integer.class) {
      return Converter.toInteger(value);
    }
    if (wrappedTarget == Long.class) {
      return Converter.toLong(value);
    }
    if (wrappedTarget == Float.class) {
      return Converter.toFloat(value);
    }
    if (wrappedTarget == Double.class) {
      return Converter.toDouble(value);
    }
    if (wrappedTarget == Boolean.class) {
      return Converter.toBoolean(value);
    }
    if (wrappedTarget == java.math.BigInteger.class) {
      return Converter.toBigInteger(value);
    }
    if (wrappedTarget == java.math.BigDecimal.class) {
      return Converter.toBigDecimal(value);
    }
    if (targetClass.isEnum() && value instanceof CharSequence chars) {
      return convertEnumValue(targetClass, chars.toString());
    }
    if (targetClass == java.time.LocalDateTime.class && value instanceof CharSequence chars) {
      return Dates.toLocalDateTime(chars);
    }
    if (targetClass == java.time.LocalDate.class && value instanceof CharSequence chars) {
      return Dates.toLocalDate(chars);
    }
    if (targetClass == java.time.LocalTime.class && value instanceof CharSequence chars) {
      return Dates.toLocalTime(chars);
    }
    if (targetClass == Object.class) {
      return value;
    }
    return value;
  }

  private static Object convertBeanValue(Type targetType, Object value) {
    return convertBeanValue(targetType, value, newIdentityPath());
  }

  private static Object convertBeanValue(Type targetType, Object value, Set<Object> visiting) {
    MapSerializer serializer = MAP_SERIALIZERS.stream()
        .filter(s -> s.support(value))
        .findFirst()
        .orElse(null);
    if (serializer != null) {
      return serializer.serialize(targetType, value);
    }

    if (value == null || isSimpleValue(value)) {
      return value;
    }
    if (value instanceof Collection<?> collection) {
      if (!visiting.add(value)) {
        return null;
      }
      try {
        return collection.stream()
            .map(item -> convertBeanValue(item == null ? Object.class : item.getClass(), item, visiting))
            .collect(Collectors.toList());
      }
      finally {
        visiting.remove(value);
      }
    }
    if (value instanceof Map<?, ?> map) {
      if (!visiting.add(value)) {
        return null;
      }
      try {
        return map.entrySet().stream()
            .map(entry -> Tuple.of(entry.getKey(), convertBeanValue(entry.getValue() == null ? Object.class : entry.getValue().getClass(), entry.getValue(), visiting)))
            .collect(LinkedHashMap::new, (m, t) -> m.put(t._1(), t._2()), LinkedHashMap::putAll);
      }
      finally {
        visiting.remove(value);
      }
    }
    if (isBeanClass(value.getClass())) {
      return fromBean(value, visiting);
    }
    return value;
  }

  private static Set<Object> newIdentityPath() {
    return Collections.newSetFromMap(new IdentityHashMap<>());
  }

  private static Map<String, Object> toStringObjectMap(Map<?, ?> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    map.forEach((key, value) -> result.put(String.valueOf(key), value));
    return result;
  }

  private static boolean isAssignableValue(Class<?> targetClass, Object value) {
    return wrapPrimitive(targetClass).isAssignableFrom(value.getClass());
  }

  private static Class<?> wrapPrimitive(Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    if (type == int.class) {
      return Integer.class;
    }
    if (type == long.class) {
      return Long.class;
    }
    if (type == boolean.class) {
      return Boolean.class;
    }
    if (type == byte.class) {
      return Byte.class;
    }
    if (type == short.class) {
      return Short.class;
    }
    if (type == float.class) {
      return Float.class;
    }
    if (type == double.class) {
      return Double.class;
    }
    if (type == char.class) {
      return Character.class;
    }
    return type;
  }

  private static boolean isSimpleValue(Object value) {
    return value instanceof Number
        || value instanceof CharSequence
        || value instanceof Boolean
        || value instanceof Date
        || value instanceof java.time.temporal.Temporal
        || value.getClass().isEnum();
  }

  private static boolean isBeanClass(Class<?> type) {
    return type != null
        && type != Object.class
        && !type.isPrimitive()
        && !type.isArray()
        && !Map.class.isAssignableFrom(type)
        && !Iterable.class.isAssignableFrom(type)
        && !Class.class.isAssignableFrom(type)
        && !type.getName().startsWith("java.");
  }

  private static boolean shouldSkipBeanProperty(PropertyDescriptor property) {
    return "class".equals(property.getName())
        || "metaClass".equals(property.getName())
        || "properties".equals(property.getName());
  }

  private static Object convertEnumValue(Class<?> targetClass, String value) {
    for (Method method : ENUM_FACTORY_METHODS.get(targetClass)) {
      Object enumValue = invokeEnumMethod(method, null, value);
      if (enumValue != null) {
        return enumValue;
      }
    }

    for (Method method : ENUM_VALUE_METHODS.get(targetClass)) {
      for (Object enumConstant : targetClass.getEnumConstants()) {
        Object enumValue = invokeEnumMethod(method, enumConstant);
        if (enumValue != null && enumValue.toString().equalsIgnoreCase(value)) {
          return enumConstant;
        }
      }
    }

    for (Object enumConstant : targetClass.getEnumConstants()) {
      if (((Enum<?>) enumConstant).name().equals(value)) {
        return enumConstant;
      }
    }
    for (Object enumConstant : targetClass.getEnumConstants()) {
      if (((Enum<?>) enumConstant).name().equalsIgnoreCase(value)) {
        return enumConstant;
      }
    }
    throw new IllegalArgumentException("No enum constant " + targetClass.getName() + "." + value);
  }

  private static Object invokeEnumMethod(Method method, Object target, Object... args) {
    try {
      return method.invoke(target, args);
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to invoke enum conversion method " + method, e);
    }
  }

  private static Type resolveBeanPropertyReadType(Class<?> beanClass, PropertyDescriptor property) {
    var readMethod = property.getReadMethod();
    if (readMethod != null && readMethod.getParameterCount() == 0) {
      return resolveType(beanClass, readMethod.getGenericReturnType());
    }
    return Object.class;
  }

  private static Type resolveBeanPropertyTargetType(Class<?> beanClass, PropertyDescriptor property, Method writeMethod) {
    var readMethod = property.getReadMethod();
    if (readMethod != null && readMethod.getParameterCount() == 0) {
      Type readType = resolveType(beanClass, readMethod.getGenericReturnType());
      if (readType instanceof ParameterizedType || readType instanceof Class<?>) {
        return readType;
      }
    }
    return resolveType(beanClass, writeMethod.getGenericParameterTypes()[0]);
  }

  private static Type resolveType(Class<?> contextClass, Type type) {
    if (type instanceof TypeVariable<?> typeVariable) {
      Type resolvedType = TYPE_VARIABLES.get(contextClass).get(typeVariable);
      if (resolvedType == null || resolvedType == typeVariable) {
        return type;
      }
      return resolveType(contextClass, resolvedType);
    }
    if (type instanceof ParameterizedType parameterizedType) {
      Type[] actualTypes = parameterizedType.getActualTypeArguments();
      Type[] resolvedTypes = Arrays.stream(actualTypes)
          .map(actualType -> resolveType(contextClass, actualType))
          .toArray(Type[]::new);
      if (Arrays.equals(actualTypes, resolvedTypes)) {
        return type;
      }
      return new ResolvedParameterizedType(parameterizedType.getOwnerType(), parameterizedType.getRawType(), resolvedTypes);
    }
    return type;
  }

  private static void collectTypeVariables(Type type, Map<TypeVariable<?>, Type> resolvedTypes, Set<Type> visitedTypes) {
    if (type == null || !visitedTypes.add(type)) {
      return;
    }
    Class<?> rawClass = Types.rawClass(type);
    if (rawClass == Object.class) {
      return;
    }
    if (type instanceof ParameterizedType parameterizedType) {
      TypeVariable<?>[] variables = rawClass.getTypeParameters();
      Type[] actualTypes = parameterizedType.getActualTypeArguments();
      for (int i = 0; i < variables.length && i < actualTypes.length; i++) {
        resolvedTypes.put(variables[i], resolveTypeVariable(actualTypes[i], resolvedTypes));
      }
    }
    collectTypeVariables(rawClass.getGenericSuperclass(), resolvedTypes, visitedTypes);
    for (Type interfaceType : rawClass.getGenericInterfaces()) {
      collectTypeVariables(interfaceType, resolvedTypes, visitedTypes);
    }
  }

  private static Type resolveTypeVariable(Type type, Map<TypeVariable<?>, Type> resolvedTypes) {
    if (type instanceof TypeVariable<?> typeVariable) {
      return Optional.ofNullable(resolvedTypes.get(typeVariable)).orElse(type);
    }
    if (type instanceof ParameterizedType parameterizedType) {
      Type[] actualTypes = parameterizedType.getActualTypeArguments();
      Type[] resolvedActualTypes = Arrays.stream(actualTypes)
          .map(actualType -> resolveTypeVariable(actualType, resolvedTypes))
          .toArray(Type[]::new);
      if (Arrays.equals(actualTypes, resolvedActualTypes)) {
        return type;
      }
      return new ResolvedParameterizedType(parameterizedType.getOwnerType(), parameterizedType.getRawType(), resolvedActualTypes);
    }
    return type;
  }

  private static String resolveBeanPropertyName(Class<?> beanClass, PropertyDescriptor property, Method readMethod, Method writeMethod) {
    String annotatedName = readJsonPropertyName(readMethod)
        .or(() -> readJsonPropertyName(writeMethod))
        .or(() -> findBackingField(beanClass, property, readMethod).flatMap(Maps::readJsonPropertyName))
        .orElse(null);
    if (annotatedName != null) {
      return annotatedName;
    }
    return findBackingField(beanClass, property, readMethod)
        .filter(field -> isBooleanIsField(property, readMethod, field))
        .map(Field::getName)
        .orElse(property.getName());
  }

  private static List<String> resolveBeanPropertyAliases(Class<?> beanClass, PropertyDescriptor property, Method readMethod, Method writeMethod, String name) {
    LinkedHashSet<String> aliases = new LinkedHashSet<>();
    aliases.add(name);
    aliases.add(property.getName());
    readJsonPropertyName(readMethod).ifPresent(aliases::add);
    readJsonPropertyName(writeMethod).ifPresent(aliases::add);
    findBackingField(beanClass, property, readMethod).ifPresent(field -> {
      aliases.add(field.getName());
      readJsonPropertyName(field).ifPresent(aliases::add);
    });
    return Collections.unmodifiableList(new ArrayList<>(aliases));
  }

  private static Optional<String> readJsonPropertyName(AnnotatedElement element) {
    if (element == null) {
      return Optional.empty();
    }
    JsonProperty jsonProperty = element.getAnnotation(JsonProperty.class);
    if (jsonProperty == null || jsonProperty.value() == null || jsonProperty.value().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(jsonProperty.value());
  }

  private static Optional<Field> findBackingField(Class<?> beanClass, PropertyDescriptor property, Method readMethod) {
    List<String> candidates = new ArrayList<>();
    candidates.add(property.getName());
    if (readMethod != null && readMethod.getName().startsWith("is") && readMethod.getName().length() > 2) {
      candidates.add(readMethod.getName());
    }
    candidates.add("is" + Strings.upperFirst(property.getName()));
    for (String candidate : candidates) {
      Optional<Field> field = findField(beanClass, candidate);
      if (field.isPresent()) {
        return field;
      }
    }
    return Optional.empty();
  }

  private static Optional<Field> findField(Class<?> type, String fieldName) {
    Class<?> currentType = type;
    while (currentType != null && currentType != Object.class) {
      try {
        Field field = currentType.getDeclaredField(fieldName);
        field.setAccessible(true);
        return Optional.of(field);
      }
      catch (NoSuchFieldException e) {
        currentType = currentType.getSuperclass();
      }
    }
    return Optional.empty();
  }

  private static boolean isBooleanIsField(PropertyDescriptor property, Method readMethod, Field field) {
    return field.getName().equals("is" + Strings.upperFirst(property.getName()))
        && readMethod != null
        && readMethod.getName().equals(field.getName())
        && (field.getType() == boolean.class || field.getType() == Boolean.class);
  }

  private static final class BeanProperty {
    private final String name;
    private final List<String> aliases;
    private final Method readMethod;
    private final Method writeMethod;
    private final Type readType;
    private final Type writeType;

    private BeanProperty(Class<?> beanClass, PropertyDescriptor property) {
      this.readMethod = resolveReadMethod(property);
      this.writeMethod = resolveWriteMethod(beanClass, property);
      this.name = resolveBeanPropertyName(beanClass, property, readMethod, writeMethod);
      this.aliases = resolveBeanPropertyAliases(beanClass, property, readMethod, writeMethod, name);
      this.readType = readMethod == null ? Object.class : resolveBeanPropertyReadType(beanClass, property);
      this.writeType = writeMethod == null ? Object.class : resolveBeanPropertyTargetType(beanClass, property, writeMethod);
    }

    private String getName() {
      return name;
    }

    private boolean canRead() {
      return readMethod != null;
    }

    private boolean canWrite() {
      return writeMethod != null;
    }

    private boolean hasValue(Map<String, Object> map) {
      return aliases.stream().anyMatch(map::containsKey);
    }

    private Object readValue(Map<String, Object> map) {
      return aliases.stream()
          .filter(map::containsKey)
          .findFirst()
          .map(map::get)
          .orElse(null);
    }

    private Type getReadType() {
      return readType;
    }

    private Type getWriteType() {
      return writeType;
    }

    private Object read(Object bean) throws Exception {
      return readMethod.invoke(bean);
    }

    private void write(Object bean, Object value) throws Exception {
      writeMethod.invoke(bean, value);
    }

    private static Method resolveReadMethod(PropertyDescriptor property) {
      Method method = property.getReadMethod();
      if (method == null || method.getParameterCount() != 0) {
        return null;
      }
      method.setAccessible(true);
      return method;
    }

    private static Method resolveWriteMethod(Class<?> beanClass, PropertyDescriptor property) {
      Method method = property.getWriteMethod();
      if (method == null || method.getParameterCount() != 1) {
        method = findFluentWriteMethod(beanClass, property.getName()).orElse(null);
      }
      if (method == null || method.getParameterCount() != 1) {
        return null;
      }
      method.setAccessible(true);
      return method;
    }

    private static Optional<Method> findFluentWriteMethod(Class<?> beanClass, String propertyName) {
      String methodName = "set" + Strings.upperFirst(propertyName);
      return Stream.concat(Arrays.stream(beanClass.getMethods()), declaredMethods(beanClass).stream())
          .filter(method -> method.getName().equals(methodName))
          .filter(method -> method.getParameterCount() == 1)
          .filter(method -> !Modifier.isStatic(method.getModifiers()))
          .filter(method -> isSetterReturnType(beanClass, method))
          .findFirst();
    }

    private static List<Method> declaredMethods(Class<?> beanClass) {
      List<Method> methods = new ArrayList<>();
      Class<?> current = beanClass;
      while (current != null && current != Object.class) {
        methods.addAll(Arrays.asList(current.getDeclaredMethods()));
        current = current.getSuperclass();
      }
      return methods;
    }

    private static boolean isSetterReturnType(Class<?> beanClass, Method method) {
      Class<?> returnType = method.getReturnType();
      return returnType == Void.TYPE
          || (returnType != Object.class && returnType.isAssignableFrom(beanClass));
    }
  }

  private static final class ResolvedParameterizedType implements ParameterizedType {
    private final Type ownerType;
    private final Type rawType;
    private final Type[] actualTypes;

    private ResolvedParameterizedType(Type ownerType, Type rawType, Type[] actualTypes) {
      this.ownerType = ownerType;
      this.rawType = rawType;
      this.actualTypes = actualTypes.clone();
    }

    @Override
    public Type[] getActualTypeArguments() {
      return actualTypes.clone();
    }

    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public Type getOwnerType() {
      return ownerType;
    }
  }
}
