package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

import io.vavr.Tuple;
import io.vavr.Tuple2;

public class ResourceUtilCoverageTest {

  @Test
  public void readsClasspathDirectoryResourcesFromFilesystem() {
    var all = Pattern.compile(".*");

    var urls = ResourceUtil.getResourceUrlList("META-INF/services/com.ouroboros.util.map.MapSerializer");
    assertFalse(urls.isEmpty());

    var contents = ResourceUtil.getResourceContentStream("META-INF/services/com.ouroboros.util.map.MapSerializer")
        .collect(Collectors.toList());
    assertEquals(1, contents.size());
    assertTrue(contents.get(0).contains("ModelValueMapTypeConvert"));

    var lines = ResourceUtil.getResourceContentLinesList("META-INF/services/com.ouroboros.util.map.MapDeserializer")
        .get(0)
        .collect(Collectors.toList());
    assertEquals(1, lines.size());

    var names = ResourceUtil.getResourceNamesFromDirectory("META-INF/services", all);
    assertTrue(names.contains("com.ouroboros.util.map.MapSerializer"));
    assertTrue(names.contains("com.ouroboros.util.map.MapDeserializer"));

    var namedContents = ResourceUtil.getResourceContentsFromDirectory("META-INF/services", all);
    assertTrue(namedContents.stream().anyMatch(value -> value.contains("ModelValueMapTypeConvert")));

    var namedPairs = ResourceUtil.getResourceNameAndContentListFromDirectory("META-INF/services", all);
    assertTrue(namedPairs.stream().anyMatch(pair -> pair._1().equals("com.ouroboros.util.map.MapSerializer")));

    var withStreams = ResourceUtil.getResourceStreamWithNameFromDirectory("META-INF/services", all);
    assertTrue(withStreams.stream().anyMatch(pair -> pair._1().equals("com.ouroboros.util.map.MapDeserializer")));
    var streams = ResourceUtil.getResourceInputStreamFromDirectory("META-INF/services", all);
    assertFalse(streams.isEmpty());
  }

