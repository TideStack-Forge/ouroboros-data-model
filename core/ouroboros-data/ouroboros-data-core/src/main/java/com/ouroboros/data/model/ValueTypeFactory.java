package com.ouroboros.data.model;

import java.util.Optional;
import java.util.function.Function;

import com.ouroboros.data.util.DataServices;

public interface ValueTypeFactory extends Function<DataModelField, Optional<ValueType>> {
  ValueTypeFactory VALUE_TYPE_FACTORY_CHAIN = DataServices.getSortedServiceStream(ValueTypeFactory.class)
      .reduce(meta -> Optional.empty(),
          (prev, curr) -> meta -> {
            var valueType = curr.apply(meta);
            if (valueType.isPresent()) {
              return valueType;
            }
            return prev.apply(meta);
          });

  @SuppressWarnings({"unused"})
  static Optional<ValueType> getValueType(DataModelField field) {
    return VALUE_TYPE_FACTORY_CHAIN.apply(field);
  }
}
