package com.ouroboros.data.model;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准测试模型定义
 * <p>
 * 提供 Department、User、Order 三表模型，用于集成测试。
 * <p>
 * 关联关系：
 * <pre>
 * Department  ←──(N:1)──  User  ←──(N:1)──  Order
 * </pre>
 *
 * @since 1.0.0-beta.2
 */
public final class TestModels {

  // 模型名称常量
  public static final String DEPARTMENT = "Department";
  public static final String USER = "User";
  public static final String ORDER = "Order";
  public static final String ORDER_ITEM = "OrderItem";
  public static final String REVIEW = "Review";
  public static final String LOGICAL_DELETE_USER = "LogicalDeleteUser";
  public static final String ARCHIVE_DELETE_USER = "ArchiveDeleteUser";
  public static final String ARCHIVE_DELETE_USER_ARCHIVE = "ArchiveDeleteUserArchive";
  public static final String ARCHIVE_DELETE_ROLLBACK_USER = "ArchiveDeleteRollbackUser";
  public static final String ARCHIVE_DELETE_ROLLBACK_USER_ARCHIVE = "ArchiveDeleteRollbackUserArchive";
  // 表名常量
  public static final String TABLE_DEPARTMENT = "t_department";
  public static final String TABLE_USER = "t_user";
  public static final String TABLE_ORDER = "t_order";
  public static final String TABLE_ORDER_ITEM = "t_order_item";
  public static final String TABLE_REVIEW = "t_review";
  public static final String TABLE_LOGICAL_DELETE_USER = "t_soft_delete_user";
  public static final String TABLE_ARCHIVE_DELETE_USER = "t_recycle_bin_user";
  public static final String TABLE_ARCHIVE_DELETE_USER_ARCHIVE = "t_recycle_bin_user_trash";
  public static final String TABLE_ARCHIVE_DELETE_ROLLBACK_USER = "t_recycle_bin_rollback_user";
  public static final String TABLE_ARCHIVE_DELETE_ROLLBACK_USER_ARCHIVE = "t_recycle_bin_rollback_user_trash";
  private TestModels() {
    // Utility class
  }

  /**
   * 部门模型
   * <p>
   * 字段：id, name, code
   */
  public static DataModelMeta departmentMeta() {
    return ModelMetaBuilder.create(DEPARTMENT)
        .table(TABLE_DEPARTMENT)
        .label("部门")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .stringField("name").size(100).label("部门名称").end()
        .stringField("code").size(50).label("部门编码").end()
        .collectionField("users", USER, "departmentId").label("部门用户").end()
        .end()
        .build();
  }

  /**
   * 用户模型
   * <p>
   * 字段：id, name, age, email, status, departmentId, createdAt, department(关联)
   */
  public static DataModelMeta userMeta() {
    return ModelMetaBuilder.create(USER)
        .table(TABLE_USER)
        .label("用户")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .stringField("name").size(100).label("用户名").end()
        .integerField("age").label("年龄").nullable().end()
        .stringField("email").size(200).label("邮箱").nullable().end()
        .stringField("status").size(20).label("状态").nullable().end()
        .longField("departmentId").label("部门ID").end()
        .dateTimeField("createdAt").label("创建时间").nullable().end()
        // 正确的 modelField 定义：使用 key 指定源模型 FK，referenceKey 默认为目标模型主键
        .field("department").type("Model")
        .extraProp("model", DEPARTMENT)
        .extraProp("key", "departmentId")  // 源模型 FK 字段
        .label("所属部门").end()
        .collectionField("orders", ORDER, "userId").label("用户订单").end()
        .end()
        .build();
  }

  /**
   * 订单模型
   * <p>
   * 字段：id, userId, amount, status, createdAt, user(关联), orderItems(关联), reviews(关联)
   */
  public static DataModelMeta orderMeta() {
    return ModelMetaBuilder.create(ORDER)
        .table(TABLE_ORDER)
        .label("订单")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .longField("userId").label("用户ID").end()
        .doubleField("amount").label("金额").end()
        .stringField("status").size(20).label("状态").end()
        .dateTimeField("createdAt").label("创建时间").nullable().end()
        // 正确的 modelField 定义：使用 key 指定源模型 FK，referenceKey 默认为目标模型主键
        .field("user").type("Model")
        .extraProp("model", USER)
        .extraProp("key", "userId")  // 源模型 FK 字段
        .label("所属用户").end()
        .collectionField("orderItems", ORDER_ITEM, "orderId").label("订单明细").end()
        .collectionField("reviews", REVIEW, "orderId").label("订单评论").end()
        .end()
        .build();
  }

  /**
   * 订单明细模型
   * <p>
   * 字段：id, orderId, productName, qty, price, amount, status, order(关联)
   */
  public static DataModelMeta orderItemMeta() {
    return ModelMetaBuilder.create(ORDER_ITEM)
        .table(TABLE_ORDER_ITEM)
        .label("订单明细")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .longField("orderId").label("订单ID").end()
        .stringField("productName").size(100).label("产品名称").end()
        .integerField("qty").label("数量").end()
        .doubleField("price").label("单价").end()
        .doubleField("amount").label("金额").end()
        .stringField("status").size(20).label("状态").end()
        .field("order").type("Model")
        .extraProp("model", ORDER)
        .extraProp("key", "orderId")
        .label("所属订单").end()
        .end()
        .build();
  }

