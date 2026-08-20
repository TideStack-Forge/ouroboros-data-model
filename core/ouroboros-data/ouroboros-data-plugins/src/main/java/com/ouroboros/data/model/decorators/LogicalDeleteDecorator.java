package com.ouroboros.data.model.decorators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Priority;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.model.DataModelFieldMeta;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.DataModelMetaDecorator;
import com.ouroboros.data.model.PluginDescriptor;
import com.ouroboros.data.model.UniquenessScope;

/**
 * 逻辑删除装饰器
 * <p>
 * 为数据模型自动添加逻辑删除相关字段（删除标记、删除人、删除时间）和逻辑删除插件
 * 当数据模型的扩展属性中包含 enableSoftDelete=true 时生效
 */
@Priority(999970)
public class LogicalDeleteDecorator implements DataModelMetaDecorator {

  private static final String LOGICAL_DELETE_ENABLED_PROP = "enableSoftDelete";
  private static final String LOGICAL_DELETE_CONFIG_PROP = "softDeleteConfig";
  private static final String PLUGIN_LOGICAL_DELETE = "LogicalDelete";
  private static final String FIELD_IS_DELETED = "isDeleted";
  private static final String FIELD_DELETED_BY = "deletedBy";
  private static final String FIELD_DELETED_AT = "deletedAt";
  private static final String CONFIG_IS_DELETED_RAW_NAME = "isDeletedRawName";
  private static final String CONFIG_DELETED_BY_RAW_NAME = "deletedByRawName";
  private static final String CONFIG_DELETED_BY_DISABLED = "deletedByDisabled";
  private static final String CONFIG_DELETED_AT_RAW_NAME = "deletedAtRawName";
  private static final String CONFIG_DELETED_AT_DISABLED = "deletedAtDisabled";
  private static final String PLUGIN_KEY_DELETED_BY_FIELD = "deletedByField";
  private static final String PLUGIN_KEY_DELETED_AT_FIELD = "deletedAtField";
  private static final String PLUGIN_KEY_IS_DELETED_FIELD = "isDeletedField";

  @Override
  public boolean supports(DataModelMeta originalMeta) {
    return originalMeta.getExtraProp(Boolean.class, LOGICAL_DELETE_ENABLED_PROP).orElse(false);
  }

  @Override
  public DataModelMeta decorate(DataModelMeta originalMeta) {
    // 创建新的元数据副本
    DataModelMeta decoratedMeta = originalMeta.deepCopy();

    // 添加逻辑删除字段
    List<DataModelFieldMeta> newFields = new ArrayList<>();
    var config = new LogicalDeleteConfig(decoratedMeta);
    var logicalDeleteFields = buildLogicalDeleteFields(config);
    decoratedMeta.getFields()
        .stream()
        .filter(field -> logicalDeleteFields.stream()
            .noneMatch(f -> f.getName().equalsIgnoreCase(field.getName())))
        .forEach(newFields::add);

    newFields.addAll(buildLogicalDeleteFields(config));
    decoratedMeta.setFields(newFields);
    decoratedMeta.removePluginDescriptor(PLUGIN_LOGICAL_DELETE);
    var logicalDeletePluginDescriptor = buildLogicalDeletePluginDescriptor(config);
    decoratedMeta.addPluginDescriptor(logicalDeletePluginDescriptor);
    decoratedMeta.setExtraProp(UniquenessScope.EXTRA_PROP_NAME, UniquenessScope.ACTIVE_RECORDS);

    return decoratedMeta;
  }

