package com.ouroboros.data.model.valuetypes;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataConverters;

public class SnowflakeIdValue implements ValueType<String>, ValueTypeBuilder<String, SnowflakeIdValue> {
  @Override
  public String getName() {
    return "Snowflake";
  }

  @Override
  public String getLabel() {
    return "雪花ID";
  }

  @Override
  public Class<String> getType() {
    return String.class;
  }

  @Override
  public String convert(Object value) {
    return DataConverters.toString(value);
  }

  @Override
  public Object toPersistentValue(Object value) {
    return DataConverters.toLong(value);
  }

  @Override
  public SnowflakeIdValue build(DataModelField field) {
    return this;
  }
}
