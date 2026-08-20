package com.ouroboros.data.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.vavr.control.Try;

public final class DataResources {
  private DataResources() {
  }

  public static List<String> getResourceContentLinesList(String resourceName) {
    var classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = DataResources.class.getClassLoader();
    }
    var input = classLoader.getResourceAsStream(resourceName);
    if (input == null) {
      return Collections.emptyList();
    }
    return Try.withResources(() -> new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)))
        .of(reader -> reader.lines().collect(Collectors.toList()))
        .getOrElse(Collections::emptyList);
  }
}
