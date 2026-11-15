package com.ouroboros.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author liansz
 */
public class GenericTypeUtil {

  /**
   * 获取泛型类型（根据继承递归向上查找）
   *
   * @param instance
   * @param targetClass
   * @return
   */
  public static Class<?>[] getGenericType(Object instance, Class<?> targetClass) {
    if (instance == null || targetClass == null) {
      throw new IllegalArgumentException("Instance and targetClass must not be null.");
    }

    List<Type> types = new ArrayList<>();

    Type genericSuperclass = instance.getClass().getGenericSuperclass();
    if (genericSuperclass != null) {
      types.add(genericSuperclass);
    }

    Type[] genericInterfaces = instance.getClass().getGenericInterfaces();
    types.addAll(Arrays.asList(genericInterfaces));

    return getGenericType(types, targetClass);
  }

  private static Class<?>[] getGenericType(List<Type> types, Class<?> targetClass) {
    // 看一下当前类是否包含目标类型
    for (Type type : types) {
      if (type instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() == targetClass) {
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        Class<?>[] classes = new Class[actualTypeArguments.length];
        for (int i = 0; i < actualTypeArguments.length; i++) {
          classes[i] = (Class<?>) actualTypeArguments[i];
        }
        return classes;
      }
    }

    // 如果当前类不包含目标类型，那递归查找父类
    List<Type> superTypes = new ArrayList<>();
    for (Type type : types) {
      Class<?> rawType;
      if (type instanceof ParameterizedType parameterizedType) {
        rawType = (Class<?>) parameterizedType.getRawType();
      } else if (type instanceof Class clazz) {
        rawType = clazz;
      } else {
        continue;
      }

      Type genericSuperclass = rawType.getGenericSuperclass();
      if (genericSuperclass != null) {
        superTypes.add(genericSuperclass);
      }

      Type[] genericInterfaces = rawType.getGenericInterfaces();
      superTypes.addAll(Arrays.asList(genericInterfaces));
    }

    // 如果没有父类、接口，那就返回空
    if (superTypes.isEmpty()) {
      return Collections.emptyList().toArray(new Class[0]);
    }

    return getGenericType(superTypes, targetClass);
  }
}
