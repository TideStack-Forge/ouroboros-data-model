package com.ouroboros.data.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestDslUtil {
  @Test
  public void testBuildPath() {
    var test = "abc__test".substring("abc__test".lastIndexOf("__") + 2);
    assertEquals("test", test);
  }
}
