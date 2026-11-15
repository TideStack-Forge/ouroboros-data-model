package com.ouroboros.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.lang.reflect.Array;

@SuppressWarnings("unused")
public class Types {

  private Types() {
  }

  public static Class<?> rawClass(Type type) {
    if (type instanceof Class<?> clazz) {
      return clazz;
    }
    if (type instanceof ParameterizedType parameterizedType) {
      return rawClass(parameterizedType.getRawType());
    }
    if (type instanceof GenericArrayType arrayType) {
      return Array.newInstance(rawClass(arrayType.getGenericComponentType()), 0).getClass();
    }
    if (type instanceof WildcardType wildcardType) {
      Type[] upperBounds = wildcardType.getUpperBounds();
      return upperBounds.length == 0 ? Object.class : rawClass(upperBounds[0]);
    }
    if (type instanceof TypeVariable<?> typeVariable) {
      Type[] bounds = typeVariable.getBounds();
      return bounds.length == 0 ? Object.class : rawClass(bounds[0]);
    }
    return Object.class;
  }
}
