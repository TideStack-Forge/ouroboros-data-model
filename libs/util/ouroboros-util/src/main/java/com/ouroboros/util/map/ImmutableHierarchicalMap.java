package com.ouroboros.util.map;

import java.util.*;
import java.util.function.Function;

import com.ouroboros.util.Maps;

/**
 * 不可修改的层级化Map门面类
 * <p>包装一个Map并提供只读的层级化操作，不允许修改底层Map</p>
 *
 */
public class ImmutableHierarchicalMap {
  private final Map<String, Object> wrappedMap;
  private final Function<String, List<String>> pathSplitter;

  /**
   * 构造函数
   *
   * @param map 要包装的Map，不能为null
   * @throws NullPointerException 如果map为null
   */
  public ImmutableHierarchicalMap(Map<String, Object> map) {
    this(map, Maps.Hierarchical::defaultPathSplitter);
  }

  /**
   * 构造函数（支持自定义路径分割器）
   *
   * @param map          要包装的Map，不能为null
   * @param pathSplitter 路径分割函数，不能为null
   * @throws NullPointerException 如果map或pathSplitter为null
   */
  public ImmutableHierarchicalMap(Map<String, Object> map, Function<String, List<String>> pathSplitter) {
    this.wrappedMap = Objects.requireNonNull(map, "map cannot be null");
    this.pathSplitter = Objects.requireNonNull(pathSplitter, "pathSplitter cannot be null");
  }

  /**
   * 获取层级化路径的值
   *
   * @param path 路径，如 "user.profile.name" 或 "items[0].name"
   * @return 值的Optional包装，如果路径不存在则返回Optional.empty()
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
   * 检查路径是否存在
   *
   * @param path 路径
   * @return 如果路径存在返回true，否则返回false
   */
  public boolean hasPath(String path) {
    return getValue(path).isPresent();
  }

  /**
   * 构建层级化结构（从扁平Map）
   * <p>将当前Map作为扁平Map处理，返回构建后的新Map</p>
   *
   * @return 构建后的新Map
   */
  public Map<String, Object> build() {
    return Maps.Hierarchical.build(wrappedMap, pathSplitter);
  }

  /**
   * 扁平化当前Map
   * <p>将层级化的Map转换为扁平结构，返回新的扁平Map</p>
   *
   * @return 扁平化后的新Map
   */
  public Map<String, Object> flatten() {
    return Maps.Hierarchical.flatten(wrappedMap);
  }

  /**
   * 获取包装的Map的只读视图
   * <p>返回的Map是只读的，尝试修改会抛出异常</p>
   *
   * @return 包装的Map的只读视图
   */
  public Map<String, Object> getWrappedMap() {
    return Collections.unmodifiableMap(wrappedMap);
  }

  /**
   * 转换为可修改的门面类
   * <p>创建底层Map的深度拷贝，返回可修改的门面类</p>
   *
   * @return 基于深度拷贝的可修改门面类
   */
  public MutableHierarchicalMap toMutable() {
    Map<String, Object> clonedMap = Maps.deepClone(wrappedMap, LinkedHashMap::new);
    return new MutableHierarchicalMap(clonedMap, pathSplitter);
  }

  /**
   * 转换为可修改的门面类（浅拷贝）
   * <p>创建底层Map的浅拷贝，返回可修改的门面类</p>
   * <p>注意：浅拷贝意味着嵌套对象仍然共享引用</p>
   *
   * @return 基于浅拷贝的可修改门面类
   */
  public MutableHierarchicalMap toMutableShallow() {
    Map<String, Object> clonedMap = new LinkedHashMap<>(wrappedMap);
    return new MutableHierarchicalMap(clonedMap, pathSplitter);
  }

  /**
   * 转换为字符串表示
   */
  @Override
  public String toString() {
    return "ImmutableHierarchicalMap{" + wrappedMap + "}";
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
    ImmutableHierarchicalMap that = (ImmutableHierarchicalMap) obj;
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
