package com.ouroboros.data.model.decorators;

import java.util.stream.Collectors;

import jakarta.annotation.Priority;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.model.DataModelFieldMeta;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.DataModelMetaDecorator;

/**
 * 主键装饰器（代理+缓存模式）
 * <p>
 * 自动配置数据模型的主键相关属性，包括：
 * - 设置主键字段列表
 * - 配置主键生成器
 * - 设置自增字段属性
 */
@Priority(999999)
public class PrimaryKeyDecorator implements DataModelMetaDecorator {

  private static final String IS_PRIMARY_KEY = "isPrimaryKey";
  private static final String PRIMARY_KEY_GENERATOR = "primaryKeyGenerator";
  private static final String IS_AUTO_INCREMENT = "isAutoIncrement";
  private static final String AUTO_INCREMENT = "autoIncrement";

  @Override
  public boolean supports(DataModelMeta originalMeta) {
    if (originalMeta.getPrimaryKeys().size() == 1) {
      return true;
    }
    return originalMeta.getFields().stream().anyMatch(f -> f.getExtraProp(Boolean.class, IS_PRIMARY_KEY).orElse(false));
  }

  @Override
  public DataModelMeta decorate(DataModelMeta originalMeta) {
    var decoratedMeta = originalMeta.deepCopy();
    if (ObjectUtils.isEmpty(decoratedMeta.getPrimaryKeys())) {
      var pkFields = decoratedMeta.getFields().stream()
          .filter(f -> f.getExtraProp(Boolean.class, IS_PRIMARY_KEY).orElse(false))
          .map(DataModelFieldMeta::getName)
          .collect(Collectors.toList());
      decoratedMeta.setPrimaryKeys(pkFields);
    }
    var pkField = decoratedMeta.getPrimaryKeys().get(0);
    var field = decoratedMeta.getFields().stream()
        .filter(f -> pkField.equalsIgnoreCase(f.getName()))
        .findFirst()
        .orElse(null);
    var primaryKeyGenerator = extractPrimaryKeyGenerator(field);
    if (StringUtils.isEmpty(primaryKeyGenerator)) {
      return decoratedMeta;
    }
    if (AUTO_INCREMENT.equalsIgnoreCase(primaryKeyGenerator)) {
      field.setIsAutoIncrement(true);
    } else {
      decoratedMeta.setPrimaryKeyGenerator(primaryKeyGenerator);
    }
    return decoratedMeta;
  }

  /**
   * 从字段配置中提取主键生成器信息
   */
  private String extractPrimaryKeyGenerator(DataModelFieldMeta field) {
    if (field == null) {
      return null;
    }

    if (field.getExtraProps() == null) {
      return null;
    }

    // 直接从字段的扩展属性中获取预配置的主键生成器
    Object generator = field.getExtraProps().get(PRIMARY_KEY_GENERATOR);
    if (generator instanceof String) {
      return (String) generator;
    }

    // 检查是否标记为自增
    if (Boolean.TRUE.equals(field.getExtraProps().get(IS_AUTO_INCREMENT))) {
      return AUTO_INCREMENT;
    }

    return null;
  }
}