  /**
   * 构建逻辑删除字段
   */
  private List<DataModelFieldMeta> buildLogicalDeleteFields(LogicalDeleteConfig config) {

    List<DataModelFieldMeta> fields = new ArrayList<>();

    // 是否已删除字段
    DataModelFieldMeta isDeleted = new DataModelFieldMeta();
    isDeleted.setName(FIELD_IS_DELETED);
    if (StringUtils.isNotEmpty(config.isDeletedRawName)) {
      isDeleted.setRawName(config.isDeletedRawName);
    }
    isDeleted.setLabel("是否已删除");
    isDeleted.setType("Boolean");
    isDeleted.setDefaultValue("false");
    isDeleted.setIsNullable(false);
    fields.add(isDeleted);

    // 删除人字段
    if (!config.deletedByDisabled) {
      DataModelFieldMeta deletedBy = new DataModelFieldMeta();
      deletedBy.setName(FIELD_DELETED_BY);
      if (StringUtils.isNotEmpty(config.deletedByRawName)) {
        deletedBy.setRawName(config.deletedByRawName);
      }
      deletedBy.setLabel("删除人");
      deletedBy.setType("String");
      deletedBy.setIsNullable(true);
      fields.add(deletedBy);
    }

    // 删除时间字段
    if (!config.deletedAtDisabled) {
      DataModelFieldMeta deletedAt = new DataModelFieldMeta();
      deletedAt.setName(FIELD_DELETED_AT);
      if (StringUtils.isNotEmpty(config.deletedAtRawName)) {
        deletedAt.setRawName(config.deletedAtRawName);
      }
      deletedAt.setLabel("删除时间");
      deletedAt.setType("DateTime");
      deletedAt.setIsNullable(true);
      fields.add(deletedAt);
    }

    return fields;
  }

  private PluginDescriptor buildLogicalDeletePluginDescriptor(LogicalDeleteConfig logicalDeleteConfig) {
    Map<String, Object> config = new LinkedHashMap<>();
    if (!logicalDeleteConfig.deletedByDisabled) {
      config.put(PLUGIN_KEY_DELETED_BY_FIELD, FIELD_DELETED_BY);
    }
    if (!logicalDeleteConfig.deletedAtDisabled) {
      config.put(PLUGIN_KEY_DELETED_AT_FIELD, FIELD_DELETED_AT);
    }
    config.put(PLUGIN_KEY_IS_DELETED_FIELD, FIELD_IS_DELETED);
    return new PluginDescriptor(PLUGIN_LOGICAL_DELETE, config);
  }

  private static class LogicalDeleteConfig {
    public final String isDeletedRawName;
    public final String deletedByRawName;
    public final String deletedAtRawName;
    public final boolean deletedByDisabled;
    public final boolean deletedAtDisabled;

    public LogicalDeleteConfig(DataModelMeta meta) {
      var logicalDeleteConfig = readLogicalDeleteConfig(meta);
      isDeletedRawName = Optional.ofNullable(logicalDeleteConfig.get(CONFIG_IS_DELETED_RAW_NAME))
          .map(String::valueOf)
          .orElse("");
      deletedByRawName = Optional.ofNullable(logicalDeleteConfig.get(CONFIG_DELETED_BY_RAW_NAME))
          .map(String::valueOf)
          .orElse("");
      deletedByDisabled = Optional.ofNullable(logicalDeleteConfig.get(CONFIG_DELETED_BY_DISABLED))
          .filter(Boolean.class::isInstance)
          .map(Boolean.class::cast)
          .orElse(false);
      deletedAtRawName = Optional.ofNullable(logicalDeleteConfig.get(CONFIG_DELETED_AT_RAW_NAME))
          .map(String::valueOf)
          .orElse("");
      deletedAtDisabled = Optional.ofNullable(logicalDeleteConfig.get(CONFIG_DELETED_AT_DISABLED))
          .filter(Boolean.class::isInstance)
          .map(Boolean.class::cast)
          .orElse(false);
    }
  }

  private static Map<String, Object> readLogicalDeleteConfig(DataModelMeta meta) {
    Object rawConfig = meta.getExtraProp(LOGICAL_DELETE_CONFIG_PROP).orElse(null);
    if (!(rawConfig instanceof Map<?, ?>)) {
      return Collections.emptyMap();
    }
    Map<?, ?> rawMap = (Map<?, ?>) rawConfig;
    Map<String, Object> config = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
      Object key = entry.getKey();
      if (key instanceof String) {
        config.put((String) key, entry.getValue());
      }
    }
    return config;
  }
}
