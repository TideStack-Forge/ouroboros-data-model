package com.ouroboros.data.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ouroboros.data.validation.Rule;

public interface DataModelField {
  String getName();

  String getLabel();

  String getDescription();

  String getType();

  String getRawName();

  String getRawType();

  ValueType<?> getValueType();

  Object getDefaultValue(Map<String, Object> context);

  List<Rule> getRules();

  Integer getDecimalDigits();

  Integer getSize();

  Boolean getIsNullable();

  Boolean getIsUnsigned();

  Boolean getIsAutoIncrement();

  Boolean getIsUnique();

  default UniquenessScope getUniquenessScope() {
    return null;
  }

  Map<String, Object> getExtraProps();

  Optional<Object> getExtraProp(String name);

  default <T> Optional<T> getExtraProp(Class<T> type, String name) {
    return getExtraProp(name).map(value -> {
      if (type.isInstance(value)) {
        return type.cast(value);
      } else {
        return null;
      }
    });
  }

  DataModel getDataModel();
}
