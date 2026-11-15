package com.ouroboros.util;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class TestFunctionsJava {

  @Test
  public void testChain() {
    var fn = Functions
        .chain((String str) -> str.trim())
        .next((String str) -> str.toUpperCase())
        .next((String str) -> StringUtils.leftPad(str, 10, ' '))
        .end();
    assertEquals("     HELLO", fn.apply("  hello  "));
  }
}
