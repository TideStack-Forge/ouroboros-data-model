package com.ouroboros.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceConfigurationError;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@Timeout(10)
class SPICacheInitializationTest {
  @TempDir
  Path temporary;

  public interface Contract {
    String value();
  }

  public static class LeafProvider implements Contract {
    static final AtomicInteger constructions = new AtomicInteger();

    public LeafProvider() {
      constructions.incrementAndGet();
    }

    public String value() {
      return "leaf";
    }
  }

  public static class NestedProvider implements Contract {
    private final Contract dependency;

    public NestedProvider() {
      var loader = (ProviderLoader) Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader(loader.dependency);
        dependency = SPI.getCachedPrimaryService(Contract.class);
      } finally {
        Thread.currentThread().setContextClassLoader(loader);
      }
    }

    public String value() {
      return "nested:" + dependency.value();
    }
  }

  public static class RetryingProvider implements Contract {
    static final AtomicInteger attempts = new AtomicInteger();

    public RetryingProvider() {
      if (attempts.getAndIncrement() == 0) {
        throw new IllegalStateException("fixture initialization failed");
      }
    }

    public String value() {
      return "recovered";
    }
  }

  public static class ClearingProvider implements Contract {
    static final AtomicInteger constructions = new AtomicInteger();

    public ClearingProvider() {
      if (constructions.getAndIncrement() == 0) {
        var loader = (ProviderLoader) Thread.currentThread().getContextClassLoader();
        if (loader.clearAll) {
          SPI.clearCache();
        } else {
          SPI.clearCache(loader);
        }
      }
    }

    public String value() {
      return "cleared";
    }
  }

  public static class ParallelProvider implements Contract {
    static CountDownLatch started;

    public ParallelProvider() throws InterruptedException {
      started.countDown();
      if (!started.await(3, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Independent SPI initializations blocked each other");
      }
    }

    public String value() {
      return "parallel";
    }
  }

  @Test
  void nestedDiscoveryWorksWithExactlyCollidingCacheKeys() throws Exception {
    LeafProvider.constructions.set(0);
    try (var inner = loader("inner", LeafProvider.class);
         var outer = loader("outer", NestedProvider.class)) {
      // Same contract and equal loader hashes collide at every cache table capacity.
      outer.dependency = inner;
      var provider = discover(outer);
      assertEquals("nested:leaf", provider.value());
      assertSame(provider, discover(outer));
      assertEquals(1, LeafProvider.constructions.get());
      assertEquals("leaf", discover(inner).value());
    }
  }

  @Test
  void concurrentColdDiscoveryConstructsOnlyOneProvider() throws Exception {
    LeafProvider.constructions.set(0);
    try (var loader = loader("concurrent", LeafProvider.class);
         var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var start = new CountDownLatch(1);
      var tasks = java.util.stream.IntStream.range(0, 32).mapToObj(index -> executor.submit(() -> {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        return discover(loader);
      })).toList();
      start.countDown();
      var provider = tasks.getFirst().get(5, TimeUnit.SECONDS);
      for (var task : tasks) {
        assertSame(provider, task.get(5, TimeUnit.SECONDS));
      }
      assertEquals("leaf", provider.value());
      assertEquals(1, LeafProvider.constructions.get());
    }
  }

  @Test
  void independentColdDiscoveriesCanInitializeInParallel() throws Exception {
    ParallelProvider.started = new CountDownLatch(2);
    try (var first = loader("parallel-first", ParallelProvider.class);
         var second = loader("parallel-second", ParallelProvider.class);
         var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var firstResult = executor.submit(() -> discover(first));
      var secondResult = executor.submit(() -> discover(second));
      assertEquals("parallel", firstResult.get(5, TimeUnit.SECONDS).value());
      assertEquals("parallel", secondResult.get(5, TimeUnit.SECONDS).value());
    }
  }

  @Test
  void reportsRealCircularDependenciesAndCanRecoverAfterClearing() throws Exception {
    try (var first = loader("cycle-first", NestedProvider.class);
         var second = loader("cycle-second", NestedProvider.class);
         var leaf = loader("cycle-recovery", LeafProvider.class)) {
      first.dependency = second;
      second.dependency = first;
      var failure = assertThrows(ServiceConfigurationError.class, () -> discover(first));
      Throwable cause = failure;
      while (cause.getCause() != null) {
        cause = cause.getCause();
      }
      assertTrue(cause.getMessage().contains("Circular SPI provider initialization"), cause.toString());
      first.dependency = leaf;
      SPI.clearCache(first);
      SPI.clearCache(second);
      assertEquals("nested:leaf", discover(first).value());
    }
  }

  @Test
  void failedConstructionIsNotCachedAndNextCallCanRetry() throws Exception {
    RetryingProvider.attempts.set(0);
    try (var loader = loader("retry", RetryingProvider.class)) {
      var failure = assertThrows(ServiceConfigurationError.class, () -> discover(loader));
      assertEquals("fixture initialization failed", failure.getCause().getMessage());
      var provider = discover(loader);
      assertEquals("recovered", provider.value());
      assertSame(provider, discover(loader));
      assertEquals(2, RetryingProvider.attempts.get());
    }
  }

  @Test
  void clearingDuringConstructionDoesNotRepublishAnInvalidatedProvider() throws Exception {
    for (boolean clearAll : new boolean[] {false, true}) {
      ClearingProvider.constructions.set(0);
      try (var loader = loader("clear-" + clearAll, ClearingProvider.class)) {
        loader.clearAll = clearAll;
        var beforeClear = discover(loader);
        var afterClear = discover(loader);
        assertEquals("cleared", afterClear.value());
        assertNotSame(beforeClear, afterClear);
        assertSame(afterClear, discover(loader));
        assertEquals(2, ClearingProvider.constructions.get());
      }
    }
  }

  private Contract discover(ProviderLoader loader) {
    var original = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(loader);
      return SPI.getCachedPrimaryService(Contract.class);
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  private ProviderLoader loader(String name, Class<? extends Contract> provider) throws Exception {
    Path root = temporary.resolve(name);
    Path descriptor = root.resolve("META-INF/services/" + Contract.class.getName());
    Files.createDirectories(descriptor.getParent());
    Files.writeString(descriptor, provider.getName() + "\n");
    return new ProviderLoader(root.toUri().toURL(), getClass().getClassLoader());
  }

  private static final class ProviderLoader extends URLClassLoader {
    private ClassLoader dependency;
    private boolean clearAll;

    private ProviderLoader(URL root, ClassLoader parent) {
      super(new URL[] {root}, parent);
    }

    @Override
    public int hashCode() {
      return 42;
    }

    @Override
    public void close() throws java.io.IOException {
      SPI.clearCache(this);
      super.close();
    }
  }
}
