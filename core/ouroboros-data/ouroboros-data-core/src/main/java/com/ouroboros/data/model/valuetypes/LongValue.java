package com.ouroboros.data.model.valuetypes;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataConverters;

public class LongValue implements ValueType<Long>, ValueTypeBuilder<Long, LongValue> {
  @Override
  public String getName() {
    return "Long";
  }

  @Override
  public String getLabel() {
    return "长整型";
  }

  @Override
  public Class<Long> getType() {
    return Long.class;
  }

  @Override
  public Long convert(Object value) {
    return DataConverters.toLong(value);
  }

  @Override
  public LongValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    return convert(value);
  }
}
