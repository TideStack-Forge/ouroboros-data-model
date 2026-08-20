package com.ouroboros.data.model.query;

import java.util.Objects;

import com.ouroboros.data.model.TypedDataModel;

/**
 * Factory for typed runtime query facades.
 */
public final class TypedDataModelQueries {

  private TypedDataModelQueries() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static <PK, M> TypedDataModelQuery<PK, M> from(TypedDataModel<PK, M> typedDataModel) {
    return new DefaultTypedDataModelQuery<>(
        Objects.requireNonNull(typedDataModel, "typedDataModel must not be null"));
  }
}
