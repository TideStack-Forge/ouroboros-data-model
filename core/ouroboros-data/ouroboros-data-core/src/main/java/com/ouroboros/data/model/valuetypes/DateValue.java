package com.ouroboros.data.model.valuetypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataDates;

public class DateValue implements ValueType<LocalDate>, ValueTypeBuilder<LocalDate, DateValue> {

  @Override
  public String getName() {
    return "Date";
  }

  @Override
  public String getLabel() {
    return "日期";
  }

  @Override
  public Class<LocalDate> getType() {
    return LocalDate.class;
  }

  @Override
  public LocalDate convert(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDate localDateValue) {
      return localDateValue;
    }
    if (value instanceof LocalDateTime localDateTimeValue) {
      return localDateTimeValue.toLocalDate();
    }
    if (value instanceof CharSequence strValue) {
      return DataDates.toLocalDate(strValue.toString());
    }
    if (value instanceof Number numberValue) {
      return DataDates.toLocalDate(numberValue);
    }
    if (value instanceof Date dateValue) {
      return DataDates.toLocalDate(dateValue);
    }

    return null;
  }

  @Override
  public DateValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    return convert(value);
  }
}
