package com.ouroboros.data.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DataStationDefineCoverageTest {

  @Test
  void supportsBasicPropertiesAndEquality() {
    var define = new DataStationDefine();
    define.setName("user");
    define.setLabel("User");
    define.setDescription("user station");
    define.setType("sql");
    define.setProperty("dialect", "mysql");

    assertEquals("user", define.getName());
    assertEquals("User", define.getLabel());
    assertEquals("user station", define.getDescription());
    assertEquals("sql", define.getType());
    assertTrue(define.getProperty("dialect").isPresent());
    assertEquals("mysql", define.getProperty(String.class, "dialect").get());

  }

  @Test
  void setPropertiesNullFallsBackToEmptyMap() {
    var define = new DataStationDefine();
    define.setProperties(null);

    assertTrue(define.getProperties().isEmpty());
  }
}
