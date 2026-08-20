package com.ouroboros.data.model;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

import com.ouroboros.data.util.DataServices;

public final class DataOperationIdentityProviders {

  public static final String SYSTEM_OPERATOR = "system";

  private static final ThreadLocal<String> CURRENT_OPERATOR = new ThreadLocal<>();

  private DataOperationIdentityProviders() {
  }

  public static Optional<String> findCurrentOperator() {
    String currentOperator = CURRENT_OPERATOR.get();
    if (currentOperator != null) {
      return Optional.of(currentOperator);
    }

    return DataServices.getCachedReversedServiceStream(DataOperationIdentityProvider.class)
        .map(DataOperationIdentityProvider::findCurrentOperator)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  public static <T> T withCurrentOperator(String operator, Supplier<T> supplier) {
    requireNonNull(operator, "operator must not be null");
    requireNonNull(supplier, "supplier must not be null");

    String previousOperator = CURRENT_OPERATOR.get();
    CURRENT_OPERATOR.set(operator);
    try {
      return supplier.get();
    } finally {
      if (previousOperator == null) {
        CURRENT_OPERATOR.remove();
      } else {
        CURRENT_OPERATOR.set(previousOperator);
      }
    }
  }
}
