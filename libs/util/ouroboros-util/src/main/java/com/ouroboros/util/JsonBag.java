package com.ouroboros.util;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ouroboros.util.json.JsonBagDeserializer;
import com.ouroboros.util.json.JsonBagSerializer;

/**
 * JsonBag - JSON包装器
 * 用于容纳JSON对象、数组、字符串、数字、布尔值、空值
 */
@JsonSerialize(using = JsonBagSerializer.class)
@JsonDeserialize(using = JsonBagDeserializer.class)
@SuppressWarnings({"unused"})
public class JsonBag {
  private List<Object> list;
  private Map<String, Object> map;
  private Object value;

  public JsonBag(List<Object> list) {
    this.list = list;
  }

  public JsonBag(Map<String, Object> map) {
    this.map = map;
  }

  public JsonBag(Object value) {
    this.value = value;
  }

  @SuppressWarnings("unchecked")
  public static JsonBag of(Object value) {
    if (value instanceof List) {
      return new JsonBag((List<Object>) value);
    }
    if (value instanceof Map) {
      return new JsonBag((Map<String, Object>) value);
    }
    return new JsonBag(value);
  }

  public static JsonBag empty() {
    return new JsonBag((Object) null);
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
    return isMap()
        ? getMap().get(key)
        : isList() && key.matches("\\d+")
        ? get(Integer.parseInt(key))
        : null;
  }

  public Object get(int index) {
    return isList()
        ? Lists.getValue(getList(), index)
        : isMap()
        ? getMap().get(String.valueOf(index))
        : null;
  }

  public Object put(String key, Object value) {
    return isMap()
        ? getMap().put(key, value)
        : isList() && key.matches("\\d+")
        ? put(Integer.parseInt(key), value)
        : null;
  }

  public Object put(int index, Object value) {
    return isList()
        ? Lists.setValue(getList(), index, value)
        : isMap()
        ? getMap().put(String.valueOf(index), value)
        : null;
  }

  public Object remove(String key) {
    return isMap()
        ? getMap().remove(key)
        : isList() && key.matches("\\d+")
        ? Lists.removeValue(getList(), Integer.parseInt(key))
        : null;
  }

  public Object remove(int index) {
    return isList()
        ? Lists.removeValue(getList(), index)
        : isMap()
        ? getMap().remove(String.valueOf(index))
        : null;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (obj instanceof JsonBag bag) {
      if (isNull() && bag.isNull()) {
        return true;
      }
      if (isList() && bag.isList()) {
        return getList().equals(bag.list);
      }
      if (isMap() && bag.isMap()) {
        return getMap().equals(bag.map);
      }
      if (isValue() && bag.isValue()) {
        return get().equals(bag.value);
      }
    }
    if (obj instanceof List listValue) {
      return isList() && getList().equals(listValue);
    }
    if (obj instanceof Map mapValue) {
      return isMap() && getMap().equals(mapValue);
    }
    return isValue() && get().equals(obj);
  }
}
