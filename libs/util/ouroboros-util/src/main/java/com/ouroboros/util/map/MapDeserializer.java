package com.ouroboros.util.map;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import com.ouroboros.util.SPI;

/**
 * Map 转 Bean 时，类型转换接口函数，根据给定的值和目标类型，由用户自定义转换规则。
 *
 * @author liansz
 */
public interface MapDeserializer {

  /**
   * 是否支持给定的值
   *
   * @param targetType 目标Type，非泛型类使用
   * @return
   */
  boolean support(Type targetType);

  /**
   * 根据给定的值和目标类型，进行转换
   *
   * @param targetType 目标Type，非泛型类使用
   * @param value      原始值
   * @return 转换后的值
   */
  Object deserialize(Type targetType, Object value);

  static List<MapDeserializer> getAllFromSPI() {
    return SPI.getSortedServiceStream(MapDeserializer.class).collect(Collectors.toList());
  }
}
