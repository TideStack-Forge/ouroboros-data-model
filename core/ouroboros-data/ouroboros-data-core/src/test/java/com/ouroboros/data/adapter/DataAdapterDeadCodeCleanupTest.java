package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class DataAdapterDeadCodeCleanupTest {

  @Test
  void dataAdapterShouldNotExposeDeprecatedPluginContract() {
    boolean hasWithPlugin = Arrays.stream(DataAdapter.class.getMethods())
        .map(Method::getName)
        .anyMatch("withPlugin"::equals);

    assertFalse(hasWithPlugin);
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("com.ouroboros.data.adapter.DataAdapterPlugin"));
  }
}
