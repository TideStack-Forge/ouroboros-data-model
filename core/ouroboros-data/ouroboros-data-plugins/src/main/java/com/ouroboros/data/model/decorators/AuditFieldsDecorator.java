package com.ouroboros.data.model.decorators;

import java.util.*;

import jakarta.annotation.Priority;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.model.DataModelFieldMeta;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.DataModelMetaDecorator;
import com.ouroboros.data.model.PluginDescriptor;

/**
 * 审计字段装饰器
 * <p>
 * 为数据模型自动添加审计字段（创建人、创建时间、更新人、更新时间）和审计插件
 * 当数据模型的扩展属性中包含 enableAuditFields=true 时生效
 */
@Priority(999980)
public class AuditFieldsDecorator implements DataModelMetaDecorator {

  private static final String ENABLE_AUDIT_FIELDS = "enableAuditFields";
  private static final String BASIC_AUDIT_CONFIG = "basicAuditConfig";
  private static final String PLUGIN_BASIC_AUDIT = "BasicAudit";
  private static final String FIELD_CREATED_BY = "createdBy";
  private static final String FIELD_CREATED_AT = "createdAt";
  private static final String FIELD_UPDATED_BY = "updatedBy";
  private static final String FIELD_UPDATED_AT = "updatedAt";
  private static final String CONFIG_CREATED_BY_RAW_NAME = "createdByRawName";
  private static final String CONFIG_CREATED_AT_RAW_NAME = "createdAtRawName";
  private static final String CONFIG_UPDATED_BY_RAW_NAME = "updatedByRawName";
  private static final String CONFIG_UPDATED_AT_RAW_NAME = "updatedAtRawName";

  @Override
  public boolean supports(DataModelMeta originalMeta) {
    // 检查是否启用了审计字段功能
    return originalMeta.getExtraProp(Boolean.class, ENABLE_AUDIT_FIELDS).orElse(false);
  }

  @Override
  public DataModelMeta decorate(DataModelMeta originalMeta) {
    // 创建新的元数据副本
    DataModelMeta decoratedMeta = originalMeta.deepCopy();

    // 添加审计字段
    List<DataModelFieldMeta> newFields = new ArrayList<>();

    var config = new AuditFieldsConfig(originalMeta);
    var auditFields = buildAuditFields(config);

    decoratedMeta.getFields().stream()
        .filter(originalField -> auditFields.stream()
            .noneMatch(field -> field.getName().equals(originalField.getName())))
        .forEach(newFields::add);
    newFields.addAll(auditFields);
    decoratedMeta.setFields(newFields);

    // 添加审计插件描述符
    List<PluginDescriptor> newPlugins = new ArrayList<>(decoratedMeta.getPluginDescriptors());
    newPlugins.add(new PluginDescriptor(PLUGIN_BASIC_AUDIT));
    decoratedMeta.setPluginDescriptors(newPlugins);

    return decoratedMeta;
  }

  /**
   * 构建审计字段
   */
  private List<DataModelFieldMeta> buildAuditFields(AuditFieldsConfig config) {
    List<DataModelFieldMeta> auditFields = new ArrayList<>();

    // 创建人字段
    DataModelFieldMeta createdBy = new DataModelFieldMeta();
    createdBy.setName(FIELD_CREATED_BY);
    if (StringUtils.isNotEmpty(config.createdByRawName)) {
      createdBy.setRawName(config.createdByRawName);
    }
    createdBy.setLabel("创建人");
    createdBy.setType("String");
    createdBy.setIsNullable(false);
    auditFields.add(createdBy);

    // 创建时间字段
    DataModelFieldMeta createdAt = new DataModelFieldMeta();
    createdAt.setName(FIELD_CREATED_AT);
    if (StringUtils.isNotEmpty(config.createdAtRawName)) {
      createdAt.setRawName(config.createdAtRawName);
    }
    createdAt.setLabel("创建时间");
    createdAt.setType("DateTime");
    createdAt.setIsNullable(false);
    auditFields.add(createdAt);

    // 更新人字段
    DataModelFieldMeta updatedBy = new DataModelFieldMeta();
    updatedBy.setName(FIELD_UPDATED_BY);
    if (StringUtils.isNotEmpty(config.updatedByRawName)) {
      updatedBy.setRawName(config.updatedByRawName);
    }
    updatedBy.setLabel("更新人");
    updatedBy.setType("String");
    updatedBy.setIsNullable(true);
    auditFields.add(updatedBy);

    // 更新时间字段
    DataModelFieldMeta updatedAt = new DataModelFieldMeta();
    updatedAt.setName(FIELD_UPDATED_AT);
    if (StringUtils.isNotEmpty(config.updatedAtRawName)) {
      updatedAt.setRawName(config.updatedAtRawName);
    }
    updatedAt.setLabel("更新时间");
    updatedAt.setType("DateTime");
    updatedAt.setIsNullable(true);
    auditFields.add(updatedAt);

    return auditFields;
  }

  private static class AuditFieldsConfig {
    public final String createdByRawName;
    public final String createdAtRawName;
    public final String updatedByRawName;
    public final String updatedAtRawName;

    public AuditFieldsConfig(DataModelMeta meta) {
      Map<?, ?> config = meta.getExtraProp(Map.class, BASIC_AUDIT_CONFIG)
          .orElseGet(Collections::emptyMap);
      createdByRawName = Optional.ofNullable(config.get(CONFIG_CREATED_BY_RAW_NAME))
          .map(String::valueOf)
          .orElse("");
      createdAtRawName = Optional.ofNullable(config.get(CONFIG_CREATED_AT_RAW_NAME))
          .map(String::valueOf)
          .orElse("");
      updatedByRawName = Optional.ofNullable(config.get(CONFIG_UPDATED_BY_RAW_NAME))
          .map(String::valueOf)
          .orElse("");
      updatedAtRawName = Optional.ofNullable(config.get(CONFIG_UPDATED_AT_RAW_NAME))
          .map(String::valueOf)
          .orElse("");
    }
  }

}
