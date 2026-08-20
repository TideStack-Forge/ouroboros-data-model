package com.ouroboros.data.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.exception.InvalidEntityName;
import com.ouroboros.data.exception.StatementCheckFailure;
import com.ouroboros.data.exception.ValuesError;

class AssertsCoverageTest {

  @Test
  void validNamesAndValuesPass() {
    assertDoesNotThrow(() -> Asserts.assertValidEntityName("user_profile"));
    assertDoesNotThrow(() -> Asserts.assertValidFieldName("user_name"));

    var values = new LinkedHashMap<>();
    values.put("user_name", "alice");
    values.put("age", 18);

    assertDoesNotThrow(() -> Asserts.assertValidValueMap(values));
  }

  @Test
  void invalidNamesAndValuesFailWithMappedExceptions() {
    assertThrows(InvalidEntityName.class, () -> Asserts.assertValidEntityName(""));
    assertThrows(StatementCheckFailure.class, () -> Asserts.assertValidFieldName(""));
    assertThrows(ValuesError.class, () -> Asserts.assertValidValueMap(Collections.emptyMap()));

    var invalidKeyMap = new LinkedHashMap<>();
    invalidKeyMap.put(1, "x");
    assertThrows(ValuesError.class, () -> Asserts.assertValidValueMap(invalidKeyMap));
  }

  @Test
  void assertAllSuccessStopsOnFirstFailure() {
    var success = io.vavr.control.Try.success("ok");
    var failure = io.vavr.control.Try.failure(new IllegalArgumentException("boom"));

    assertDoesNotThrow(() -> Asserts.assertAllSuccess(Arrays.asList(success)));
    assertThrows(IllegalArgumentException.class, () -> Asserts.assertAllSuccess(Arrays.asList(success, failure)));
  }

  @Test
  void invalidPathFormatsAreRejected() {
    assertThrows(InvalidEntityName.class, () -> Asserts.assertValidEntityName("1bad"));
    assertThrows(StatementCheckFailure.class, () -> Asserts.assertValidFieldName("1bad"));
  }
}
