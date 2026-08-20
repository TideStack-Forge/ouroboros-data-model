package com.ouroboros.data.util;

import java.util.List;
import java.util.Map;

public final class DataLists {
  private DataLists() {
  }

  public static Boolean isStringKeyMapList(List<?> list) {
    return list != null && list.stream().allMatch(value -> value instanceof Map<?, ?> map && DataMaps.isStringKeyMap(map));
  }

  public static <T> T getValue(List<T> list, Integer index) {
    if (index == null || index < 0 || index >= list.size()) {
      return null;
    }
    return list.get(index);
  }

  public static <T> T setValue(List<T> list, Integer index, T value) {
    while (list.size() < index) {
      list.add(null);
    }
    if (list.size() == index) {
      list.add(value);
      return value;
    }
    return list.set(index, value);
  }

  public static <T> T removeValue(List<T> list, Integer index) {
    if (index == null || index < 0 || index >= list.size()) {
      return null;
    }
    return list.remove(index.intValue());
  }
}
