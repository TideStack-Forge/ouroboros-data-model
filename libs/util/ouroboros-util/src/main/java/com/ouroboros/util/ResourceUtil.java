package com.ouroboros.util;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

@SuppressWarnings("unused")
public class ResourceUtil {
  private static final Logger LOGGER = Logger.getLogger(ResourceUtil.class.getName());

  /**
   * 获取资源文件URL列表
   *
   * @param resourceName 资源文件名，包含路径，如：META-INF/spring.factories
   * @return 资源文件URL列表
   */
  public static List<URL> getResourceUrlList(String resourceName) {
    return getResourceUrlList(ResourceUtil.class.getClassLoader(), resourceName);
  }

  /**
   * 获取指定ClassLoader中的资源文件URL列表
   *
   * @param classLoader  指定ClassLoader
   * @param resourceName 资源文件名，包含路径，如：META-INF/spring.factories
   * @return 资源文件URL列表
   */
  public static List<URL> getResourceUrlList(ClassLoader classLoader, String resourceName) {
    try {
      var resources = classLoader.getResources(resourceName);
      return Collections.list(resources);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "枚举 classpath 资源失败: " + resourceName, e);
      return Collections.emptyList();
    }
  }

  public static List<URL> getResourceUrlListForOverride(String resourceName) {
    return getResourceUrlListForOverride(ResourceUtil.class.getClassLoader(), resourceName);
  }

  public static List<URL> getResourceUrlListForOverride(ClassLoader classLoader, String resourceName) {
    return reversed(getResourceUrlList(classLoader, resourceName));
  }

  /**
   * 获取资源文件内容Stream
   *
   * @param resourceName 资源文件名，包含路径，如：META-INF/spring.factories
   * @return 资源文件内容Stream
   */
  public static Stream<String> getResourceContentStream(String resourceName) {
    return getResourceContentStream(ResourceUtil.class.getClassLoader(), resourceName);
  }

  /**
   * 获取指定ClassLoader中的资源文件内容Stream
   *
   * @param classLoader  指定ClassLoader
   * @param resourceName 资源文件名，包含路径，如：META-INF/spring.factories
   * @return 资源文件内容Stream
   */
  public static Stream<String> getResourceContentStream(ClassLoader classLoader, String resourceName) {
    return getResourceContentLinesList(classLoader, resourceName).stream()
        .map(lines -> lines.collect(Collectors.joining("\n")));
  }

  public static Stream<String> getResourceContentStreamForOverride(String resourceName) {
    return getResourceContentStreamForOverride(ResourceUtil.class.getClassLoader(), resourceName);
  }

  public static Stream<String> getResourceContentStreamForOverride(ClassLoader classLoader, String resourceName) {
    return getResourceContentLinesListForOverride(classLoader, resourceName).stream()
        .map(lines -> lines.collect(Collectors.joining("\n")));
  }

  /**
   * 获取资源文件行Stream列表
   *
   * @param resourceName 资源文件名，包含路径，如：META-INF/spring.factories
   * @return 资源文件行Stream列表
   */
  public static List<Stream<String>> getResourceContentLinesList(String resourceName) {
    return getResourceContentLinesList(ResourceUtil.class.getClassLoader(), resourceName);
  }

  /**
   * 获取指定ClassLoader中的资源文件行Stream列表
   *
   * @param classLoader  指定ClassLoader
   * @param resourceName 资源文件名，包含路径，如：META-INF/spring.factories
   * @return 资源文件行Stream列表
   */
  public static List<Stream<String>> getResourceContentLinesList(ClassLoader classLoader, String resourceName) {
    return getResourceUrlList(classLoader, resourceName).stream()
        .map(ResourceUtil::openResourceLines)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public static List<Stream<String>> getResourceContentLinesListForOverride(String resourceName) {
    return getResourceContentLinesListForOverride(ResourceUtil.class.getClassLoader(), resourceName);
  }

  public static List<Stream<String>> getResourceContentLinesListForOverride(ClassLoader classLoader, String resourceName) {
    return getResourceUrlListForOverride(classLoader, resourceName).stream()
        .map(ResourceUtil::openResourceLines)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private static Stream<String> openResourceLines(URL url) {
    try {
      var inputStream = url.openStream();
      var reader = new BufferedReader(new InputStreamReader(inputStream));
      return reader.lines();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "打开 classpath 资源失败: " + url, e);
      return null;
    }
  }

  private static <T> List<T> reversed(List<T> values) {
    var reversedValues = new ArrayList<>(values);
    Collections.reverse(reversedValues);
    return reversedValues;
  }


  /**
   * 获取指定目录下资源文件 InputStream 列表
   *
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件 InputStream 列表
   */
  public static List<InputStream> getResourceInputStreamFromDirectory(String directory, Pattern filePattern) {
    return getResourceInputStreamFromDirectory(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  /**
   * 获取指定ClassLoader中指定目录下资源文件 InputStream 列表
   *
   * @param classLoader 指定ClassLoader
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件 InputStream 列表
   */
  public static List<InputStream> getResourceInputStreamFromDirectory(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceStreamWithNameFromDirectory(classLoader, directory, filePattern).stream()
        .map(Tuple2::_2)
        .collect(Collectors.toList());
  }

  public static List<InputStream> getResourceInputStreamFromDirectoryForOverride(String directory, Pattern filePattern) {
    return getResourceInputStreamFromDirectoryForOverride(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  public static List<InputStream> getResourceInputStreamFromDirectoryForOverride(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceStreamWithNameFromDirectoryForOverride(classLoader, directory, filePattern).stream()
        .map(Tuple2::_2)
        .collect(Collectors.toList());
  }

  /**
   * 获取指定目录下资源文件的 文件名 + InputStream 列表
   *
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件 文件名 + InputStream 列表
   */
  public static List<Tuple2<String, InputStream>> getResourceStreamWithNameFromDirectory(String directory, Pattern filePattern) {
    return getResourceStreamWithNameFromDirectory(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  /**
   * 获取指定ClassLoader中指定目录下资源文件的 文件名 + InputStream 列表
   *
   * @param classLoader 指定ClassLoader
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件 文件名 + InputStream 列表
   */
  public static List<Tuple2<String, InputStream>> getResourceStreamWithNameFromDirectory(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceStreamWithNameFromDirectory(classLoader, directory, filePattern, false);
  }

  public static List<Tuple2<String, InputStream>> getResourceStreamWithNameFromDirectoryForOverride(String directory, Pattern filePattern) {
    return getResourceStreamWithNameFromDirectoryForOverride(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  public static List<Tuple2<String, InputStream>> getResourceStreamWithNameFromDirectoryForOverride(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceStreamWithNameFromDirectory(classLoader, directory, filePattern, true);
  }

  private static List<Tuple2<String, InputStream>> getResourceStreamWithNameFromDirectory(
      ClassLoader classLoader, String directory, Pattern filePattern, boolean reverseClassPathOrder) {
    try {
      var dirs = classLoader.getResources(directory);
      var dirList = Collections.list(dirs);
      if (reverseClassPathOrder) {
        Collections.reverse(dirList);
      }

      var resources = dirList.stream()
          .flatMap(dirURL -> {
            var protocol = dirURL.getProtocol();
            if ("file".equals(protocol)) {
              // 仅文件协议需要 URI；Spring Boot 的 nested: 目录 URL 无法转换为 URI
              var dirURI = Try.of(dirURL::toURI).get();
              var prefix = dirURI.getPath();
              if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
              }
              var finalPrefix = Pattern.compile("^/[A-Z]+:/.*$").matcher(prefix).matches()
                  ? prefix.substring(1).replace("/", "\\")
                  : prefix;
              return getResourceNameAndStreamListFromFileSystem(dirURI, filePattern)
                  .map(tuple ->
                      tuple.map1(path -> {
                        final var finalPath = path.replace(finalPrefix, "");
                        return "/".equals(File.separator) ? finalPath : finalPath.replace("\\", "/");
                      })
                  );
            }

            if ("jar".equals(protocol)) {
              // 保留原始 URL：Spring Boot 的 nested: 目录 URL 依赖其自带的 StreamHandler，经 URI 往返后会丢失
              return getResourceNameAndInputStreamListFromJar(dirURL, directory, filePattern);
            }

            return Stream.empty();
          })
          .collect(Collectors.toList());
      return resources;
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "枚举 classpath 资源目录失败: " + directory, e);
      return Collections.emptyList();
    }
  }

  /**
   * 获取指定目录下资源文件内容列表
   *
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件文本内容列表
   */
  public static List<String> getResourceContentsFromDirectory(String directory, Pattern filePattern) {
    return getResourceContentsFromDirectory(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  /**
   * 获取指定ClassLoader中指定目录下资源文件内容列表
   *
   * @param classLoader 指定ClassLoader
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件文本内容列表
   */
  public static List<String> getResourceContentsFromDirectory(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceNameAndContentListFromDirectory(classLoader, directory, filePattern).stream()
        .map(Tuple2::_2)
        .collect(Collectors.toList());

  }

  public static List<String> getResourceContentsFromDirectoryForOverride(String directory, Pattern filePattern) {
    return getResourceContentsFromDirectoryForOverride(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  public static List<String> getResourceContentsFromDirectoryForOverride(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceNameAndContentListFromDirectoryForOverride(classLoader, directory, filePattern).stream()
        .map(Tuple2::_2)
        .collect(Collectors.toList());

  }

  /**
   * 获取指定目录下资源文件的 文件名 + 内容 列表
   *
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件的 文件名 + 内容 列表
   */
  public static List<Tuple2<String, String>> getResourceNameAndContentListFromDirectory(String directory, Pattern filePattern) {
    return getResourceNameAndContentListFromDirectory(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  /**
   * 获取指定ClassLoader中指定目录下资源文件的 文件名 + 内容 列表
   *
   * @param classLoader 指定ClassLoader
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件的 文件名 + 内容 列表
   */
  public static List<Tuple2<String, String>> getResourceNameAndContentListFromDirectory(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceStreamWithNameFromDirectory(classLoader, directory, filePattern)
        .stream()
        .map(tuple -> tuple.map2(is ->
            new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n")))
        ).collect(Collectors.toList());
  }

  public static List<Tuple2<String, String>> getResourceNameAndContentListFromDirectoryForOverride(String directory, Pattern filePattern) {
    return getResourceNameAndContentListFromDirectoryForOverride(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  public static List<Tuple2<String, String>> getResourceNameAndContentListFromDirectoryForOverride(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceStreamWithNameFromDirectoryForOverride(classLoader, directory, filePattern)
        .stream()
        .map(tuple -> tuple.map2(is ->
            new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n")))
        ).collect(Collectors.toList());
  }

  /**
   * 获取指定目录下资源文件名列表
   *
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件名列表
   */
  public static List<String> getResourceNamesFromDirectory(String directory, Pattern filePattern) {
    return getResourceNamesFromDirectory(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  /**
   * 获取指定ClassLoader中指定目录下资源文件名列表
   *
   * @param classLoader 指定ClassLoader
   * @param directory   资源文件目录
   * @param filePattern 资源文件名正则表达式，将对资源路径+文件名进行匹配测试
   * @return 资源文件名列表
   */
  public static List<String> getResourceNamesFromDirectory(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceNamesFromDirectory(classLoader, directory, filePattern, false);
  }

  public static List<String> getResourceNamesFromDirectoryForOverride(String directory, Pattern filePattern) {
    return getResourceNamesFromDirectoryForOverride(ResourceUtil.class.getClassLoader(), directory, filePattern);
  }

  public static List<String> getResourceNamesFromDirectoryForOverride(ClassLoader classLoader, String directory, Pattern filePattern) {
    return getResourceNamesFromDirectory(classLoader, directory, filePattern, true);
  }

  private static List<String> getResourceNamesFromDirectory(
      ClassLoader classLoader, String directory, Pattern filePattern, boolean reverseClassPathOrder) {
    try {
      var dirs = classLoader.getResources(directory);
      var dirList = Collections.list(dirs);
      if (reverseClassPathOrder) {
        Collections.reverse(dirList);
      }

      return dirList.stream()
          .flatMap(dirURL -> {
            var protocol = dirURL.getProtocol();
            if ("file".equals(protocol)) {
              // 仅文件协议需要 URI；Spring Boot 的 nested: 目录 URL 无法转换为 URI
              var dirURI = Try.of(dirURL::toURI).get();
              var prefix = dirURI.getPath();
              if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
              }
              var finalPrefix = Pattern.compile("^/[A-Z]+:/.*$").matcher(prefix).matches()
                  ? prefix.substring(1).replace("/", "\\")
                  : prefix;
              return getResourceNameListFromFileSystem(dirURI, filePattern)
                  .map(path -> {
                    final var finalPath = path.replace(finalPrefix, "");
                    return "/".equals(File.separator) ? finalPath : finalPath.replace("\\", "/");
                  });
            }

            if ("jar".equals(protocol)) {
              // 保留原始 URL：Spring Boot 的 nested: 目录 URL 依赖其自带的 StreamHandler，经 URI 往返后会丢失
              return getResourceNameListFromJar(dirURL, directory, filePattern);
            }

            return Stream.empty();
          })
          .collect(Collectors.toList());
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "枚举 classpath 资源目录失败: " + directory, e);
      return Collections.emptyList();
    }
  }

  private static Stream<Tuple2<String, InputStream>> getResourceNameAndStreamListFromFileSystem(URI path, Pattern filePattern) {
    var file = new File(path.getPath());
    if (!file.exists()) {
      return Stream.empty();
    }
    if (file.isFile()) {
      try {
        if (filePattern.matcher(file.getPath()).matches()) {
          return Stream.of(Tuple.of(file.getPath(), new FileInputStream(file)));
        }
        return Stream.empty();
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "打开文件资源失败: " + path, e);
        return Stream.empty();
      }
    }

    var list = file.listFiles();
    if (list == null) {
      return Stream.empty();
    }
    return Arrays.stream(list).flatMap(f -> getResourceNameAndStreamListFromFileSystem(f.toURI(), filePattern));
  }

  private static Stream<String> getResourceNameListFromFileSystem(URI path, Pattern filePattern) {
    var file = new File(path.getPath());
    if (!file.exists()) {
      return Stream.empty();
    }
    if (file.isFile()) {
      if (filePattern.matcher(file.getPath()).matches()) {
        return Stream.of(file.getPath());
      }
      return Stream.empty();
    }

    var list = file.listFiles();
    if (list == null) {
      return Stream.empty();
    }
    return Arrays.stream(list).flatMap(f -> getResourceNameListFromFileSystem(f.toURI(), filePattern));
  }

  private static Stream<String> getResourceNameListFromJar(URL dirURL, String directory, Pattern filePattern) {
    return getResourceNameAndInputStreamListFromJar(dirURL, directory, filePattern)
        .map(Tuple2::_1);
  }

  private static Stream<Tuple2<String, InputStream>> getResourceNameAndInputStreamListFromJar(URL dirURL, String directory,
      Pattern filePattern) {
    return readJarEntries(dirURL, directory, filePattern)
        .stream()
        .map(tuple -> tuple.map2(content -> (InputStream) new ByteArrayInputStream(content)));
  }

  /**
   * 读取 jar URL 所指向目录下的资源条目。目录 URL 可以是 {@code jar:file:...!/dir/}，也可以是
   * Spring Boot 可执行 Jar 使用的 {@code jar:nested:.../app.jar/!BOOT-INF/lib/x.jar!/dir/} 形式。
   *
   * @return 资源名（相对目录）+ 资源内容
   */
  private static List<Tuple2<String, byte[]>> readJarEntries(URL directoryURL, String directory, Pattern filePattern) {
    try {
      var connection = directoryURL.openConnection();
      if (!(connection instanceof JarURLConnection jarConnection)) {
        return Collections.emptyList();
      }
      jarConnection.setUseCaches(false);
      return readEntries(jarConnection, directoryEntryName(directory), filePattern);
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "读取 Jar 资源列表失败: " + directoryURL, e);
      return Collections.emptyList();
    }
  }

  private static List<Tuple2<String, byte[]>> readEntries(JarURLConnection connection, String directoryPath, Pattern filePattern)
      throws IOException {
    var jarFileURL = connection.getJarFileURL();
    // Spring Boot 3 可执行 Jar 的 jarFileURL 形如 nested:/app.jar/!BOOT-INF/lib/x.jar：
    // 外层 Jar 与内层 Jar 之间是 "/!"，内层 Jar 与目录之间是 "!/"，两者不可混用
    if ("nested".equals(jarFileURL.getProtocol())) {
      var nestedPath = schemeSpecificPart(jarFileURL);
      var outerSeparator = nestedPath.indexOf("/!");
      if (outerSeparator < 0) {
        return Collections.emptyList();
      }
      var outerJarPath = decodePath(stripFileScheme(nestedPath.substring(0, outerSeparator)));
      var nestedJarEntry = nestedPath.substring(outerSeparator + 2);
      try (var outerJarFile = new JarFile(outerJarPath);
          var zipInputStream = new ZipInputStream(outerJarFile.getInputStream(outerJarFile.getEntry(nestedJarEntry)))) {
        return readEntriesFromNestedJarStream(zipInputStream, directoryPath, filePattern);
      }
    }
    try (var jarFile = connection.getJarFile()) {
      return readEntriesFromJarFile(jarFile, directoryPath, filePattern);
    }
  }

  private static String stripFileScheme(String path) {
    return path.startsWith("file:") ? path.substring("file:".length()) : path;
  }

  private static List<Tuple2<String, byte[]>> readEntriesFromJarFile(JarFile jarFile, String directoryPath, Pattern filePattern)
      throws IOException {
    var entries = new ArrayList<Tuple2<String, byte[]>>();
    for (var entry : Collections.list(jarFile.entries())) {
      if (entry.getName().startsWith(directoryPath) && !entry.isDirectory() && filePattern.matcher(entry.getName()).matches()) {
        try (var inputStream = jarFile.getInputStream(entry)) {
          entries.add(Tuple.of(entry.getName().substring(directoryPath.length()), inputStream.readAllBytes()));
        }
      }
    }
    return entries;
  }

  private static List<Tuple2<String, byte[]>> readEntriesFromNestedJarStream(ZipInputStream zipInputStream, String directoryPath,
      Pattern filePattern) throws IOException {
    var entries = new ArrayList<Tuple2<String, byte[]>>();
    ZipEntry zipEntry;
    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
      if (zipEntry.getName().startsWith(directoryPath) && !zipEntry.isDirectory() && filePattern.matcher(zipEntry.getName()).matches()) {
        entries.add(Tuple.of(zipEntry.getName().substring(directoryPath.length()), readZipEntry(zipInputStream)));
      }
    }
    return entries;
  }

  private static String directoryEntryName(String directory) {
    if (directory == null || directory.isEmpty()) {
      return "";
    }
    return directory.endsWith("/") ? directory : directory + "/";
  }

  /**
   * 取 URL 的 scheme-specific part。这里基于 {@link URL#toString()} 而不是 {@link URL#toURI()}：
   * 嵌套 Jar 的 spec 含有 {@code !}，经过 URI 往返会被丢弃。
   */
  private static String schemeSpecificPart(URL url) {
    var value = url.toString();
    var schemeSeparator = value.indexOf(':');
    return schemeSeparator < 0 ? value : value.substring(schemeSeparator + 1);
  }

  private static String decodePath(String path) {
    try {
      return URLDecoder.decode(path, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.WARNING, "解码资源路径失败，使用原路径: " + path, e);
      return path;
    }
  }

  private static byte[] readZipEntry(ZipInputStream zipInputStream) throws IOException {
    var outputStream = new ByteArrayOutputStream();
    var buffer = new byte[1024 * 4];
    int read;
    while ((read = zipInputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, read);
    }
    return outputStream.toByteArray();
  }

}
