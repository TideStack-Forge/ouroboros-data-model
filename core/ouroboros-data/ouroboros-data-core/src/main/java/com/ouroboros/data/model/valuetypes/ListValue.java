package com.ouroboros.data.model.valuetypes;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataJson;

public class ListValue implements ValueType<List<Object>>, ValueTypeBuilder<List<Object>, ListValue> {

  @Override
  public String getName() {
    return "List";
  }

  @Override
  public String getLabel() {
    return "列表";
  }

  @Override
  public Class<List<Object>> getType() {
    return (Class) List.class;
  }

  @Override
  public List<Object> convert(Object value) {
    if (value instanceof List list) {
      return list;
    }

    if (value instanceof Collection<?> collection) {
      return new ArrayList<>(collection);
    }

    if (value instanceof CharSequence charSequence) {
      return DataJson.toList(charSequence);
    }

    // TODO: 此类型的希望不要一次性将数据全部加载到内存，后期考虑增加一个包裹类实现懒加载，临时处理Clob类型问题
    if (value instanceof Clob clob) {
      return DataJson.toList(Try.of(() -> clob.getSubString(1, (int) clob.length())).get());
    }

    return new ArrayList<>();
  }

  @Override
  public ListValue build(DataModelField field) {
    return this;
  }
}
