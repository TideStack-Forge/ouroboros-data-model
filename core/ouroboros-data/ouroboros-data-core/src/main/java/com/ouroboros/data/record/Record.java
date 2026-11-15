package com.ouroboros.data.record;

import java.util.Map;

import com.ouroboros.data.util.DataMaps;

public interface Record extends Map<String, Object> {
  static Record of(Map<String, ?> map) {
    if (map == null) {
      return null;
    }
    if (map instanceof Record record) {
      return record;
    }
    return new DefaultRecordImpl(map);
  }

  default <T> T toBean(Class<T> clazz) {
    T bean = DataMaps.tryToBean(clazz, this).get();
    if (bean == null && !isEmpty()) {
      throw new IllegalStateException("Non-empty record converted to null: " + clazz.getName());
    }
    return bean;
  }
}
