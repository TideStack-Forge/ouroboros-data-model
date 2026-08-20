package com.ouroboros.data.model.valuetypes;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataConverters;

public class IntegerValue implements ValueType<Integer>, ValueTypeBuilder<Integer, IntegerValue> {

  @Override
  public String getName() {
    return "Integer";
  }

  @Override
  public String getLabel() {
    return "整型";
  }

  @Override
  public Class<Integer> getType() {
    return Integer.class;
  }

  @Override
  public Integer convert(Object value) {
    return DataConverters.toInteger(value);
  }

  @Override
  public IntegerValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    return convert(value);
  }
}