  @Test
  public void readsResourcesFromJarUrls() throws Exception {
    var root = Files.createTempDirectory("resource-util-coverage");
    Files.createDirectories(root.resolve("assets/nested"));
    Files.write(root.resolve("assets/sample.txt"), java.util.Collections.singletonList("alpha"), StandardCharsets.UTF_8);
    Files.write(root.resolve("assets/nested/ignored.md"), java.util.Collections.singletonList("beta"), StandardCharsets.UTF_8);

    try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, null)) {
      var txt = Pattern.compile(".*\\.txt");
      assertEquals(1, ResourceUtil.getResourceNamesFromDirectory(loader, "assets", txt).size());
      assertEquals(1, ResourceUtil.getResourceContentsFromDirectory(loader, "assets", txt).size());
      assertEquals(1, ResourceUtil.getResourceNameAndContentListFromDirectory(loader, "assets", txt).size());
      assertEquals(1, ResourceUtil.getResourceStreamWithNameFromDirectory(loader, "assets", txt).size());
      assertEquals(1, ResourceUtil.getResourceInputStreamFromDirectory(loader, "assets", txt).size());
      assertEquals(1, ResourceUtil.getResourceUrlList(loader, "assets/sample.txt").size());
    }
  }

  @Test
  public void concurrentJarReadsDoNotCloseSharedCachedJar() throws Exception {
    var root = Files.createTempDirectory("resource-util-shared-jar");
    var jarPath = root.resolve("metadata.jar");
    writeJar(jarPath, List.of(Tuple.of("metadata/", ""), Tuple.of("metadata/sample.json", "{\"value\":1}")));
    var directoryUrl = new URL("jar:" + jarPath.toUri().toURL() + "!/metadata/");
    var connection = (JarURLConnection) directoryUrl.openConnection();
    var loader = new ClasspathRootClassLoader(directoryUrl);
    var pattern = Pattern.compile(".*\\.json");

    try (var sharedJar = connection.getJarFile(); var executor = Executors.newFixedThreadPool(4)) {
      assertEquals(List.of("{\"value\":1}"), ResourceUtil.getResourceContentsFromDirectory(loader, "metadata", pattern));
      assertEquals("metadata/sample.json", sharedJar.getJarEntry("metadata/sample.json").getName());

      var reads = java.util.stream.IntStream.range(0, 20)
          .mapToObj(index -> executor.submit(() -> ResourceUtil.getResourceContentsFromDirectory(loader, "metadata", pattern)))
          .collect(Collectors.toList());
      for (var read : reads) {
        assertEquals(List.of("{\"value\":1}"), read.get());
      }
      assertEquals("metadata/sample.json", sharedJar.getJarEntry("metadata/sample.json").getName());
    }
  }

  @Test
  public void readsOverrideOrderFromLaterClasspathRootsFirst() throws Exception {
    var extensionRoot = Files.createTempDirectory("resource-util-extension");
    var baseRoot = Files.createTempDirectory("resource-util-base");
    Files.createDirectories(extensionRoot.resolve("assets"));
    Files.createDirectories(baseRoot.resolve("assets"));
    Files.writeString(extensionRoot.resolve("assets/shared.txt"), "extension", StandardCharsets.UTF_8);
    Files.writeString(baseRoot.resolve("assets/shared.txt"), "base", StandardCharsets.UTF_8);

    try (URLClassLoader loader = new URLClassLoader(new URL[]{extensionRoot.toUri().toURL(), baseRoot.toUri().toURL()}, null)) {
      var txt = Pattern.compile(".*shared\\.txt");

      assertEquals(List.of("extension", "base"), readContents(ResourceUtil.getResourceUrlList(loader, "assets/shared.txt")));
      assertEquals(List.of("base", "extension"),
          readContents(ResourceUtil.getResourceUrlListForOverride(loader, "assets/shared.txt")));
      assertEquals(List.of("extension", "base"), ResourceUtil.getResourceContentsFromDirectory(loader, "assets", txt));
      assertEquals(List.of("base", "extension"),
          ResourceUtil.getResourceContentsFromDirectoryForOverride(loader, "assets", txt));
      assertEquals(List.of("base", "extension"),
          ResourceUtil.getResourceContentStreamForOverride(loader, "assets/shared.txt").collect(Collectors.toList()));
    }
  }

  @Test
  public void readsClasspathDirectoryResourcesFromNestedJarUrls() throws Exception {
    var directory = "META-INF/ouroboros/metadata";
    var root = Files.createTempDirectory("resource-util-nested");
    var innerJar = root.resolve("default-types.jar");
    writeJar(innerJar, List.of(
        Tuple.of(directory + "/单行文本.field-type.json", "{\"name\":\"单行文本\"}"),
        Tuple.of(directory + "/整数数字.field-type.json", "{\"name\":\"整数数字\"}"),
        Tuple.of(directory + "/ignored.txt", "ignored")));
    var bootJar = root.resolve("app.jar");
    writeJar(bootJar, List.of(Tuple.of("BOOT-INF/lib/" + innerJar.getFileName(), Files.readAllBytes(innerJar))));

    var nestedDirectory = nestedJarDirectory(bootJar, "BOOT-INF/lib/" + innerJar.getFileName(), directory);
    var loader = new ClasspathRootClassLoader(nestedDirectory);
    var fieldTypePattern = Pattern.compile(".*\\.field-type\\.json");

    assertEquals(2, ResourceUtil.getResourceNamesFromDirectory(loader, directory, fieldTypePattern).size());
    assertEquals(2, ResourceUtil.getResourceContentsFromDirectory(loader, directory, fieldTypePattern).size());
    assertEquals(2, ResourceUtil.getResourceInputStreamFromDirectory(loader, directory, fieldTypePattern).size());

    var namedContents = ResourceUtil.getResourceNameAndContentListFromDirectory(loader, directory, fieldTypePattern);
    assertEquals(List.of("单行文本.field-type.json", "整数数字.field-type.json"),
        namedContents.stream().map(Tuple2::_1).sorted().collect(Collectors.toList()));
    namedContents.forEach(pair -> {
      assertNotEquals(null, pair._2());
      assertFalse("资源名应为目录相对路径: " + pair._1(), pair._1().startsWith("BOOT-INF"));
    });

    var withStreams = ResourceUtil.getResourceStreamWithNameFromDirectory(loader, directory, fieldTypePattern);
    assertEquals(2, withStreams.size());
    for (var pair : withStreams) {
      assertFalse("资源内容不应为空: " + pair._1(), new String(pair._2().readAllBytes(), StandardCharsets.UTF_8).isEmpty());
    }
  }

  private void writeJar(Path jarPath, List<Tuple2<String, ?>> entries) throws IOException {
    try (var jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
      for (var entry : entries) {
        jarOutputStream.putNextEntry(new JarEntry(entry._1()));
        if (entry._2() instanceof String text) {
          jarOutputStream.write(text.getBytes(StandardCharsets.UTF_8));
        } else {
          jarOutputStream.write((byte[]) entry._2());
        }
        jarOutputStream.closeEntry();
      }
    }
  }

  /** 构造 Spring Boot 3 可执行 Jar 的目录 URL：{@code jar:nested:/app.jar/!BOOT-INF/lib/inner.jar!/dir/}。 */
  private URL nestedJarDirectory(Path bootJar, String innerJarEntry, String directory) throws Exception {
    var nestedJarURL = "nested:" + bootJar.toUri().toURL() + "/!" + innerJarEntry;
    var directoryURL = nestedJarURL + "!/" + directory + "/";
    return new URL("jar", null, -1, directoryURL, new NestedJarUrlStreamHandler(nestedJarURL, directory + "/"));
  }

  /**
   * 模拟 Spring Boot 可执行 Jar 的类加载器：只暴露一个 {@code jar:nested:} 形式的目录 URL，
   * 其它资源一律不可见（父加载器为空）。
   */
  private static class ClasspathRootClassLoader extends ClassLoader {
    private final URL root;

    ClasspathRootClassLoader(URL root) {
      super(null);
      this.root = root;
    }

    @Override
    public Enumeration<URL> getResources(String name) {
      var path = root.getPath();
      return path.endsWith("!/" + name) || path.endsWith("!/" + name + "/")
          ? Collections.enumeration(List.of(root))
          : Collections.emptyEnumeration();
    }
  }

  /** 模拟 Spring Boot 的 {@code nested:} 协议处理器：只负责给出 jarFileURL 与目录条目名。 */
  private static class NestedJarUrlStreamHandler extends java.net.URLStreamHandler {
    private final String nestedPath;
    private final String entryName;

    NestedJarUrlStreamHandler(String nestedPath, String entryName) {
      this.nestedPath = nestedPath;
      this.entryName = entryName;
    }

    @Override
    protected java.net.URLConnection openConnection(URL url) throws IOException {
      var jarFileURL = new URL(null, nestedPath, new NestedJarUrlStreamHandler(nestedPath, entryName));
      // 用一个 JDK 可解析的 jar URL 作为父构造入参：JDK 无法解析 nested:，但解析结果会被下面覆盖
      return new JarURLConnection(new URL("jar:file:/carrier.jar!/")) {
        @Override
        public void connect() {
        }

        @Override
        public java.util.jar.JarFile getJarFile() throws IOException {
          throw new UnsupportedOperationException("嵌套 Jar 目录不通过 JarFile 解析");
        }

        @Override
        public URL getJarFileURL() {
          return jarFileURL;
        }

        @Override
        public String getEntryName() {
          return entryName;
        }
      };
    }
  }

  private List<String> readContents(List<URL> urls) throws IOException {
    var contents = new java.util.ArrayList<String>();
    for (URL url : urls) {
      try (var inputStream = url.openStream()) {
        contents.add(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
      }
    }
    return contents;
  }
}
