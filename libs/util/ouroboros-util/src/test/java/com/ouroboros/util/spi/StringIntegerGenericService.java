package com.ouroboros.util.spi;

import jakarta.annotation.Priority;

@Priority(3)
public class StringIntegerGenericService implements TestGenericService2<String, Integer> {
  @Override
  public String name() {
    return "string-integer";
  }
}
