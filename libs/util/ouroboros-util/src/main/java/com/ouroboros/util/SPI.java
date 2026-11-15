package com.ouroboros.util;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Priority;

/**
 * SPI 相关工具方法
 */
@SuppressWarnings("unused")
public final class SPI {
  static ConcurrentMap<String, List<?>> cachedSpiInstanceMap = new ConcurrentSkipListMap<>();

  private SPI() {
  }

  /**
   * 根据 Priority 排序
   *
   * @param a a
   * @param b b
   * @return 同 Comparator 返回
   */
  public static <T> int sortByPriority(T a, T b) {
    var pA = a.getClass().getAnnotation(Priority.class);
    var pB = b.getClass().getAnnotation(Priority.class);
    return (pA == null ? 0 : pA.value()) - (pB == null ? 0 : pB.value());
  }

  /**
   * 获取指定接口的 SPI 实例流
   *
   * @param serviceClass 实例类型
   * @return serviceClass 的 Stream
   */
  public static <T> Stream<T> getServiceStream(Class<T> serviceClass) {
    return StreamSupport.stream(ServiceLoader.load(serviceClass).spliterator(), false);
  }

  /**
   * 返回根据 Priority 排序后的实例流
   *
   * @param serviceClass 实例类型
   * @return serviceClass 的 Stream
   */
  public static <T> Stream<T> getSortedServiceStream(Class<T> serviceClass) {
    return getServiceStream(serviceClass).sorted(SPI::sortByPriority);
  }


  /**
   * 返回根据 Priority 反向排序后的实例流
   *
   * @param serviceClass 实例类型
   * @return serviceClass 的 Stream
   */
  public static <T> Stream<T> getReversedServiceStream(Class<T> serviceClass) {
    return getServiceStream(serviceClass).sorted(((Comparator<T>) SPI::sortByPriority).reversed());
  }

  /**
   * 获取指定接口的 SPI 实例流
   *
   * @param serviceClass 实例类型
   * @param p1           实例类型参数
   * @return serviceClass 的 Stream
   */
  public static <T, P1> Stream<T> getServiceStream(Class<T> serviceClass, Class<P1> p1) {
    return getServiceStream(serviceClass)
        .filter(s -> s.getClass().getTypeParameters().length == 1)
        .filter(s -> s.getClass().getTypeParameters()[0].getBounds()[0].getTypeName().equals(p1.getTypeName()));
  }

  /**
   * 返回根据 Priority 排序后的实例列表
   *
   * @param serviceClass 实例类型
   * @param p1           实例类型参数
   * @return serviceClass 的 Stream
   */
  public static <T, P1> Stream<T> getSortedServiceStream(Class<T> serviceClass, Class<P1> p1) {
    return getServiceStream(serviceClass, p1).sorted(SPI::sortByPriority);
  }

  /**
   * 返回根据 Priority 反向排序后的实例列表
   *
   * @param serviceClass 实例类型
   * @param p1           实例类型参数
   * @return serviceClass 的 Stream
   */
  public static <T, P1> Stream<T> getReversedServiceStream(Class<T> serviceClass, Class<P1> p1) {
    return getServiceStream(serviceClass, p1).sorted(((Comparator<T>) SPI::sortByPriority).reversed());
  }

