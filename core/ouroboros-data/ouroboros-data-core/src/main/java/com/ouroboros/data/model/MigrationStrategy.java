package com.ouroboros.data.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 迁移策略
 *
 * @author liansz
 */
public enum MigrationStrategy {

  /**
   * 自动迁移
   */
  AUTO("auto"),

  /**
   * 不迁移
   */
  DISABLED("disabled");

  private final String value;

  MigrationStrategy(String value) {
    this.value = value;
  }

  @JsonCreator
  public static MigrationStrategy fromValue(String value) {
    for (MigrationStrategy e : MigrationStrategy.values()) {
      if (e.value.equals(value)) {
        return e;
      }
    }
    return DISABLED;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
