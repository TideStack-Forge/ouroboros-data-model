package com.ouroboros.data.dsl.query;

import java.util.Objects;

import com.ouroboros.data.model.DataModel;

/**
 * Factory for runtime-bound data model query facades.
 */
public final class DataModelQueries {

  private DataModelQueries() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static DataModelQuery from(DataModel dataModel) {
    return DefaultDataModelQuery.bound(Objects.requireNonNull(dataModel, "dataModel must not be null"));
  }
}
