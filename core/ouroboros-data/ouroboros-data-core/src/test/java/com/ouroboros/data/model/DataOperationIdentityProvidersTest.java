package com.ouroboros.data.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataOperationIdentityProvidersTest {

  @Test
  void withCurrentOperatorExposesOperatorInsideScope() {
    String operator = DataOperationIdentityProviders.withCurrentOperator("operator-1",
        () -> DataOperationIdentityProviders.findCurrentOperator().orElseThrow());

    assertEquals("operator-1", operator);
    assertFalse(DataOperationIdentityProviders.findCurrentOperator().isPresent());
  }

  @Test
  void withCurrentOperatorRestoresOuterOperatorAfterNestedScope() {
    String operator = DataOperationIdentityProviders.withCurrentOperator("outer", () -> {
      String inner = DataOperationIdentityProviders.withCurrentOperator("inner",
          () -> DataOperationIdentityProviders.findCurrentOperator().orElseThrow());

      assertEquals("inner", inner);
      return DataOperationIdentityProviders.findCurrentOperator().orElseThrow();
    });

    assertEquals("outer", operator);
    assertFalse(DataOperationIdentityProviders.findCurrentOperator().isPresent());
  }

  @Test
  void withCurrentOperatorRestoresOperatorAfterFailure() {
    IllegalStateException failure = assertThrows(IllegalStateException.class,
        () -> DataOperationIdentityProviders.withCurrentOperator("operator-1", () -> {
          throw new IllegalStateException("boom");
        }));

    assertEquals("boom", failure.getMessage());
    assertFalse(DataOperationIdentityProviders.findCurrentOperator().isPresent());
  }
}
