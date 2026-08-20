package com.ouroboros.data.fixture;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.TestModels;
import com.ouroboros.data.station.DataStation;

/**
 * 测试数据夹具
 * <p>
 * 提供测试数据的初始化和清理功能。
 * <p>
 * 预置数据：
 * <ul>
 *   <li>2 个部门：技术部(TECH)、人力资源部(HR)</li>
 *   <li>3 个用户：Alice、Bob(技术部)、Charlie(人力资源部)</li>
 *   <li>5 个订单：分布在 Alice、Bob、Charlie</li>
 *   <li>8 条订单明细：覆盖高价、低价、多数量、不同状态</li>
 *   <li>5 条评价：覆盖高分、低分、无评价订单场景</li>
 * </ul>
 *
 * @since 1.0.0-beta.2
 */
public class TestDataFixture {

  private final DataStation<?> dataStation;

  public TestDataFixture(DataStation<?> dataStation) {
    this.dataStation = dataStation;
  }

  /**
   * 初始化基础测试数据
   */
  public void initBaseData() {
    initDepartments();
    initUsers();
    initOrders();
    initOrderItems();
    initReviews();
  }

  private void initDepartments() {
    DataModel dept = getModel(TestModels.DEPARTMENT);

    Map<String, Object> tech = new HashMap<>();
    tech.put("id", Ids.DEPT_TECH);
    tech.put("name", "技术部");
    tech.put("code", "TECH");
    dept.insert(tech).get();

    Map<String, Object> hr = new HashMap<>();
    hr.put("id", Ids.DEPT_HR);
    hr.put("name", "人力资源部");
    hr.put("code", "HR");
    dept.insert(hr).get();
  }

  private void initUsers() {
    DataModel user = getModel(TestModels.USER);

    Map<String, Object> alice = new HashMap<>();
    alice.put("id", Ids.USER_ALICE);
    alice.put("name", "Alice");
    alice.put("age", 28);
    alice.put("email", "alice@test.com");
    alice.put("status", "active");
    alice.put("departmentId", Ids.DEPT_TECH);
    alice.put("createdAt", LocalDateTime.of(2025, 1, 1, 0, 0));
    user.insert(alice).get();

    Map<String, Object> bob = new HashMap<>();
    bob.put("id", Ids.USER_BOB);
    bob.put("name", "Bob");
    bob.put("age", 35);
    bob.put("email", "bob@test.com");
    bob.put("status", "active");
    bob.put("departmentId", Ids.DEPT_TECH);
    bob.put("createdAt", LocalDateTime.of(2025, 6, 1, 0, 0));
    user.insert(bob).get();

    Map<String, Object> charlie = new HashMap<>();
    charlie.put("id", Ids.USER_CHARLIE);
    charlie.put("name", "Charlie");
    charlie.put("age", 22);
    charlie.put("email", "charlie@test.com");
    charlie.put("status", "inactive");
    charlie.put("departmentId", Ids.DEPT_HR);
    charlie.put("createdAt", LocalDateTime.of(2025, 9, 1, 0, 0));
    user.insert(charlie).get();
  }

  private void initOrders() {
    DataModel order = getModel(TestModels.ORDER);

    // Alice 的订单
    insertOrder(order, Ids.ORDER_1, Ids.USER_ALICE, 100.00, "PENDING", LocalDateTime.of(2026, 1, 1, 10, 0));
    insertOrder(order, Ids.ORDER_2, Ids.USER_ALICE, 200.00, "COMPLETED", LocalDateTime.of(2026, 1, 2, 11, 0));

    // Bob 的订单
    insertOrder(order, Ids.ORDER_3, Ids.USER_BOB, 150.00, "PENDING", LocalDateTime.of(2026, 1, 3, 12, 0));

    // Charlie 的订单
    insertOrder(order, Ids.ORDER_4, Ids.USER_CHARLIE, 300.00, "CANCELLED", LocalDateTime.of(2026, 1, 4, 13, 0));
    insertOrder(order, Ids.ORDER_5, Ids.USER_CHARLIE, 250.00, "COMPLETED", LocalDateTime.of(2026, 1, 5, 14, 0));
  }

  private void insertOrder(DataModel order, Long id, Long userId, Double amount, String status, LocalDateTime createdAt) {
    Map<String, Object> data = new HashMap<>();
    data.put("id", id);
    data.put("userId", userId);
    data.put("amount", amount);
    data.put("status", status);
    data.put("createdAt", createdAt);
    order.insert(data).get();
  }

