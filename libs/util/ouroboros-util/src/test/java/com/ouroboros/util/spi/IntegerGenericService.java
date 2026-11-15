package com.ouroboros.util.spi;

import jakarta.annotation.Priority;

@Priority(1)
public class IntegerGenericService implements TestGenericService1<Integer> {
  @Override
  public String name() {
    return "integer";
  }
}
