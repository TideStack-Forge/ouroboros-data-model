package com.ouroboros.data.model.valuetypes;

import java.sql.Clob;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataJson;
import com.ouroboros.data.util.DataJsonBag;

public class JsonValue implements ValueType<DataJsonBag>, ValueTypeBuilder<DataJsonBag, JsonValue> {
  @Override
  public String getName() {
    return "Json";
  }

  @Override
  public String getLabel() {
    return "Json";
  }

  @Override
  public Class<DataJsonBag> getType() {
    return DataJsonBag.class;
  }

  @Override
  public DataJsonBag convert(Object value) {
    if (value instanceof CharSequence charSequence) {
      return DataJson.toJsonBag(charSequence);
    }

    // TODO: 此类型的希望不要一次性将数据全部加载到内存，后期考虑增加一个包裹类实现懒加载，临时处理Clob类型问题
    if (value instanceof Clob clob) {
      return DataJson.toJsonBag(Try.of(() -> clob.getSubString(1, (int) clob.length())).get());
    }

    return DataJsonBag.of(value);
  }

  @Override
  public JsonValue build(DataModelField field) {
    return this;
  }
}
