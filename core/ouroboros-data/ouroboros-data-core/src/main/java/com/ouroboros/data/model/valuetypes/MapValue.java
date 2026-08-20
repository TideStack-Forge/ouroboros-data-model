package com.ouroboros.data.model.valuetypes;

import java.sql.Clob;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.ValueTypeBuilder;
import com.ouroboros.data.util.DataJson;
import com.ouroboros.data.util.DataMaps;

public class MapValue implements ValueType<Map<String, Object>>, ValueTypeBuilder<Map<String, Object>, MapValue> {

  @Override
  public String getName() {
    return "Map";
  }

  @Override
  public String getLabel() {
    return "键值对";
  }

  @Override
  public Class<Map<String, Object>> getType() {
    return (Class<Map<String, Object>>) (Class<?>) Map.class;
  }

  @Override
  public Map<String, Object> convert(Object value) {
    if (value instanceof Map map) {
      return DataMaps.remap(map, String::valueOf);
    }

    if (value instanceof CharSequence str) {
      return DataJson.toMap(str);
    }

    // TODO: 此类型的希望不要一次性将数据全部加载到内存，后期考虑增加一个包裹类实现懒加载，临时处理Clob类型问题
    if (value instanceof Clob clob) {
      return DataJson.toMap(Try.of(() -> clob.getSubString(1, (int) clob.length())).get());
    }

    return new LinkedHashMap<>();
  }

  @Override
  public MapValue build(DataModelField field) {
    return this;
  }

  @Override
  public Object toPersistentValue(Object value) {
    if (value == null || value instanceof Collection<?>) {
      value = Collections.emptyMap();
    }

    return DataJson.toJsonString(value);
  }
}
