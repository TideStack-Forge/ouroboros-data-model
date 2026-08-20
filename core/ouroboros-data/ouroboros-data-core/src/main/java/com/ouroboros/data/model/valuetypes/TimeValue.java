package com.ouroboros.data.model.valuetypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataDates;

public class TimeValue implements ValueType<LocalTime>, ValueTypeBuilder<LocalTime, TimeValue> {

  @Override
  public String getName() {
    return "Time";
  }

  @Override
  public String getLabel() {
    return "时间";
  }

  @Override
  public Class<LocalTime> getType() {
    return LocalTime.class;
  }

  @Override
  public LocalTime convert(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalTime) {
      return (LocalTime) value;
    }
    if (value instanceof Date dateValue) {
      return DataDates.toLocalTime(dateValue);
    }
    if (value instanceof CharSequence charSequenceValue) {
      return DataDates.toLocalTime(charSequenceValue.toString());
    }
    if (value instanceof Number numberValue) {
      return DataDates.toLocalTime(numberValue);
    }
    if (value instanceof LocalDateTime localDateTimeValue) {
      return localDateTimeValue.toLocalTime();
    }
    return null;
  }

  @Override
  public TimeValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    return convert(value);
  }
}
