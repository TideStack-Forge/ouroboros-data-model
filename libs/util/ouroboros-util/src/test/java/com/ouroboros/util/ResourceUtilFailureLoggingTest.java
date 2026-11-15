package com.ouroboros.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourceUtilFailureLoggingTest {
  private final List<LogRecord> records = new ArrayList<>();
  private final Logger logger = Logger.getLogger(ResourceUtil.class.getName());
  private final Handler handler = new Handler() {
    @Override
    public void publish(LogRecord record) {
      records.add(record);
    }

    @Override
    public void flush() {
      records.clear();
    }

    @Override
    public void close() {
      records.clear();
    }
  };

  @BeforeEach
  void attachHandler() {
    logger.addHandler(handler);
  }

  @AfterEach
  void detachHandler() {
    logger.removeHandler(handler);
    handler.close();
  }

  @Test
  void resourceEnumerationFailureIsLoggedAndReturnsEmptyWithoutThrowing() {
    var failure = new IOException("enumeration unavailable");
    var loader = new ClassLoader(null) {
      @Override
      public Enumeration<URL> getResources(String name) throws IOException {
        throw failure;
      }
    };

    assertEquals(List.of(), ResourceUtil.getResourceUrlList(loader, "test/resource.json"));

    assertLogged("test/resource.json", failure);
  }

  @Test
  void directoryEnumerationFailuresAreLoggedAndReturnEmptyWithoutThrowing() {
    var failure = new IOException("directory unavailable");
    var loader = new ClassLoader(null) {
      @Override
      public Enumeration<URL> getResources(String name) throws IOException {
        throw failure;
      }
    };

    assertEquals(List.of(), ResourceUtil.getResourceContentsFromDirectory(loader, "test/metadata", Pattern.compile(".*")));
    assertEquals(List.of(), ResourceUtil.getResourceNamesFromDirectory(loader, "test/metadata", Pattern.compile(".*")));

    assertEquals(2, records.size());
    assertLogged("test/metadata", failure);
  }

  @Test
  void failedResourceIsLoggedAndHealthyResourcesStillLoad() throws Exception {
    var failure = new IOException("resource unavailable");
    var broken = resourceUrl("broken.json", failure);
    var healthy = resourceUrl("healthy.json", null);
    var loader = new ClassLoader(null) {
      @Override
      public Enumeration<URL> getResources(String name) {
        return Collections.enumeration(List.of(broken, healthy));
      }
    };

    assertEquals(List.of("healthy"), ResourceUtil.getResourceContentStream(loader, "test.json").toList());

    assertLogged("broken.json", failure);
  }

  @Test
  void malformedNestedJarPathIsLoggedAndDoesNotBlock() throws Exception {
    var nested = new URL(null, "nested:/missing%invalid/app.jar/!lib.jar", new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL url) throws IOException {
        throw new IOException("nested jar unavailable");
      }
    });
    var directory = new URL(null, "jar:" + nested + "!/test/", new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL url) throws IOException {
        return new java.net.JarURLConnection(new URL("jar:file:/placeholder.jar!/test/")) {
          @Override
          public void connect() {
            connected = true;
          }

          @Override
          public URL getJarFileURL() {
            return nested;
          }

          @Override
          public java.util.jar.JarFile getJarFile() throws IOException {
            throw new IOException("nested jar unavailable");
          }
        };
      }
    });
    var loader = new ClassLoader(null) {
      @Override
      public Enumeration<URL> getResources(String name) {
        return Collections.enumeration(List.of(directory));
      }
    };

    assertTrue(ResourceUtil.getResourceContentsFromDirectory(loader, "test", Pattern.compile(".*")).isEmpty());
    assertTrue(records.stream().anyMatch(record -> record.getMessage().contains("missing%invalid")
        && record.getThrown() instanceof IllegalArgumentException));
    assertTrue(records.stream().anyMatch(record -> record.getMessage().contains(directory.toString())
        && record.getThrown() instanceof IOException));
  }

  private void assertLogged(String resourceName, IOException failure) {
    var matching = records.stream().filter(record -> record.getMessage().contains(resourceName)).toList();
    assertTrue(!matching.isEmpty(), "failure log must identify the resource");
    assertSame(failure, matching.getFirst().getThrown(), "failure log must retain the original stack trace");
  }

  private URL resourceUrl(String name, IOException failure) throws Exception {
    return new URL(null, "memory:/" + name, new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL url) {
        return new URLConnection(url) {
          @Override
          public void connect() throws IOException {
            if (failure != null) {
              throw failure;
            }
            connected = true;
          }

          @Override
          public java.io.InputStream getInputStream() throws IOException {
            connect();
            return new ByteArrayInputStream("healthy".getBytes(StandardCharsets.UTF_8));
          }
        };
      }
    });
  }
}
