package com.ouroboros.data.record;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ouroboros.data.util.DataJson;

@SuppressWarnings("deprecation")
public class DefaultRecordImpl implements Record {
  final Map<String, ?> data;

  public DefaultRecordImpl(Map<String, ?> data) {
    this.data = data;
  }

  @Override
  public int size() {
    return data.size();
  }

  @Override
  public boolean isEmpty() {
    return data.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return data.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return data.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return data.get(key);
  }

  @Override
  public Object put(String key, Object value) {
    throw new UnsupportedOperationException("Record 不支持修改操作");
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException("Record 不支持修改操作");
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    throw new UnsupportedOperationException("Record 不支持修改操作");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("Record 不支持修改操作");
  }

  @Override
  public Set<String> keySet() {
    return data.keySet();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<Object> values() {
    return (Collection<Object>) data.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return data.entrySet().stream()
        .map(entry -> new Entry<String, Object>() {
          @Override
          public String getKey() {
            return entry.getKey();
          }

          @Override
          public Object getValue() {
            return entry.getValue();
          }

          @Override
          public Object setValue(Object value) {
            throw new UnsupportedOperationException("Record 不支持修改操作");
          }
        })
        .collect(Collectors.toSet());
  }


  @SuppressWarnings("unchecked")
  public void mutate(Consumer<Map<String, Object>> modifier) {
    modifier.accept((Map<String, Object>) data);
  }

  @Override
  public String toString() {
    return DataJson.toJsonString(data);
  }
}
