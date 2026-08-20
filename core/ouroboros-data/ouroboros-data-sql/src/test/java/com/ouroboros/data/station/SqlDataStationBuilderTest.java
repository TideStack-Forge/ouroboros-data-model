package com.ouroboros.data.station;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class SqlDataStationBuilderTest {

  @Test
  void shouldReturnEmptyForNonSqlType() {
    var define = new DataStationDefine();
    define.setName("demo");
    define.setType("memory");

    var station = new SqlDataStationBuilder().apply(define);
    assertFalse(station.isPresent());
  }
}
