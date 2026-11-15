package com.ouroboros.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * List 工具类
 */
@SuppressWarnings("unused")
public class Lists {

  /**
   * 安全检查 List 内容物是否为 Map&lt;String, ?&gt;
   *
   * @param list list 可以为 Null
   */
  public static Boolean isStringKeyMapList(List<?> list) {
    if (list == null) {
      return false;
    }
    return list.stream()
        .allMatch(map -> map instanceof Map &&
            Maps.isStringKeyMap((Map<?, ?>) map));
  }

  /**
   * 安全检查 List 是否为空
   *
   * @param list list 可以为 Null
   */
  public static Boolean isEmpty(List<?> list) {
    return null == list || list.isEmpty();
  }

  /**
   * 将 List 填充到指定长度，以null填充
   */
  public static <T> void fill(List<T> list, Integer length) {
    fill(list, length, null);
  }

  /**
   * 将 List 填充到指定长度，以指定 item 填充
   */
  public static <T> void fill(List<T> list, Integer length, T item) {
    if (list.size() > length) {
      return;
    }
    var lengthToFill = length - list.size();
    for (int i = 0; i < lengthToFill; i++) {
      list.add(item);
    }
  }

  /**
   * 将 List 填充到指定长度，以指定 Supplier 填充
   */
  public static <T> void fill(List<T> list, int length, Supplier<T> supplier) {
    if (list.size() > length) {
      return;
    }
    var lengthToFill = length - list.size();
    for (int i = 0; i < lengthToFill; i++) {
      list.add(supplier.get());
    }
  }

  /**
   * 将 List 填充到指定长度，以指定 Function 填充，Function 参数为待填充位置的索引值
   */
  public static <T> void fill(List<T> list, int length, Function<Integer, T> fn) {
    if (list.size() > length) {
      return;
    }
    var lengthToFill = length - list.size();
    for (int i = 0; i < lengthToFill; i++) {
      list.add(fn.apply(i));
    }
  }

  /**
   * 指定 List 特定位置的值，若长度不足则自动补齐
   */
  public static <T> T setValue(List<T> list, Integer index, T value, T fillItem) {
    fill(list, index, fillItem);
    if (list.size() == index) {
      list.add(value);
      return value;
    }
    return list.set(index, value);
  }


  /**
   * 指定 List 特定位置的值，若长度不足则自动以null补齐
   */
  public static <T> T setValue(List<T> list, Integer index, T value) {
    return setValue(list, index, value, (T) null);
  }

  /**
   * 指定 List 特定位置的值，若长度不足则自动补齐
   */
  public static <T> T setValue(List<T> list, Integer index, T item, Supplier<T> fillValueSupplier) {
    fill(list, index - 1, fillValueSupplier);
    if (list.size() == index) {
      list.add(item);
      return item;
    }
    return list.set(index, item);
  }

  /**
   * 指定 List 特定位置的值，若长度不足则自动补齐
   */
  public static <T> T setValue(List<T> list, Integer index, T item, Function<Integer, T> fillValueFunction) {
    fill(list, index - 1, fillValueFunction);
    if (list.size() == index) {
      list.add(item);
      return item;
    }
    return list.set(index, item);
  }

  /**
   * 获取 List 指定位置的值，若长度不足则返回 defaultValue
   */
  public static <T> T getValue(List<T> list, Integer index, T defaultValue) {
    if (index >= list.size() || index < 0) {
      return defaultValue;
    }
    return list.get(index);
  }

  /**
   * 获取 List 指定位置的值，若长度不足则返回 null
   */
  public static <T> T getValue(List<T> list, Integer index) {
    return getValue(list, index, null);
  }

  /**
   * 删除 List 指定位置的值并返回，若长度不足则直接返回 null
   */
  public static <T> T removeValue(List<T> list, Integer index) {
    if (index >= list.size() || index < 0) {
      return null;
    }
    return list.remove(index.intValue());
  }

  @SafeVarargs
  public static <T> List<T> merge(Collection<T>... lists) {
    var newList = new ArrayList<T>(lists.length);
    for (Collection<T> list : lists) {
      newList.addAll(list);
    }
    return newList;
  }

  /**
   * 深拷贝 List
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T> deepClone(Collection<T> list) {
    var newList = new ArrayList<T>(list.size());
    for (T item : list) {
      if (item instanceof Collection<?> collection) {
        newList.add((T) deepClone(collection));
      } else if (item instanceof Map<?, ?> map) {
        newList.add((T) Maps.deepClone(map, LinkedHashMap::new));
      } else {
        newList.add(item);
      }
    }
    return newList;
  }

  /**
   * 列表分块
   */
  public static <T> List<List<T>> partition(List<T> list, int chunkSize) {
    var result = new ArrayList<List<T>>();
    for (int i = 0; i < list.size(); i += chunkSize) {
      result.add(list.subList(i, Math.min(i + chunkSize, list.size())));
    }
    return result;
  }
}