  /**
   * 获取指定接口的 SPI 实例流
   *
   * @param serviceClass 实例类型
   * @param p1           实例类型参数1
   * @param p2           实例类型参数2
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2> Stream<T> getServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2) {
    return getServiceStream(serviceClass)
        .filter(s -> s.getClass().getTypeParameters().length == 2)
        .filter(s -> s.getClass().getTypeParameters()[0].getBounds()[0].getTypeName().equals(p1.getTypeName()))
        .filter(s -> s.getClass().getTypeParameters()[1].getBounds()[0].getTypeName().equals(p2.getTypeName()));
  }

  /**
   * 返回根据 Priority 排序后的实例列表
   *
   * @param serviceClass 实例类型
   * @param p1           实例类型参数1
   * @param p2           实例类型参数2
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2> Stream<T> getSortedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2) {
    return getServiceStream(serviceClass, p1, p2).sorted(SPI::sortByPriority);
  }

  /**
   * 返回根据 Priority 反向排序后的实例列表
   *
   * @param serviceClass 实例类型
   * @param p1           实例类型参数1
   * @param p2           实例类型参数2
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2> Stream<T> getReversedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2) {
    return getServiceStream(serviceClass, p1, p2).sorted(((Comparator<T>) SPI::sortByPriority).reversed());
  }

  /**
   * 获取指定接口的 SPI 实例流
   *
   * @param serviceClass 实例类型
   * @param p1           实例类型参数1
   * @param p2           实例类型参数2
   * @param p3           实例类型参数3
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2, P3> Stream<T> getServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
    return getServiceStream(serviceClass)
        .filter(s -> s.getClass().getTypeParameters().length == 3)
        .filter(s -> s.getClass().getTypeParameters()[0].getBounds()[0].getTypeName().equals(p1.getTypeName()))
        .filter(s -> s.getClass().getTypeParameters()[1].getBounds()[0].getTypeName().equals(p2.getTypeName()))
        .filter(s -> s.getClass().getTypeParameters()[2].getBounds()[0].getTypeName().equals(p3.getTypeName()));
  }

  /**
   * 返回根据 Priority 排序后的实例列表
   *
   * @param serviceClass 实例类型
   * @param p1           实例类型参数1
   * @param p2           实例类型参数2
   * @param p3           实例类型参数3
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2, P3> Stream<T> getSortedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
    return getServiceStream(serviceClass, p1, p2, p3).sorted(SPI::sortByPriority);
  }

  /**
   * 返回根据 Priority 反向排序后的实例列表
   *
   * @param serviceClass 实例类型
   * @param p1           实例类型参数1
   * @param p2           实例类型参数2
   * @param p3           实例类型参数3
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2, P3> Stream<T> getReversedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
    return getServiceStream(serviceClass, p1, p2, p3).sorted(((Comparator<T>) SPI::sortByPriority).reversed());
  }

  /**
   * 获取指定接口的 SPI 实例流（缓存的）
   *
   * @param serviceClass 实例类型
   * @return serviceClass 的 Stream
   */
  @SuppressWarnings("unchecked")
  public static <T> Stream<T> getCachedServiceStream(Class<T> serviceClass) {
    var list = cachedSpiInstanceMap
        .computeIfAbsent(serviceClass.getName(), k -> getSortedServiceStream(serviceClass).collect(Collectors.toList()));
    return (Stream<T>) list.stream();
  }

  /**
   * 返回根据 Priority 排序后的实例流（缓存的）
   *
   * @param serviceClass 实例类型
   * @return serviceClass 的 Stream
   */
  public static <T> Stream<T> getCachedSortedServiceStream(Class<T> serviceClass) {
    return getCachedServiceStream(serviceClass);
  }

  /**
   * 返回根据 Priority 反向排序后的实例流（缓存的）
   *
   * @param serviceClass 实例类型
   * @return serviceClass 的 Stream
   */
  public static <T> Stream<T> getCachedReversedServiceStream(Class<T> serviceClass) {
    return getCachedServiceStream(serviceClass).sorted(((Comparator<T>) SPI::sortByPriority).reversed());
  }

  /**
   * 获取泛型接口的指定类型参数的 SPI 实例流（缓存的）
   *
   * @param serviceClass 泛型接口
   * @param p1           泛型参数
   * @param <T>          泛型接口类型
   * @param <P1>         泛型参数类型
   * @return serviceClass 的 Stream
   */
  public static <T, P1> Stream<T> getCachedServiceStream(Class<T> serviceClass, Class<P1> p1) {
    return getCachedServiceStream(serviceClass)
        .filter(s -> {
          var classes = GenericTypeUtil.getGenericType(s, serviceClass);
          return classes.length > 0 && classes[0].equals(p1);
        });
  }

  /**
   * 返回根据 Priority 排序后的实例流（缓存的）
   *
   * @param serviceClass 泛型接口
   * @param p1           泛型参数
   * @param <T>          泛型接口类型
   * @param <P1>         泛型参数类型
   * @return serviceClass 的 Stream
   */
  public static <T, P1> Stream<T> getCachedSortedServiceStream(Class<T> serviceClass, Class<P1> p1) {
    return getCachedServiceStream(serviceClass, p1).sorted(SPI::sortByPriority);
  }

