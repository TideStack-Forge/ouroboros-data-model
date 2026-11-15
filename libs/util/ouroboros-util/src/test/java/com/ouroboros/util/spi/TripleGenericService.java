package com.ouroboros.util.spi;

import jakarta.annotation.Priority;

@Priority(7)
public class TripleGenericService implements TestGenericService3<String, Integer, Long> {
  @Override
  public String name() {
    return "triple";
  }
}
