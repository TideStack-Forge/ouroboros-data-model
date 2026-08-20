package com.ouroboros.data.record;

import java.util.Map;

import com.ouroboros.data.util.DataMaps;

public interface Record extends Map<String, Object> {
  static Record of(Map<String, ?> map) {
    if (map instanceof Record record) {
      return record;
    }
    return new DefaultRecordImpl(map);
  }

  default <T> T toBean(Class<T> clazz) {
    return DataMaps.tryToBean(clazz, this)
        //TODO: 加失败处理
        .getOrElse((T) null);
  }
}
