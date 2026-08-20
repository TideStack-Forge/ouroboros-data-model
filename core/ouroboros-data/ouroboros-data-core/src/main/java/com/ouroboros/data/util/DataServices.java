package com.ouroboros.data.util;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Priority;

public final class DataServices {
  private static final ConcurrentMap<Class<?>, List<?>> cachedServiceInstanceMap = new ConcurrentHashMap<>();

  private DataServices() {
  }

  public static <T> Stream<T> getServiceStream(Class<T> serviceClass) {
    return StreamSupport.stream(ServiceLoader.load(serviceClass).spliterator(), false);
  }

  public static <T> Stream<T> getSortedServiceStream(Class<T> serviceClass) {
    return getServiceStream(serviceClass).sorted(DataServices::sortByPriority);
  }

  public static <T> Stream<T> getReversedServiceStream(Class<T> serviceClass) {
    return getServiceStream(serviceClass).sorted(DataServices.<T>priorityComparator().reversed());
  }

  public static <T> Stream<T> getCachedServiceStream(Class<T> serviceClass) {
    var services = cachedServiceInstanceMap.computeIfAbsent(serviceClass,
        key -> getSortedServiceStream(serviceClass).toList());
    return services.stream().map(serviceClass::cast);
  }

  public static <T> Stream<T> getCachedSortedServiceStream(Class<T> serviceClass) {
    return getCachedServiceStream(serviceClass);
  }

  public static <T> Stream<T> getCachedReversedServiceStream(Class<T> serviceClass) {
    return getCachedServiceStream(serviceClass).sorted(DataServices.<T>priorityComparator().reversed());
  }

  public static <T> T getPrimaryService(Class<T> serviceClass) {
    return getReversedServiceStream(serviceClass).findFirst().orElse(null);
  }

  public static void clearCache() {
    cachedServiceInstanceMap.clear();
  }

  private static <T> Comparator<T> priorityComparator() {
    return DataServices::sortByPriority;
  }

  public static <T> int sortByPriority(T first, T second) {
    var firstPriority = first.getClass().getAnnotation(Priority.class);
    var secondPriority = second.getClass().getAnnotation(Priority.class);
    return Integer.compare(
        firstPriority == null ? 0 : firstPriority.value(),
        secondPriority == null ? 0 : secondPriority.value()
    );
  }
}
