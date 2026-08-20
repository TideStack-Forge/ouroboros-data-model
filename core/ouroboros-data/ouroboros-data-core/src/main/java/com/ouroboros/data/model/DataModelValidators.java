package com.ouroboros.data.model;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ouroboros.data.util.DataServices;

/**
 * Executes model definition validators.
 */
public final class DataModelValidators {
  private DataModelValidators() {
  }

  public static void validate(DataModel model) {
    Objects.requireNonNull(model, "model must not be null");
    DataServices.getCachedServiceStream(DataModelValidator.class)
        .sorted(Comparator.comparingInt(DataModelValidator::getOrder)
            .thenComparing(validator -> validator.getClass().getName()))
        .collect(Collectors.toList())
        .forEach(validator -> validator.validate(model));
  }
}