  /**
   * 返回根据 Priority 反向排序后的实例流（缓存的）
   *
   * @param serviceClass 泛型接口
   * @param p1           泛型参数
   * @param <T>          泛型接口类型
   * @param <P1>         泛型参数类型
   * @return serviceClass 的 Stream
   */
  public static <T, P1> Stream<T> getCachedReversedServiceStream(Class<T> serviceClass, Class<P1> p1) {
    return getCachedServiceStream(serviceClass, p1).sorted(((Comparator<T>) SPI::sortByPriority).reversed());
  }

  /**
   * 获取泛型接口的指定类型参数的 SPI 实例流（缓存的）
   *
   * @param serviceClass 泛型接口
   * @param p1           泛型参数1
   * @param p2           泛型参数2
   * @param <T>          泛型接口类型
   * @param <P1>         泛型参数1类型
   * @param <P2>         泛型参数2类型
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2> Stream<T> getCachedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2) {
    return getCachedServiceStream(serviceClass)
        .filter(s -> {
          var classes = GenericTypeUtil.getGenericType(s, serviceClass);
          return classes.length > 1 && classes[0].equals(p1) && classes[1].equals(p2);
        });
  }

  /**
   * 返回根据 Priority 排序后的实例流（缓存的）
   *
   * @param serviceClass 泛型接口
   * @param p1           泛型参数1
   * @param p2           泛型参数2
   * @param <T>          泛型接口类型
   * @param <P1>         泛型参数1类型
   * @param <P2>         泛型参数2类型
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2> Stream<T> getCachedSortedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2) {
    return getCachedServiceStream(serviceClass, p1, p2).sorted(SPI::sortByPriority);
  }

  /**
   * 返回根据 Priority 反向排序后的实例流（缓存的）
   *
   * @param serviceClass 泛型接口
   * @param p1           泛型参数1
   * @param p2           泛型参数2
   * @param <T>          泛型接口类型
   * @param <P1>         泛型参数1类型
   * @param <P2>         泛型参数2类型
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2> Stream<T> getCachedReversedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2) {
    return getCachedServiceStream(serviceClass, p1, p2).sorted(((Comparator<T>) SPI::sortByPriority).reversed());
  }

  /**
   * 获取泛型接口的指定类型参数的 SPI 实例流（缓存的）
   *
   * @param serviceClass 泛型接口
   * @param p1           泛型参数1
   * @param p2           泛型参数2
   * @param p3           泛型参数3
   * @param <T>          泛型接口类型
   * @param <P1>         泛型参数1类型
   * @param <P2>         泛型参数2类型
   * @param <P3>         泛型参数3类型
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2, P3> Stream<T> getCachedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
    return getCachedServiceStream(serviceClass)
        .filter(s -> {
          var classes = GenericTypeUtil.getGenericType(s, serviceClass);
          return classes.length > 2 && classes[0].equals(p1) && classes[1].equals(p2) && classes[2].equals(p3);
        });
  }

  /**
   * 返回根据 Priority 排序后的实例流（缓存的）
   *
   * @param serviceClass 泛型接口
   * @param p1           泛型参数1
   * @param p2           泛型参数2
   * @param p3           泛型参数3
   * @param <T>          泛型接口类型
   * @param <P1>         泛型参数1类型
   * @param <P2>         泛型参数2类型
   * @param <P3>         泛型参数3类型
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2, P3> Stream<T> getCachedSortedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
    return getCachedServiceStream(serviceClass, p1, p2, p3).sorted(SPI::sortByPriority);
  }

  /**
   * 返回根据 Priority 反向排序后的实例流（缓存的）
   *
   * @param serviceClass 泛型接口
   * @param p1           泛型参数1
   * @param p2           泛型参数2
   * @param p3           泛型参数3
   * @param <T>          泛型接口类型
   * @param <P1>         泛型参数1类型
   * @param <P2>         泛型参数2类型
   * @param <P3>         泛型参数3类型
   * @return serviceClass 的 Stream
   */
  public static <T, P1, P2, P3> Stream<T> getCachedReversedServiceStream(Class<T> serviceClass, Class<P1> p1, Class<P2> p2, Class<P3> p3) {
    return getCachedServiceStream(serviceClass, p1, p2, p3).sorted(((Comparator<T>) SPI::sortByPriority).reversed());
  }

  public static <T> T getPrimaryService(Class<T> serviceClass) {
    return getReversedServiceStream(serviceClass).findFirst().orElse(null);
  }

  public static <T> T getCachedPrimaryService(Class<T> serviceClass) {
    return getCachedReversedServiceStream(serviceClass).findFirst().orElse(null);
  }
}
