package com.ouroboros.data.model.valuetypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataDates;

public class DateTimeValue implements ValueType<LocalDateTime>, ValueTypeBuilder<LocalDateTime, DateTimeValue> {

  @Override
  public String getName() {
    return "DateTime";
  }

  @Override
  public String getLabel() {
    return "日期时间";
  }

  @Override
  public Class<LocalDateTime> getType() {
    return LocalDateTime.class;
  }

  @Override
  public LocalDateTime convert(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDateTime localDateTimeValue) {
      return localDateTimeValue;
    }
    if (value instanceof Date dateValue) {
      return DataDates.toLocalDateTime(dateValue);
    }
    if (value instanceof CharSequence charSequenceValue) {
      return DataDates.toLocalDateTime(charSequenceValue.toString());
    }
    if (value instanceof Number numberValue) {
      return DataDates.toLocalDateTime(numberValue);
    }
    if (value instanceof LocalDate localDateValue) {
      return localDateValue.atStartOfDay();
    }
    if (value instanceof LocalTime localTime) {
      return LocalDateTime.of(LocalDate.now(), localTime);
    }
    return null;
  }

  @Override
  public DateTimeValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    return convert(value);
  }
}
