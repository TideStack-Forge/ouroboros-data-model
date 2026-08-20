package com.ouroboros.data.record;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface RecordList extends List<Record> {

  static RecordList of(List<? extends Map<String, ?>> list) {
    return new DefaultRecordListImpl(list);
  }

  static RecordList empty() {
    return new DefaultRecordListImpl(Collections.emptyList());
  }

  default <T> List<T> toBeanList(Class<T> clazz) {
    return this.stream()
        .map(r -> r.toBean(clazz))
        .collect(Collectors.toList());
  }
}
