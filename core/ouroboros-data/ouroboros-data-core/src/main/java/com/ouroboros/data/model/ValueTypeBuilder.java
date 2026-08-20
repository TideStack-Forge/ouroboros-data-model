package com.ouroboros.data.model;

/**
 * 值类型
 *
 * @author Song Mingxu
 */
@SuppressWarnings("unused")
public interface ValueTypeBuilder<VT, T extends ValueType<VT>> {
  String getName();

  String getLabel();

  T build(DataModelField field);
}
