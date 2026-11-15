package com.ouroboros.util.map;


import java.util.*;
import java.util.function.Function;

import com.ouroboros.util.Maps;

/**
 * 可修改的层级化Map门面类
 * <p>包装一个Map并提供层级化操作，允许修改底层Map</p>
 *
  */
public class MutableHierarchicalMap {
  private final Map<String, Object> wrappedMap;
  private final Function<String, List<String>> pathSplitter;

  /**
   * 构造函数
   *
   * @param map 要包装的Map，不能为null
   * @throws NullPointerException 如果map为null
   */
  public MutableHierarchicalMap(Map<String, Object> map) {
    this(map, Maps.Hierarchical::defaultPathSplitter);
  }

  /**
   * 构造函数（支持自定义路径分割器）
   *
   * @param map          要包装的Map，不能为null
   * @param pathSplitter 路径分割函数，不能为null
   * @throws NullPointerException 如果map或pathSplitter为null
   */
  public MutableHierarchicalMap(Map<String, Object> map, Function<String, List<String>> pathSplitter) {
    this.wrappedMap = Objects.requireNonNull(map, "map cannot be null");
    this.pathSplitter = Objects.requireNonNull(pathSplitter, "pathSplitter cannot be null");
  }

  /**
   * 获取层级化路径的值
   *
   * @param path 路径，如 "user.profile.name" 或 "items[0].name"
   * @return 值的Optional包装，如果路径不存在则返回Optional.empty()
   * @example <pre>
   * MutableHierarchicalMap hierarchicalMap = new MutableHierarchicalMap(map);
   * Optional&lt;Object&gt; name = hierarchicalMap.getValue("user.profile.name");
   * Optional&lt;Object&gt; tag = hierarchicalMap.getValue("user.tags[0]");
   * </pre>
   */
  public Optional<Object> getValue(String path) {
    return Maps.Hierarchical.getValue(wrappedMap, path, pathSplitter);
  }

  /**
   * 获取层级化路径的值并转换类型
   *
   * @param <T>  目标类型
   * @param path 路径
   * @param type 目标类型的Class
   * @return 转换后的值，如果路径不存在或类型不匹配则返回Optional.empty()
   */
  public <T> Optional<T> getValue(String path, Class<T> type) {
    return getValue(path)
        .filter(type::isInstance)
        .map(type::cast);
  }

  /**
   * 设置层级化路径的值
   * <p>如果路径不存在会自动创建中间层级</p>
   *
   * @param path  路径，如 "user.profile.name" 或 "items[0]"
   * @param value 要设置的值
   * @return 当前实例（支持链式调用）
   * @example <pre>
   * MutableHierarchicalMap hierarchicalMap = new MutableHierarchicalMap(new HashMap&lt;&gt;());
   * hierarchicalMap.setValue("user.profile.name", "John")
   *               .setValue("user.age", 30)
   *               .setValue("user.tags[0]", "developer");
   * </pre>
   */
  public MutableHierarchicalMap setValue(String path, Object value) {
    Maps.Hierarchical.setValue(wrappedMap, path, pathSplitter, value);
    return this;
  }

  /**
   * 检查路径是否存在
   *
   * @param path 路径
   * @return 如果路径存在返回true，否则返回false
   */
  public boolean hasPath(String path) {
    return getValue(path).isPresent();
  }

  /**
   * 移除指定路径的值
   * <p>注意：这只能移除最终的键，不会清理空的中间层级</p>
   *
   * @param path 路径
   * @return 被移除的值，如果路径不存在则返回Optional.empty()
   */
  public Optional<Object> removePath(String path) {
    if (path == null) {
      return Optional.empty();
    }

    var pathParts = pathSplitter.apply(path);
    if (pathParts.isEmpty()) {
      return Optional.empty();
    }

    if (pathParts.size() == 1) {
      return Optional.ofNullable(wrappedMap.remove(pathParts.get(0)));
    }

    // 导航到父容器
    var parentPath = pathParts.subList(0, pathParts.size() - 1);
    var parentContainer = Maps.Hierarchical.getValue(wrappedMap, parentPath);
    var finalKey = pathParts.get(pathParts.size() - 1);

    if (parentContainer.isPresent()) {
      if (parentContainer.get() instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> parentMap = (Map<String, Object>) parentContainer.get();
        return Optional.ofNullable(parentMap.remove(finalKey));
      } else if (parentContainer.get() instanceof List && finalKey.matches("\\d+")) {
        @SuppressWarnings("unchecked")
        List<Object> parentList = (List<Object>) parentContainer.get();
        int index = Integer.parseInt(finalKey);
        if (index >= 0 && index < parentList.size()) {
          return Optional.ofNullable(parentList.remove(index));
        }
      }
    }

    return Optional.empty();
  }

  /**
   * 构建层级化结构（从扁平Map）
   * <p>将当前Map作为扁平Map处理，构建为层级化结构</p>
   *
   * @return 当前实例（支持链式调用）
   */
  public MutableHierarchicalMap build() {
    Map<String, Object> flattened = new LinkedHashMap<>(wrappedMap);
    wrappedMap.clear();
    wrappedMap.putAll(Maps.Hierarchical.build(flattened, pathSplitter));
    return this;
  }

  /**
   * 扁平化当前Map
   * <p>将层级化的Map转换为扁平结构，覆盖当前Map内容</p>
   *
   * @return 当前实例（支持链式调用）
   */
  public MutableHierarchicalMap flatten() {
    Map<String, Object> nested = new LinkedHashMap<>(wrappedMap);
    wrappedMap.clear();
    wrappedMap.putAll(Maps.Hierarchical.flatten(nested));
    return this;
  }

  /**
   * 获取包装的Map
   * <p>注意：返回的是对原始Map的引用，修改会影响这个门面类</p>
   *
   * @return 包装的Map
   */
  public Map<String, Object> getWrappedMap() {
    return wrappedMap;
  }

  /**
   * 创建不可修改的门面类
   *
   * @return 包装相同Map的不可修改门面类
   */
  public ImmutableHierarchicalMap asImmutable() {
    return new ImmutableHierarchicalMap(wrappedMap, pathSplitter);
  }

  /**
   * 转换为字符串表示
   */
  @Override
  public String toString() {
    return "MutableHierarchicalMap{" + wrappedMap + "}";
  }

  /**
   * 检查相等性
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MutableHierarchicalMap that = (MutableHierarchicalMap) obj;
    return Objects.equals(wrappedMap, that.wrappedMap);
  }

  /**
   * 计算哈希码
   */
  @Override
  public int hashCode() {
    return Objects.hash(wrappedMap);
  }
}