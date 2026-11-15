package com.ouroboros.data.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ouroboros.data.util.DataMaps;

public final class Scd2HistoryTestModels {
  public static final String USER_WITH_HISTORY = "UserWithHistory";
  public static final String USER_WITH_HISTORY_HISTORY = "UserWithHistoryHistory";
  public static final String USER_WITH_DIFF_HISTORY = "UserWithDiffHistory";
  public static final String USER_WITH_DIFF_HISTORY_HISTORY = "UserWithDiffHistoryHistory";
  public static final String TABLE_USER_WITH_HISTORY = "t_user_with_history";
  public static final String TABLE_USER_WITH_HISTORY_HISTORY = "t_user_with_history_history";
  public static final String TABLE_USER_WITH_DIFF_HISTORY = "t_user_with_diff_history";
  public static final String TABLE_USER_WITH_DIFF_HISTORY_HISTORY = "t_user_with_diff_history_history";

  private Scd2HistoryTestModels() {
  }

  public static DataModelMeta userWithHistoryMeta() {
    Map<String, Object> historyConfig = new LinkedHashMap<String, Object>();
    historyConfig.put("historyModelFullName", USER_WITH_HISTORY_HISTORY);
    historyConfig.put("ignoreFields", Collections.singletonList("updatedAt"));

    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", USER_WITH_HISTORY,
        "rawName", TABLE_USER_WITH_HISTORY,
        "label", "带历史用户",
        "migrationStrategy", MigrationStrategy.AUTO,
        "enableScd2History", true,
        "scd2HistoryConfig", historyConfig,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "status", "type", "String", "size", 20, "label", "状态", "isNullable", true),
            Map.of("name", "updatedAt", "type", "DateTime", "label", "更新时间", "isNullable", true)))).get();
  }

  public static DataModelMeta userWithHistoryHistoryMeta() {
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", USER_WITH_HISTORY_HISTORY,
        "rawName", TABLE_USER_WITH_HISTORY_HISTORY,
        "label", "带历史用户历史表",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "businessKey", "type", "Long", "label", "业务键"),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "status", "type", "String", "size", 20, "label", "状态", "isNullable", true),
            Map.of("name", "updatedAt", "type", "DateTime", "label", "更新时间", "isNullable", true),
            Map.of("name", "validFrom", "type", "DateTime", "label", "生效开始时间"),
            Map.of("name", "validTo", "type", "DateTime", "label", "生效结束时间", "isNullable", true),
            Map.of("name", "isCurrent", "type", "Boolean", "label", "当前版本"),
            Map.of("name", "opType", "type", "String", "size", 20, "label", "操作类型"),
            Map.of("name", "operator", "type", "String", "size", 100, "label", "操作人", "isNullable", true)))).get();
  }

  public static DataModelMeta userWithDiffHistoryMeta() {
    Map<String, Object> historyConfig = new LinkedHashMap<String, Object>();
    historyConfig.put("historyModelFullName", USER_WITH_DIFF_HISTORY_HISTORY);
    historyConfig.put("storeDiff", true);
    historyConfig.put("ignoreFields", Collections.singletonList("updatedAt"));

    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", USER_WITH_DIFF_HISTORY,
        "rawName", TABLE_USER_WITH_DIFF_HISTORY,
        "label", "带明细历史用户",
        "migrationStrategy", MigrationStrategy.AUTO,
        "enableScd2History", true,
        "scd2HistoryConfig", historyConfig,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "status", "type", "String", "size", 20, "label", "状态", "isNullable", true),
            Map.of("name", "updatedAt", "type", "DateTime", "label", "更新时间", "isNullable", true)))).get();
  }

  public static DataModelMeta userWithDiffHistoryHistoryMeta() {
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", USER_WITH_DIFF_HISTORY_HISTORY,
        "rawName", TABLE_USER_WITH_DIFF_HISTORY_HISTORY,
        "label", "带明细历史用户历史表",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "businessKey", "type", "Long", "label", "业务键"),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "status", "type", "String", "size", 20, "label", "状态", "isNullable", true),
            Map.of("name", "updatedAt", "type", "DateTime", "label", "更新时间", "isNullable", true),
            Map.of("name", "validFrom", "type", "DateTime", "label", "生效开始时间"),
            Map.of("name", "validTo", "type", "DateTime", "label", "生效结束时间", "isNullable", true),
            Map.of("name", "isCurrent", "type", "Boolean", "label", "当前版本"),
            Map.of("name", "opType", "type", "String", "size", 20, "label", "操作类型"),
            Map.of("name", "operator", "type", "String", "size", 100, "label", "操作人", "isNullable", true),
            Map.of("name", "changedFields", "type", "String", "size", 2000, "label", "变更字段", "isNullable", true),
            Map.of("name", "changeSet", "type", "String", "size", 4000, "label", "变更详情", "isNullable", true)))).get();
  }

  public static List<DataModelMeta> allMetas() {
    return Arrays.asList(
        userWithHistoryMeta(),
        userWithHistoryHistoryMeta(),
        userWithDiffHistoryMeta(),
        userWithDiffHistoryHistoryMeta()
    );
  }
}
