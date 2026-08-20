package com.ouroboros.data.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    return ModelMetaBuilder.create(USER_WITH_HISTORY)
        .table(TABLE_USER_WITH_HISTORY)
        .label("带历史用户")
        .migrationStrategy(MigrationStrategy.AUTO)
        .extraProps("enableScd2History", true)
        .extraProps("scd2HistoryConfig", historyConfig)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .stringField("name").size(100).label("用户名").end()
        .stringField("status").size(20).label("状态").nullable().end()
        .dateTimeField("updatedAt").label("更新时间").nullable().end()
        .end()
        .build();
  }

  public static DataModelMeta userWithHistoryHistoryMeta() {
    return ModelMetaBuilder.create(USER_WITH_HISTORY_HISTORY)
        .table(TABLE_USER_WITH_HISTORY_HISTORY)
        .label("带历史用户历史表")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .longField("businessKey").label("业务键").end()
        .stringField("name").size(100).label("用户名").end()
        .stringField("status").size(20).label("状态").nullable().end()
        .dateTimeField("updatedAt").label("更新时间").nullable().end()
        .dateTimeField("validFrom").label("生效开始时间").end()
        .dateTimeField("validTo").label("生效结束时间").nullable().end()
        .booleanField("isCurrent").label("当前版本").end()
        .stringField("opType").size(20).label("操作类型").end()
        .stringField("operator").size(100).label("操作人").nullable().end()
        .end()
        .build();
  }

  public static DataModelMeta userWithDiffHistoryMeta() {
    Map<String, Object> historyConfig = new LinkedHashMap<String, Object>();
    historyConfig.put("historyModelFullName", USER_WITH_DIFF_HISTORY_HISTORY);
    historyConfig.put("storeDiff", true);
    historyConfig.put("ignoreFields", Collections.singletonList("updatedAt"));

    return ModelMetaBuilder.create(USER_WITH_DIFF_HISTORY)
        .table(TABLE_USER_WITH_DIFF_HISTORY)
        .label("带明细历史用户")
        .migrationStrategy(MigrationStrategy.AUTO)
        .extraProps("enableScd2History", true)
        .extraProps("scd2HistoryConfig", historyConfig)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .stringField("name").size(100).label("用户名").end()
        .stringField("status").size(20).label("状态").nullable().end()
        .dateTimeField("updatedAt").label("更新时间").nullable().end()
        .end()
        .build();
  }

  public static DataModelMeta userWithDiffHistoryHistoryMeta() {
    return ModelMetaBuilder.create(USER_WITH_DIFF_HISTORY_HISTORY)
        .table(TABLE_USER_WITH_DIFF_HISTORY_HISTORY)
        .label("带明细历史用户历史表")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .longField("businessKey").label("业务键").end()
        .stringField("name").size(100).label("用户名").end()
        .stringField("status").size(20).label("状态").nullable().end()
        .dateTimeField("updatedAt").label("更新时间").nullable().end()
        .dateTimeField("validFrom").label("生效开始时间").end()
        .dateTimeField("validTo").label("生效结束时间").nullable().end()
        .booleanField("isCurrent").label("当前版本").end()
        .stringField("opType").size(20).label("操作类型").end()
        .stringField("operator").size(100).label("操作人").nullable().end()
        .stringField("changedFields").size(2000).label("变更字段").nullable().end()
        .stringField("changeSet").size(4000).label("变更详情").nullable().end()
        .end()
        .build();
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
