package com.ouroboros.data.model;

import java.util.List;

/** A successful mutation performed through a DataModel write API. */
public record DataModelMutation(String modelFullName,
                                String operation,
                                List<String> objectIds,
                                long affectedRows) {
  public DataModelMutation {
    objectIds = objectIds == null ? List.of() : List.copyOf(objectIds);
  }
}
