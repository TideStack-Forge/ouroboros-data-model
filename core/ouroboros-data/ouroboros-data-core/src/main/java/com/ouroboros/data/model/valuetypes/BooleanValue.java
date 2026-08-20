package com.ouroboros.data.model.valuetypes;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataConverters;

public class BooleanValue implements ValueType<Boolean>, ValueTypeBuilder<Boolean, BooleanValue> {

  @Override
  public String getName() {
    return "Boolean";
  }

  @Override
  public String getLabel() {
    return "布尔值";
  }

  @Override
  public Class<Boolean> getType() {
    return Boolean.class;
  }

  @Override
  public Boolean convert(Object value) {
    return DataConverters.toBoolean(value);
  }

  @Override
  public BooleanValue build(DataModelField field) {
    return this;
  }
}
