package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PrimaryKeyGeneratorFactoryNoProviderTest {

  @Test
  void getPkGeneratorReturnsEmptyWhenNoRuntimeProviderIsOnClasspath() {
    assertTrue(PrimaryKeyGeneratorFactory.getPkGenerator("snowflake").isEmpty());
    assertTrue(PrimaryKeyGeneratorFactory.getPkGenerator("coding:any").isEmpty());
  }
}
