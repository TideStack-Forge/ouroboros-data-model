package com.ouroboros.data.model.valuetypes;

import java.sql.Clob;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataConverters;

public class StringValue implements ValueType<String>, ValueTypeBuilder<String, StringValue> {

  @Override
  public String getName() {
    return "String";
  }

  @Override
  public String getLabel() {
    return "字符串";
  }

  @Override
  public Class<String> getType() {
    return String.class;
  }

  @Override
  public String convert(Object value) {
    // TODO: 此类型的希望不要一次性将数据全部加载到内存，后期考虑增加一个包裹类实现懒加载，临时处理Clob类型问题
    if (value instanceof Clob clob) {
      return Try.of(() -> clob.getSubString(1, (int) clob.length())).get();
    }

    return DataConverters.toString(value);
  }

  @Override
  public StringValue build(DataModelField field) {
    return this;
  }
}
