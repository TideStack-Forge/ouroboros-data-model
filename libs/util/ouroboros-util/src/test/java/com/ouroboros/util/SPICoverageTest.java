package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import jakarta.annotation.Priority;

import org.junit.Test;

import com.ouroboros.util.config.ObjectMapperConfigurator;
import com.ouroboros.util.map.MapDeserializer;
import com.ouroboros.util.map.MapSerializer;
import com.ouroboros.util.spi.TestGenericService1;
import com.ouroboros.util.spi.TestGenericService2;
import com.ouroboros.util.spi.TestGenericService3;

public class SPICoverageTest {

  @Priority(1)
  static class LowPriority {
  }

  @Priority(10)
  static class HighPriority {
  }

  static class NoPriority {
  }

  @Test
  public void sortsByPriorityAndHandlesDefaults() {
    assertTrue(SPI.sortByPriority(new LowPriority(), new HighPriority()) < 0);
    assertTrue(SPI.sortByPriority(new HighPriority(), new LowPriority()) > 0);
    assertEquals(0, SPI.sortByPriority(new NoPriority(), new NoPriority()));
  }

  @Test
  public void loadsAndCachesConfiguredServices() {
    SPI.cachedSpiInstanceMap.clear();

    var loaded = SPI.getServiceStream(ObjectMapperConfigurator.class).collect(java.util.stream.Collectors.toList());
    assertFalse(loaded.isEmpty());
    assertEquals(loaded.size(), SPI.getSortedServiceStream(ObjectMapperConfigurator.class).collect(java.util.stream.Collectors.toList()).size());
    assertEquals(loaded.size(), SPI.getReversedServiceStream(ObjectMapperConfigurator.class).collect(java.util.stream.Collectors.toList()).size());

    assertEquals(loaded.get(0).getClass(), SPI.getCachedServiceStream(ObjectMapperConfigurator.class).findFirst().get().getClass());
    assertEquals(loaded.get(0).getClass(), SPI.getCachedSortedServiceStream(ObjectMapperConfigurator.class).findFirst().get().getClass());
    assertEquals(SPI.getCachedServiceStream(ObjectMapperConfigurator.class).findFirst().get().getClass(), SPI.getCachedReversedServiceStream(ObjectMapperConfigurator.class).findFirst().get().getClass());
    assertEquals(SPI.getPrimaryService(ObjectMapperConfigurator.class).getClass(), SPI.getReversedServiceStream(ObjectMapperConfigurator.class).findFirst().get().getClass());
    assertEquals(SPI.getCachedPrimaryService(ObjectMapperConfigurator.class).getClass(), SPI.getCachedReversedServiceStream(ObjectMapperConfigurator.class).findFirst().get().getClass());
    assertTrue(SPI.cachedSpiInstanceMap.containsKey(ObjectMapperConfigurator.class.getName()));

    assertEquals(1, MapSerializer.getAllFromSPI().size());
    assertEquals(1, MapDeserializer.getAllFromSPI().size());
  }
  @Test
  public void filtersAndCachesGenericServices() {
    SPI.cachedSpiInstanceMap.clear();
    assertEquals(1, SPI.getCachedServiceStream(TestGenericService1.class, String.class).count());
    assertEquals(1, SPI.getCachedSortedServiceStream(TestGenericService1.class, String.class).count());
    assertEquals(1, SPI.getCachedReversedServiceStream(TestGenericService1.class, String.class).count());
    assertEquals(1, SPI.getCachedServiceStream(TestGenericService2.class, String.class, Integer.class).count());
    assertEquals(1, SPI.getCachedSortedServiceStream(TestGenericService2.class, String.class, Integer.class).count());
    assertEquals(1, SPI.getCachedReversedServiceStream(TestGenericService2.class, String.class, Integer.class).count());
    assertEquals(1, SPI.getCachedServiceStream(TestGenericService3.class, String.class, Integer.class, Long.class).count());
    assertEquals(1, SPI.getCachedSortedServiceStream(TestGenericService3.class, String.class, Integer.class, Long.class).count());
    assertEquals(1, SPI.getCachedReversedServiceStream(TestGenericService3.class, String.class, Integer.class, Long.class).count());
  }
}
