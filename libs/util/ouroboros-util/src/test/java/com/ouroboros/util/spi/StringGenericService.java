package com.ouroboros.util.spi;

import jakarta.annotation.Priority;

@Priority(5)
public class StringGenericService implements TestGenericService1<String> {
  @Override
  public String name() {
    return "string";
  }
}
