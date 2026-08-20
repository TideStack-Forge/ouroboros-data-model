package com.ouroboros.data.integration;

import static org.mockito.Mockito.*;

import java.util.Optional;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.RelatedValue;
import com.ouroboros.data.station.DataStation;

/**
 * 集成测试 Mock 工具类
 * <p>
 * 提供通用的 Mock 对象创建方法，避免代码重复。
 * <p>
 * 使用场景:
 * <ul>
 *   <li>集成测试中创建 Mock DataModel</li>
 *   <li>集成测试中创建 Mock DataModelField</li>
 *   <li>集成测试中创建 Mock 关联字段</li>
 * </ul>
 *
 * @since 1.0.0-beta.2
 */
public final class MockTestUtils {

  private MockTestUtils() {
    // Utility class, prevent instantiation
  }

  /**
   * 创建 Mock DataModel
   *
   * @param name        模型名称（如 "Order"）
   * @param tableName   表名称（如 "orders"）
   * @param dataStation DataStation 实例
   * @return Mock DataModel
   */
  public static DataModel createMockModel(String name, String tableName, DataStation dataStation) {
    DataModel model = mock(DataModel.class);
    when(model.getName()).thenReturn(name);
    when(model.getRawName()).thenReturn(tableName);
    when(model.getDataStation()).thenReturn(dataStation);
    return model;
  }

  /**
   * 创建简单字段 Mock（非关联字段）
   *
   * @param fieldName 字段名称
   * @return Mock DataModelField
   */
  public static DataModelField createSimpleField(String fieldName) {
    DataModelField field = mock(DataModelField.class);
    when(field.getName()).thenReturn(fieldName);
    when(field.getValueType()).thenReturn(null); // 非 RelatedValue
    return field;
  }

  /**
   * 创建集合关联字段 Mock（一对多关系）
   * <p>
   * 用于模拟类似 Order.orderItems 的集合关联字段。
   *
   * @param fieldName   字段名称（如 "orderItems"）
   * @param targetModel 目标模型（如 OrderItem Model）
   * @return Mock DataModelField with RelatedValue
   */
  @SuppressWarnings("unchecked")
  public static DataModelField createCollectionRelationField(String fieldName, DataModel targetModel) {
    DataModelField field = mock(DataModelField.class);
    RelatedValue<Object> relatedValue = mock(RelatedValue.class);
    DataModelField keyField = mock(DataModelField.class);
    DataModelField refKeyField = mock(DataModelField.class);

    // 设置 field mock
    doReturn(fieldName).when(field).getName();
    doReturn(relatedValue).when(field).getValueType();

    // 设置 relatedValue mock
    doReturn(targetModel.getName()).when(relatedValue).getReferenceModelName();
    doReturn(Optional.of(targetModel)).when(relatedValue).getReferenceModel();

    // 对于集合关系，key 是主表的主键，referenceKey 是子表的外键
    doReturn("id").when(keyField).getName();
    doReturn(Optional.of(keyField)).when(relatedValue).getKey();

    // 使用 camelCase 命名（统一风格）
    doReturn("orderId").when(refKeyField).getName();
    doReturn(Optional.of(refKeyField)).when(relatedValue).getReferenceKey();

    return field;
  }

  /**
   * 创建单个关联字段 Mock（多对一关系）
   * <p>
   * 用于模拟类似 Order.user 的单个关联字段。
   *
   * @param fieldName   字段名称（如 "user"）
   * @param targetModel 目标模型（如 User Model）
   * @return Mock DataModelField with RelatedValue
   */
  @SuppressWarnings("unchecked")
  public static DataModelField createRelationField(String fieldName, DataModel targetModel) {
    DataModelField field = mock(DataModelField.class);
    RelatedValue<Object> relatedValue = mock(RelatedValue.class);
    DataModelField keyField = mock(DataModelField.class);
    DataModelField refKeyField = mock(DataModelField.class);

    // 设置 field mock
    doReturn(fieldName).when(field).getName();
    doReturn(relatedValue).when(field).getValueType();

    // 设置 relatedValue mock
    doReturn(targetModel.getName()).when(relatedValue).getReferenceModelName();
    doReturn(Optional.of(targetModel)).when(relatedValue).getReferenceModel();

    // 对于单个关系，key 是当前表的外键，referenceKey 是目标表的主键
    // 使用 camelCase 命名（统一风格）
    doReturn(fieldName + "Id").when(keyField).getName();
    doReturn(Optional.of(keyField)).when(relatedValue).getKey();

    doReturn("id").when(refKeyField).getName();
    doReturn(Optional.of(refKeyField)).when(relatedValue).getReferenceKey();

    return field;
  }
}
