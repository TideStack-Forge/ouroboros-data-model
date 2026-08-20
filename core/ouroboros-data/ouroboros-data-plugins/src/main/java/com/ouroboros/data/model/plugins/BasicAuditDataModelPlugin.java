package com.ouroboros.data.model.plugins;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginBuilder;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.DataOperationIdentityProviders;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.util.DataMaps;

/**
 * Basic audit plugin for data model create/update operator fields.
 */
public class BasicAuditDataModelPlugin implements DataModelPlugin {

  private static final String PLUGIN_NAME = "BasicAudit";
  private final Config config;
  private static boolean initUpdatedAt;

  public BasicAuditDataModelPlugin(Config config) {
    this.config = config;
  }

  @Override
  public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
    return DataModelPlugin.super.insert(buildInsertData(data), context);
  }

  @Override
  public Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
    return DataModelPlugin.super.batchInsert(dataList.stream().map(this::buildInsertData).collect(Collectors.toList()), context);
  }

  @Override
  public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
    data = new HashMap<>(data);
    DataMaps.remove(data, config.getCreatedByField(), config.getCreatedAtField());

    if (StringUtils.isNotEmpty(config.getUpdatedByField())) {
      data.put(config.getUpdatedByField(), getOperateBy());
    }

    if (StringUtils.isNotEmpty(config.getUpdatedAtField())) {
      data.put(config.getUpdatedAtField(), LocalDateTime.now());
    }

    return DataModelPlugin.super.update(where, data, context);
  }

  private Map<String, Object> buildInsertData(Map<String, Object> data) {
    data = new HashMap<>(data);

    if (StringUtils.isNotEmpty(config.getCreatedByField())) {
      data.put(config.getCreatedByField(), getOperateBy());
    }

    if (StringUtils.isNotEmpty(config.getCreatedAtField())) {
      data.put(config.getCreatedAtField(), LocalDateTime.now());
    }

    if (initUpdatedAt) {
      if (StringUtils.isNotEmpty(config.getUpdatedAtField())) {
        data.put(config.getUpdatedAtField(), LocalDateTime.now());
      }
      if (StringUtils.isNotEmpty(config.getUpdatedByField())) {
        data.put(config.getUpdatedByField(), getOperateBy());
      }
    }
    else {
      if (StringUtils.isNotEmpty(config.getUpdatedByField())) {
        data.remove(config.getUpdatedByField());
      }
      if (StringUtils.isNotEmpty(config.getUpdatedAtField())) {
        data.remove(config.getUpdatedAtField());
      }
    }

    return data;
  }

  private String getOperateBy() {
    return DataOperationIdentityProviders.findCurrentOperator().orElse(null);
  }

  public static class Config {
    private String createdByField = "createdBy";
    private String createdAtField = "createdAt";
    private String updatedByField = "updatedBy";
    private String updatedAtField = "updatedAt";
    private Boolean insertInitUpdatedAt;

    public String getCreatedByField() {
      return createdByField;
    }

    public void setCreatedByField(String createdByField) {
      this.createdByField = createdByField;
    }

    public String getCreatedAtField() {
      return createdAtField;
    }

    public void setCreatedAtField(String createdAtField) {
      this.createdAtField = createdAtField;
    }

    public String getUpdatedByField() {
      return updatedByField;
    }

    public void setUpdatedByField(String updatedByField) {
      this.updatedByField = updatedByField;
    }

    public String getUpdatedAtField() {
      return updatedAtField;
    }

    public void setUpdatedAtField(String updatedAtField) {
      this.updatedAtField = updatedAtField;
    }

    public Boolean getInsertInitUpdatedAt() {
      return insertInitUpdatedAt;
    }

    public void setInsertInitUpdatedAt(Boolean insertInitUpdatedAt) {
      this.insertInitUpdatedAt = insertInitUpdatedAt;
    }
  }

  public static class Builder implements DataModelPluginBuilder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean support(String name) {
      return PLUGIN_NAME.equalsIgnoreCase(name);
    }

    @Override
    public Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> configMap) {
      Try<Config> tryConfig = DataMaps.tryToBean(Config.class, configMap);
      if (!tryConfig.isSuccess()) {
        logger.error("数据模型 {} 审计插件配置参数错误！", dataModel.getFullName());
        return Optional.empty();
      }
      Config config = tryConfig.get();
      if (config.getInsertInitUpdatedAt() != null) {
        initUpdatedAt = config.getInsertInitUpdatedAt();
      }

      List<String> fieldNames = dataModel.getFields().stream().map(DataModelField::getName).collect(Collectors.toList());
      if (!fieldNames.contains(config.getCreatedByField())) {
        logger.error("数据模型 {} 不存在审计字段 {} 请检查！", dataModel.getFullName(), config.getCreatedByField());
        config.setCreatedByField(null);
      }

      if (!fieldNames.contains(config.getCreatedAtField())) {
        logger.error("数据模型 {} 不存在审计字段 {} 请检查！", dataModel.getFullName(), config.getCreatedAtField());
        config.setCreatedAtField(null);
      }

      if (!fieldNames.contains(config.getUpdatedByField())) {
        logger.error("数据模型 {} 不存在审计字段 {} 请检查！", dataModel.getFullName(), config.getUpdatedByField());
        config.setUpdatedByField(null);
      }

      if (!fieldNames.contains(config.getUpdatedAtField())) {
        logger.error("数据模型 {} 不存在审计字段 {} 请检查！", dataModel.getFullName(), config.getUpdatedAtField());
        config.setUpdatedAtField(null);
      }

      return Optional.of(new BasicAuditDataModelPlugin(config));
    }
  }
}
