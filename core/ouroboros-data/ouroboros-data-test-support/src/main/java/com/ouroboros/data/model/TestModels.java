package com.ouroboros.data.model;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ouroboros.data.util.DataMaps;

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
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", DEPARTMENT,
        "rawName", TABLE_DEPARTMENT,
        "label", "部门",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "name", "type", "String", "size", 100, "label", "部门名称"),
            Map.of("name", "code", "type", "String", "size", 50, "label", "部门编码"),
            Map.of("name", "users", "type", "Collection", "model", USER, "referenceKey", "departmentId", "label", "部门用户")))).get();
  }

  /**
   * 用户模型
   * <p>
   * 字段：id, name, age, email, status, departmentId, createdAt, department(关联)
   */
  public static DataModelMeta userMeta() {
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", USER,
        "rawName", TABLE_USER,
        "label", "用户",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "age", "type", "Integer", "label", "年龄", "isNullable", true),
            Map.of("name", "email", "type", "String", "size", 200, "label", "邮箱", "isNullable", true),
            Map.of("name", "status", "type", "String", "size", 20, "label", "状态", "isNullable", true),
            Map.of("name", "departmentId", "type", "Long", "label", "部门ID"),
            Map.of("name", "createdAt", "type", "DateTime", "label", "创建时间", "isNullable", true),
            Map.of("name", "department", "type", "Model", "model", DEPARTMENT, "key", "departmentId", "label", "所属部门"),
            Map.of("name", "orders", "type", "Collection", "model", ORDER, "referenceKey", "userId", "label", "用户订单")))).get();
  }

  /**
   * 订单模型
   * <p>
   * 字段：id, userId, amount, status, createdAt, user(关联), orderItems(关联), reviews(关联)
   */
  public static DataModelMeta orderMeta() {
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", ORDER,
        "rawName", TABLE_ORDER,
        "label", "订单",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "userId", "type", "Long", "label", "用户ID"),
            Map.of("name", "amount", "type", "Double", "label", "金额"),
            Map.of("name", "status", "type", "String", "size", 20, "label", "状态"),
            Map.of("name", "createdAt", "type", "DateTime", "label", "创建时间", "isNullable", true),
            Map.of("name", "user", "type", "Model", "model", USER, "key", "userId", "label", "所属用户"),
            Map.of("name", "orderItems", "type", "Collection", "model", ORDER_ITEM, "referenceKey", "orderId", "label", "订单明细"),
            Map.of("name", "reviews", "type", "Collection", "model", REVIEW, "referenceKey", "orderId", "label", "订单评论")))).get();
  }

  /**
   * 订单明细模型
   * <p>
   * 字段：id, orderId, productName, qty, price, amount, status, order(关联)
   */
  public static DataModelMeta orderItemMeta() {
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", ORDER_ITEM,
        "rawName", TABLE_ORDER_ITEM,
        "label", "订单明细",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "orderId", "type", "Long", "label", "订单ID"),
            Map.of("name", "productName", "type", "String", "size", 100, "label", "产品名称"),
            Map.of("name", "qty", "type", "Integer", "label", "数量"),
            Map.of("name", "price", "type", "Double", "label", "单价"),
            Map.of("name", "amount", "type", "Double", "label", "金额"),
            Map.of("name", "status", "type", "String", "size", 20, "label", "状态"),
            Map.of("name", "order", "type", "Model", "model", ORDER, "key", "orderId", "label", "所属订单")))).get();
  }

  /**
   * 评价模型
   * <p>
   * 字段：id, orderId, rating, score, comment, order(关联)
   */
  public static DataModelMeta reviewMeta() {
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", REVIEW,
        "rawName", TABLE_REVIEW,
        "label", "评价",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "orderId", "type", "Long", "label", "订单ID"),
            Map.of("name", "rating", "type", "Integer", "label", "评级"),
            Map.of("name", "score", "type", "Double", "label", "评分"),
            Map.of("name", "comment", "type", "String", "size", 500, "label", "评论", "isNullable", true),
            Map.of("name", "order", "type", "Model", "model", ORDER, "key", "orderId", "label", "所属订单")))).get();
  }

  public static DataModelMeta logicalDeleteUserMeta() {
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", LOGICAL_DELETE_USER,
        "rawName", TABLE_LOGICAL_DELETE_USER,
        "label", "逻辑删除用户",
        "migrationStrategy", MigrationStrategy.AUTO,
        "enableSoftDelete", true,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "email", "type", "String", "size", 200, "label", "邮箱", "isNullable", true)))).get();
  }

  public static DataModelMeta archiveDeleteUserMeta() {
    DataModelMeta meta = DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", ARCHIVE_DELETE_USER,
        "rawName", TABLE_ARCHIVE_DELETE_USER,
        "label", "归档删除用户",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "email", "type", "String", "size", 200, "label", "邮箱", "isNullable", true)))).get();
    meta.addPluginDescriptor(buildArchiveDeletePlugin(ARCHIVE_DELETE_USER_ARCHIVE));
    return meta;
  }

  public static DataModelMeta archiveDeleteUserArchiveMeta() {
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", ARCHIVE_DELETE_USER_ARCHIVE,
        "rawName", TABLE_ARCHIVE_DELETE_USER_ARCHIVE,
        "label", "用户归档表",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "label", "源主键"),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "email", "type", "String", "size", 200, "label", "邮箱", "isNullable", true),
            Map.of("name", "sourceModel", "type", "String", "size", 200, "label", "源模型"),
            Map.of("name", "sourceDataStation", "type", "String", "size", 100, "label", "源数据源"),
            Map.of("name", "sourcePrimaryKey", "type", "String", "size", 1000, "label", "源主键JSON"),
            Map.of("name", "deletedAt", "type", "DateTime", "label", "删除时间"),
            Map.of("name", "deletedBy", "type", "String", "size", 100, "label", "删除人", "isNullable", true),
            Map.of("name", "deleteOperationId", "type", "String", "size", 64, "label", "删除操作ID")))).get();
  }

  public static DataModelMeta archiveDeleteRollbackUserMeta() {
    DataModelMeta meta = DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", ARCHIVE_DELETE_ROLLBACK_USER,
        "rawName", TABLE_ARCHIVE_DELETE_ROLLBACK_USER,
        "label", "归档回滚用户",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "isAutoIncrement", true),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "email", "type", "String", "size", 200, "label", "邮箱", "isNullable", true)))).get();
    meta.addPluginDescriptor(buildArchiveDeletePlugin(ARCHIVE_DELETE_ROLLBACK_USER_ARCHIVE));
    return meta;
  }

  public static DataModelMeta archiveDeleteRollbackUserArchiveMeta() {
    return DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", ARCHIVE_DELETE_ROLLBACK_USER_ARCHIVE,
        "rawName", TABLE_ARCHIVE_DELETE_ROLLBACK_USER_ARCHIVE,
        "label", "归档回滚用户归档表",
        "migrationStrategy", MigrationStrategy.AUTO,
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long", "label", "源主键"),
            Map.of("name", "name", "type", "String", "size", 100, "label", "用户名"),
            Map.of("name", "email", "type", "String", "size", 200, "label", "邮箱", "isNullable", true),
            Map.of("name", "sourceModel", "type", "String", "size", 200, "label", "源模型"),
            Map.of("name", "sourceDataStation", "type", "String", "size", 100, "label", "源数据源"),
            Map.of("name", "sourcePrimaryKey", "type", "String", "size", 1000, "label", "源主键JSON"),
            Map.of("name", "deletedAt", "type", "DateTime", "label", "删除时间"),
            Map.of("name", "deletedBy", "type", "String", "size", 100, "label", "删除人", "rules", List.of("notNull")),
            Map.of("name", "deleteOperationId", "type", "String", "size", 64, "label", "删除操作ID")))).get();
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