  private void initOrderItems() {
    DataModel orderItem = getModel(TestModels.ORDER_ITEM);

    insertOrderItem(orderItem, Ids.ITEM_1, Ids.ORDER_1, "iPhone", 1, 999.00, 999.00, "SHIPPED");
    insertOrderItem(orderItem, Ids.ITEM_2, Ids.ORDER_1, "Case", 2, 29.00, 58.00, "SHIPPED");
    insertOrderItem(orderItem, Ids.ITEM_3, Ids.ORDER_2, "iPad", 1, 799.00, 799.00, "DELIVERED");
    insertOrderItem(orderItem, Ids.ITEM_4, Ids.ORDER_3, "MacBook", 1, 1999.00, 1999.00, "PENDING");
    insertOrderItem(orderItem, Ids.ITEM_5, Ids.ORDER_3, "Mouse", 1, 79.00, 79.00, "PENDING");
    insertOrderItem(orderItem, Ids.ITEM_6, Ids.ORDER_4, "iPhone", 1, 999.00, 999.00, "CANCELLED");
    insertOrderItem(orderItem, Ids.ITEM_7, Ids.ORDER_5, "AirPods", 2, 249.00, 498.00, "COMPLETED");
    insertOrderItem(orderItem, Ids.ITEM_8, Ids.ORDER_5, "Cable", 3, 19.00, 57.00, "COMPLETED");
  }

  private void insertOrderItem(DataModel orderItem, Long id, Long orderId, String productName, Integer qty,
                               Double price, Double amount, String status) {
    Map<String, Object> data = new HashMap<>();
    data.put("id", id);
    data.put("orderId", orderId);
    data.put("productName", productName);
    data.put("qty", qty);
    data.put("price", price);
    data.put("amount", amount);
    data.put("status", status);
    orderItem.insert(data).get();
  }

  private void initReviews() {
    DataModel review = getModel(TestModels.REVIEW);

    insertReview(review, Ids.REVIEW_1, Ids.ORDER_2, 5, 4.8, "非常满意");
    insertReview(review, Ids.REVIEW_2, Ids.ORDER_3, 4, 4.0, "还不错");
    insertReview(review, Ids.REVIEW_3, Ids.ORDER_5, 5, 4.9, "完美");
    insertReview(review, Ids.REVIEW_4, Ids.ORDER_5, 3, 3.2, "一般般");
    insertReview(review, Ids.REVIEW_5, Ids.ORDER_1, 2, 2.5, "配送太慢");
  }

  private void insertReview(DataModel review, Long id, Long orderId, Integer rating, Double score, String comment) {
    Map<String, Object> data = new HashMap<>();
    data.put("id", id);
    data.put("orderId", orderId);
    data.put("rating", rating);
    data.put("score", score);
    data.put("comment", comment);
    review.insert(data).get();
  }

  /**
   * 清空所有测试数据（保留表结构）
   */
  public void clearAllData() {
    // 按外键依赖顺序删除
    Map<String, Object> deleteAll = new HashMap<>();
    getModel(TestModels.REVIEW).delete(deleteAll);
    getModel(TestModels.ORDER_ITEM).delete(deleteAll);
    getModel(TestModels.ORDER).delete(deleteAll);
    getModel(TestModels.USER).delete(deleteAll);
    getModel(TestModels.DEPARTMENT).delete(deleteAll);
  }

  private DataModel getModel(String name) {
    return dataStation.getDataModel(name)
        .orElseThrow(() -> new IllegalStateException("Model not found: " + name));
  }

  /**
   * 预置数据 ID 常量
   */
  public static final class Ids {
    // 部门
    public static final Long DEPT_TECH = 1L;
    public static final Long DEPT_HR = 2L;

    // 用户
    public static final Long USER_ALICE = 1L;
    public static final Long USER_BOB = 2L;
    public static final Long USER_CHARLIE = 3L;

    // 订单
    public static final Long ORDER_1 = 1L;
    public static final Long ORDER_2 = 2L;
    public static final Long ORDER_3 = 3L;
    public static final Long ORDER_4 = 4L;
    public static final Long ORDER_5 = 5L;

    // 订单明细
    public static final Long ITEM_1 = 1L;
    public static final Long ITEM_2 = 2L;
    public static final Long ITEM_3 = 3L;
    public static final Long ITEM_4 = 4L;
    public static final Long ITEM_5 = 5L;
    public static final Long ITEM_6 = 6L;
    public static final Long ITEM_7 = 7L;
    public static final Long ITEM_8 = 8L;

    // 评价
    public static final Long REVIEW_1 = 1L;
    public static final Long REVIEW_2 = 2L;
    public static final Long REVIEW_3 = 3L;
    public static final Long REVIEW_4 = 4L;
    public static final Long REVIEW_5 = 5L;

    private Ids() {
    }
  }
}
