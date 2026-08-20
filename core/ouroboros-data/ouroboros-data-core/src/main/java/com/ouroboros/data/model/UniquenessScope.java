package com.ouroboros.data.model;

public enum UniquenessScope {
  /**
   * Inherits the model default scope when used by typed model annotations.
   */
  DEFAULT,
  ALL_RECORDS,
  ACTIVE_RECORDS;

  public static final String EXTRA_PROP_NAME = "uniquenessScope";

  public static UniquenessScope fromExtraProp(Object value) {
    if (value == null) {
      return ALL_RECORDS;
    }
    if (value instanceof UniquenessScope scope) {
      if (scope == DEFAULT) {
        return ALL_RECORDS;
      }
      return scope;
    }
    if (value instanceof String name) {
      for (UniquenessScope scope : values()) {
        if (scope.name().equalsIgnoreCase(name.trim())) {
          if (scope == DEFAULT) {
            return ALL_RECORDS;
          }
          return scope;
        }
      }
    }
    return ALL_RECORDS;
  }
}
