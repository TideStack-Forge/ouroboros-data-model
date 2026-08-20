package com.ouroboros.data.model;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import com.ouroboros.data.util.DataServices;

public class DefaultValueTypeFactory implements ValueTypeFactory {
  static UnknownValueType instance = new UnknownValueType();
  final Map<String, ValueTypeBuilder<?, ValueType<?>>> VALUE_TYPE_BUILDER_MAP;

  @SuppressWarnings("unchecked")
  public DefaultValueTypeFactory() {
    VALUE_TYPE_BUILDER_MAP = DataServices.getSortedServiceStream(ValueTypeBuilder.class)
        .map(valueType -> Tuple.of(valueType.getName(), valueType))
        .collect(Collectors.toMap(
            Tuple2::_1,
            Tuple2::_2,
            (v1, v2) -> v2));
  }

  @Override
  public Optional<ValueType> apply(DataModelField field) {
    var typeName = field.getType();
    var builder = Optional.ofNullable(VALUE_TYPE_BUILDER_MAP.get(typeName));
    if (builder.isPresent()) {
      return builder.map(b -> b.build(field));
    }
    return Optional.of(instance);
  }

  static class UnknownValueType implements ValueType<Object> {

    @Override
    public String getName() {
      return "Unknown";
    }

    @Override
    public String getLabel() {
      return "未知类型";
    }

    @Override
    public Class<Object> getType() {
      return Object.class;
    }

    @Override
    public Object convert(Object value) {
      return value;
    }
  }
}
