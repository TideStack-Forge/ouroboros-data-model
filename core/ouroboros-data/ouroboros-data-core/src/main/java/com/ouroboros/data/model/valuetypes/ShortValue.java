package com.ouroboros.data.model.valuetypes;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataConverters;

public class ShortValue implements ValueType<Short>, ValueTypeBuilder<Short, ShortValue> {
  @Override
  public String getName() {
    return "Short";
  }

  @Override
  public String getLabel() {
    return "短整型";
  }

  @Override
  public Class<Short> getType() {
    return Short.class;
  }

  @Override
  public Short convert(Object value) {
    return DataConverters.toShort(value);
  }

  @Override
  public ShortValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    return convert(value);
  }
}
