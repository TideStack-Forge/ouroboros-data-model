package com.ouroboros.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SPIClassLoaderIsolationTest {
  @TempDir
  Path temporary;

  public interface Contract {
    String value();
  }

  public static class ProviderA implements Contract {
    public String value() { return "A"; }
  }

  public static class ProviderB implements Contract {
    public String value() { return "B"; }
  }

  @Test
  void cachedDiscoveryFollowsTheContextLoaderAndClearingOneLoaderPreservesTheOther() throws Exception {
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (var first = loader("first", ProviderA.class); var second = loader("second", ProviderB.class)) {
      try {
        Thread.currentThread().setContextClassLoader(first);
        Contract firstProvider = SPI.getCachedPrimaryService(Contract.class);
        assertEquals("A", firstProvider.value());
        assertSame(firstProvider, SPI.getCachedPrimaryService(Contract.class));

        Thread.currentThread().setContextClassLoader(second);
        assertEquals("B", ServiceLoader.load(Contract.class).findFirst().orElseThrow().value());
        Contract secondProvider = SPI.getCachedPrimaryService(Contract.class);
        assertEquals("B", secondProvider.value());

        SPI.clearCache(first);
        assertSame(secondProvider, SPI.getCachedPrimaryService(Contract.class));
        Thread.currentThread().setContextClassLoader(first);
        assertNotSame(firstProvider, SPI.getCachedPrimaryService(Contract.class));
      } finally {
        Thread.currentThread().setContextClassLoader(original);
        SPI.clearCache(first);
        SPI.clearCache(second);
      }
    }
  }

  @Test
  void concurrentDiscoverySharesOneProviderWithinTheSameLoader() throws Exception {
    try (var loader = loader("parallel", ProviderA.class)) {
      try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var tasks = IntStream.range(0, 24).mapToObj(index ->
            (java.util.concurrent.Callable<Contract>) () -> {
              Thread.currentThread().setContextClassLoader(loader);
              return SPI.getCachedPrimaryService(Contract.class);
            }).toList();
        var results = executor.invokeAll(tasks);
        Contract provider = results.getFirst().get();
        for (var result : results) {
          assertSame(provider, result.get());
          assertEquals("A", result.get().value());
        }
      } finally {
        SPI.clearCache(loader);
      }
    }
  }

  private URLClassLoader loader(String name, Class<? extends Contract> provider) throws Exception {
    Path root = temporary.resolve(name);
    Path descriptor = root.resolve("META-INF/services/" + Contract.class.getName());
    Files.createDirectories(descriptor.getParent());
    Files.write(descriptor, List.of(provider.getName()));
    return new URLClassLoader(new URL[] {root.toUri().toURL()}, getClass().getClassLoader());
  }

  @Test
  void contractsWithTheSameNameInDifferentLoadersKeepTheirOwnProviders() throws Exception {
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (var first = isolatedLoader("isolated-first", "first");
         var second = isolatedLoader("isolated-second", "second")) {
      try {
        Class<?> firstContract = first.loadClass("fixture.Contract");
        Class<?> secondContract = second.loadClass("fixture.Contract");
        assertNotSame(firstContract, secondContract);
        Thread.currentThread().setContextClassLoader(first);
        Object firstProvider = SPI.getCachedPrimaryService(firstContract);
        assertSame(firstContract.getClassLoader(), firstProvider.getClass().getClassLoader());
        assertEquals("first", firstContract.getMethod("value").invoke(firstProvider));
        Thread.currentThread().setContextClassLoader(second);
        Object secondProvider = SPI.getCachedPrimaryService(secondContract);
        assertEquals("second", secondContract.getMethod("value").invoke(secondProvider));
        SPI.clearCache(first);
        assertSame(secondProvider, SPI.getCachedPrimaryService(secondContract));
        Thread.currentThread().setContextClassLoader(first);
        assertNotSame(firstProvider, SPI.getCachedPrimaryService(firstContract));
      } finally {
        Thread.currentThread().setContextClassLoader(original);
        SPI.clearCache(first);
        SPI.clearCache(second);
      }
    }
  }

  private URLClassLoader isolatedLoader(String name, String value) throws Exception {
    Path root = temporary.resolve(name);
    Path source = root.resolve("fixture");
    Files.createDirectories(source);
    Path contract = source.resolve("Contract.java");
    Path provider = source.resolve("Provider.java");
    Files.writeString(contract, "package fixture; public interface Contract { String value(); }");
    Files.writeString(provider, "package fixture; public class Provider implements Contract { public String value() { return \""
        + value + "\"; } }");
    assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
        "-d", root.toString(), contract.toString(), provider.toString()));
    Path descriptor = root.resolve("META-INF/services/fixture.Contract");
    Files.createDirectories(descriptor.getParent());
    Files.writeString(descriptor, "fixture.Provider\n");
    return new URLClassLoader(new URL[] {root.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
  }
}
