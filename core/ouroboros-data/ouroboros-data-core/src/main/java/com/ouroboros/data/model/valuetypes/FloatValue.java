package com.ouroboros.data.model.valuetypes;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataConverters;

public class FloatValue implements ValueType<Float>, ValueTypeBuilder<Float, FloatValue> {

  @Override
  public String getName() {
    return "Float";
  }

  @Override
  public String getLabel() {
    return "浮点数";
  }

  @Override
  public Class<Float> getType() {
    return Float.class;
  }

  @Override
  public Float convert(Object value) {
    return DataConverters.toFloat(value);
  }

  @Override
  public FloatValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    return convert(value);
  }
}
