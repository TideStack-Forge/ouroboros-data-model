package com.ouroboros.util.map;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import com.ouroboros.util.Maps;
import com.ouroboros.util.Types;

public class ModelValueMapTypeConvert implements MapSerializer, MapDeserializer {

  @Override
  public boolean support(Object value) {
    return value instanceof ModelValue;
  }

  @Override
  public Object serialize(Type targetType, Object value) {
    if (value == null) {
      return null;
    }

    return ((ModelValue) value).get();
  }

  @Override
  public boolean support(Type targetType) {
    return ModelValue.class.isAssignableFrom(Types.rawClass(targetType));
  }

  @Override
  public ModelValue deserialize(Type targetType, Object value) {
    if (value == null) {
      return null;
    }

    Type[] actualTypeArguments = ((ParameterizedType) targetType).getActualTypeArguments();
    if (actualTypeArguments[0] == value.getClass()) {
      return ModelValue.primary(value);
    } else {
      return ModelValue.model(Maps.toBean((Class<?>) actualTypeArguments[1], (Map) value));
    }
  }
}
