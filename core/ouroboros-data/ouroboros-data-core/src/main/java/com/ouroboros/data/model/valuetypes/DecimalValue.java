package com.ouroboros.data.model.valuetypes;

import java.math.BigDecimal;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataConverters;

public class DecimalValue implements ValueType<BigDecimal>, ValueTypeBuilder<BigDecimal, DecimalValue> {

  @Override
  public String getName() {
    return "Decimal";
  }

  @Override
  public String getLabel() {
    return "定点数";
  }

  @Override
  public Class<BigDecimal> getType() {
    return BigDecimal.class;
  }

  @Override
  public BigDecimal convert(Object value) {
    return DataConverters.toBigDecimal(value);
  }

  @Override
  public DecimalValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    return convert(value);
  }
}
