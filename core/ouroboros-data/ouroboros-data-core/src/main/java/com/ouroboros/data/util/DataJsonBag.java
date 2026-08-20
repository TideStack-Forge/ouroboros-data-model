package com.ouroboros.data.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

public class DataJsonBag {
  private final List<Object> list;
  private final Map<String, Object> map;
  private final Object value;

  public DataJsonBag(List<Object> list) {
    this.list = list;
    this.map = null;
    this.value = null;
  }

  public DataJsonBag(Map<String, Object> map) {
    this.list = null;
    this.map = map;
    this.value = null;
  }

  public DataJsonBag(Object value) {
    this.list = null;
    this.map = null;
    this.value = value;
  }

  public static DataJsonBag of(Object value) {
    if (value instanceof List<?> values) {
      return new DataJsonBag(new ArrayList<>(values));
    }
    if (value instanceof Map<?, ?> values) {
      var result = new LinkedHashMap<String, Object>();
      values.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
      return new DataJsonBag(result);
    }
    return new DataJsonBag(value);
  }

  public static DataJsonBag empty() {
    return new DataJsonBag((Object) null);
  }

  public Boolean isNull() {
    return list == null && map == null && value == null;
  }

  public Boolean isList() {
    return list != null;
  }

  public Boolean isMap() {
    return map != null;
  }

  public Boolean isValue() {
    return value != null;
  }

  public List<Object> getList() {
    return list;
  }

  public Map<String, Object> getMap() {
    return map;
  }

  public Object get() {
    return value;
  }

  public Object get(String key) {
    if (isMap()) {
      return getMap().get(key);
    }
    return isList() && key.matches("\\d+") ? get(Integer.parseInt(key)) : null;
  }

  public Object get(int index) {
    if (isList()) {
      return DataLists.getValue(getList(), index);
    }
    return isMap() ? getMap().get(String.valueOf(index)) : null;
  }

  public Object put(String key, Object value) {
    if (isMap()) {
      return getMap().put(key, value);
    }
    return isList() && key.matches("\\d+") ? put(Integer.parseInt(key), value) : null;
  }

  public Object put(int index, Object value) {
    if (isList()) {
      return DataLists.setValue(getList(), index, value);
    }
    return isMap() ? getMap().put(String.valueOf(index), value) : null;
  }

  public Object remove(String key) {
    if (isMap()) {
      return getMap().remove(key);
    }
    return isList() && key.matches("\\d+") ? remove(Integer.parseInt(key)) : null;
  }

  public Object remove(int index) {
    if (isList()) {
      return DataLists.removeValue(getList(), index);
    }
    return isMap() ? getMap().remove(String.valueOf(index)) : null;
  }

  @JsonValue
  public Object toJsonValue() {
    if (isMap()) {
      return map;
    }
    if (isList()) {
      return list;
    }
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (obj instanceof DataJsonBag bag) {
      return java.util.Objects.equals(toJsonValue(), bag.toJsonValue());
    }
    return java.util.Objects.equals(toJsonValue(), obj);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hashCode(toJsonValue());
  }
}