  /**
   * 评价模型
   * <p>
   * 字段：id, orderId, rating, score, comment, order(关联)
   */
  public static DataModelMeta reviewMeta() {
    return ModelMetaBuilder.create(REVIEW)
        .table(TABLE_REVIEW)
        .label("评价")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .longField("orderId").label("订单ID").end()
        .integerField("rating").label("评级").end()
        .doubleField("score").label("评分").end()
        .stringField("comment").size(500).label("评论").nullable().end()
        .field("order").type("Model")
        .extraProp("model", ORDER)
        .extraProp("key", "orderId")
        .label("所属订单").end()
        .end()
        .build();
  }

  public static DataModelMeta logicalDeleteUserMeta() {
    return ModelMetaBuilder.create(LOGICAL_DELETE_USER)
        .table(TABLE_LOGICAL_DELETE_USER)
        .label("逻辑删除用户")
        .migrationStrategy(MigrationStrategy.AUTO)
        .extraProps("enableSoftDelete", true)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .stringField("name").size(100).label("用户名").end()
        .stringField("email").size(200).label("邮箱").nullable().end()
        .end()
        .build();
  }

  public static DataModelMeta archiveDeleteUserMeta() {
    DataModelMeta meta = ModelMetaBuilder.create(ARCHIVE_DELETE_USER)
        .table(TABLE_ARCHIVE_DELETE_USER)
        .label("归档删除用户")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .stringField("name").size(100).label("用户名").end()
        .stringField("email").size(200).label("邮箱").nullable().end()
        .end()
        .build();
    meta.addPluginDescriptor(buildArchiveDeletePlugin(ARCHIVE_DELETE_USER_ARCHIVE));
    return meta;
  }

  public static DataModelMeta archiveDeleteUserArchiveMeta() {
    return ModelMetaBuilder.create(ARCHIVE_DELETE_USER_ARCHIVE)
        .table(TABLE_ARCHIVE_DELETE_USER_ARCHIVE)
        .label("用户归档表")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().label("源主键").end()
        .stringField("name").size(100).label("用户名").end()
        .stringField("email").size(200).label("邮箱").nullable().end()
        .stringField("sourceModel").size(200).label("源模型").end()
        .stringField("sourceDataStation").size(100).label("源数据源").end()
        .stringField("sourcePrimaryKey").size(1000).label("源主键JSON").end()
        .dateTimeField("deletedAt").label("删除时间").end()
        .stringField("deletedBy").size(100).label("删除人").nullable().end()
        .stringField("deleteOperationId").size(64).label("删除操作ID").end()
        .end()
        .build();
  }

  public static DataModelMeta archiveDeleteRollbackUserMeta() {
    DataModelMeta meta = ModelMetaBuilder.create(ARCHIVE_DELETE_ROLLBACK_USER)
        .table(TABLE_ARCHIVE_DELETE_ROLLBACK_USER)
        .label("归档回滚用户")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().autoIncrement().end()
        .stringField("name").size(100).label("用户名").end()
        .stringField("email").size(200).label("邮箱").nullable().end()
        .end()
        .build();
    meta.addPluginDescriptor(buildArchiveDeletePlugin(ARCHIVE_DELETE_ROLLBACK_USER_ARCHIVE));
    return meta;
  }

  public static DataModelMeta archiveDeleteRollbackUserArchiveMeta() {
    return ModelMetaBuilder.create(ARCHIVE_DELETE_ROLLBACK_USER_ARCHIVE)
        .table(TABLE_ARCHIVE_DELETE_ROLLBACK_USER_ARCHIVE)
        .label("归档回滚用户归档表")
        .migrationStrategy(MigrationStrategy.AUTO)
        .fields()
        .longField("id").isPrimaryKey().label("源主键").end()
        .stringField("name").size(100).label("用户名").end()
        .stringField("email").size(200).label("邮箱").nullable().end()
        .stringField("sourceModel").size(200).label("源模型").end()
        .stringField("sourceDataStation").size(100).label("源数据源").end()
        .stringField("sourcePrimaryKey").size(1000).label("源主键JSON").end()
        .dateTimeField("deletedAt").label("删除时间").end()
        .stringField("deletedBy").size(100).label("删除人").rules("notNull").end()
        .stringField("deleteOperationId").size(64).label("删除操作ID").end()
        .end()
        .build();
  }

  /**
   * 获取所有测试模型
   *
   * @return 模型元数据列表
   */
  public static List<DataModelMeta> allMetas() {
    return Arrays.asList(
        departmentMeta(),
        userMeta(),
        orderMeta(),
        orderItemMeta(),
        reviewMeta(),
        logicalDeleteUserMeta(),
        archiveDeleteUserMeta(),
        archiveDeleteUserArchiveMeta(),
        archiveDeleteRollbackUserMeta(),
        archiveDeleteRollbackUserArchiveMeta()
    );
  }

  private static PluginDescriptor buildArchiveDeletePlugin(String archiveModel) {
    Map<String, Object> config = new LinkedHashMap<String, Object>();
    config.put("archiveModel", archiveModel);
    config.put("batchSize", 2);
    return new PluginDescriptor("ArchiveDelete", config);
  }
}
