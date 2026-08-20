package com.ouroboros.data.model.valuetypes;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataConverters;

public class DoubleValue implements ValueType<Double>, ValueTypeBuilder<Double, DoubleValue> {

  @Override
  public String getName() {
    return "Double";
  }

  @Override
  public String getLabel() {
    return "双精度浮点数";
  }

  @Override
  public Class<Double> getType() {
    return Double.class;
  }

  @Override
  public Double convert(Object value) {
    return DataConverters.toDouble(value);
  }

  @Override
  public DoubleValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    return convert(value);
  }
}
