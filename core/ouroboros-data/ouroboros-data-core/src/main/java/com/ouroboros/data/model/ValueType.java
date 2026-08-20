package com.ouroboros.data.model;

/**
 * 值类型
 *
 * @author Song Mingxu
 */
@SuppressWarnings("unused")
public interface ValueType<T> {
  String getName();

  String getLabel();

  default Boolean canPopulate() {
    return false;
  }

  default Boolean isPhysical() {
    return true;
  }

  Class<T> getType();

  T convert(Object value);

  default Object convertToPersistentValue(Object value) {
    return toPersistentValue(convert(value));
  }

  default Object toPersistentValue(Object value) {
    return value;
  }

}
