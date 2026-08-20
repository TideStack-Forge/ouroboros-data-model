package com.ouroboros.data.model.plugins;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginBuilder;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.DataOperationIdentityProviders;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;

public class LogicalDeleteDataModelPlugin implements DataModelPlugin {

  private final String isDeletedField;
  private final String deletedAtField;
  private final String deletedByField;

  public LogicalDeleteDataModelPlugin(String isDeletedField, String deletedAtField, String deletedByField) {
    this.isDeletedField = isDeletedField;
    this.deletedAtField = deletedAtField;
    this.deletedByField = deletedByField;
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
    Map<String, Object> updateData = new LinkedHashMap<>(data);
    removeField(updateData, isDeletedField);
    removeField(updateData, deletedAtField);
    removeField(updateData, deletedByField);
    return DataModelPlugin.super.update(where, updateData, context);
  }

  @Override
  public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
    Map<String, Object> patch = new LinkedHashMap<>();
    patch.put(isDeletedField, true);
    if (deletedAtField != null) {
      patch.put(deletedAtField, LocalDateTime.now());
    }
    if (deletedByField != null) {
      patch.put(deletedByField, DataOperationIdentityProviders.findCurrentOperator().orElse(null));
    }
    return DataModelPlugin.super.update(where, patch, context);
  }

  @Override
  public Try<Long> count(QueryStatement statement, DataModelPluginContext context) {
    return DataModelPlugin.super.count(statement.getBuilder()
        .where(wrapLogicalDeleteFilter(statement.getWhere(), rootAlias(statement)))
        .build(), context);
  }

  @Override
  public Try<RecordList> query(QueryStatement statement, DataModelPluginContext context) {
    return DataModelPlugin.super.query(statement.getBuilder()
        .where(wrapLogicalDeleteFilter(statement.getWhere(), rootAlias(statement)))
        .build(), context);
  }

  private Map<String, Object> buildInsertData(Map<String, Object> data) {
    Map<String, Object> insertData = new LinkedHashMap<>(data);
    removeField(insertData, deletedAtField);
    removeField(insertData, deletedByField);
    insertData.put(isDeletedField, false);
    return insertData;
  }

  private SExpression<Boolean> wrapLogicalDeleteFilter(SExpression<Boolean> where, String rootAlias) {
    SExpression<Boolean> normalizedWhere = where == null
        ? SExpression.empty(Boolean.class)
        : where;
    if (isDeletedField == null) {
      return normalizedWhere;
    }
    if (containsField(normalizedWhere, isDeletedField, rootAlias, List.of())) {
      return normalizedWhere;
    }
    SExpression<Boolean> deleteFilter = SExpression.create(
        Operators.EQ,
        SExpression.field(isDeletedField),
        SExpression.constant(false));
    if (normalizedWhere.isEmpty()) {
      return deleteFilter;
    }
    return SExpression.create(Operators.AND, normalizedWhere, deleteFilter);
  }

  private void removeField(Map<String, Object> data, String fieldName) {
    if (fieldName != null) {
      data.remove(fieldName);
    }
  }

  private boolean isRootFieldPath(List<String> path, String fieldName, String rootAlias) {
    if (path.isEmpty() || !fieldName.equals(path.get(path.size() - 1))) {
      return false;
    }
    if (path.size() == 1) {
      return true;
    }
    return rootAlias != null
        && rootAlias.equals(String.join(".", path.subList(0, path.size() - 1)));
  }

  private boolean containsField(SExpression<?> expression, String fieldName, String rootAlias,
                                List<String> pathPrefix) {
    if (expression == null || expression.isEmpty()) {
      return false;
    }
    if (expression.getOperator() == ExtOps.REL_ANY
        || expression.getOperator() == ExtOps.REL_ALL
        || expression.getOperator() == ExtOps.REL_NONE) {
      return false;
    }
    if (expression.getOperator() == Operators.FIELD && !expression.getParams().isEmpty()) {
      List<String> segments = new ArrayList<>(pathPrefix);
      segments.addAll(extractFieldSegments(expression));
      if (isRootFieldPath(segments, fieldName, rootAlias)) {
        return true;
      }
    }
    for (Object param : expression.getParams()) {
      if (param instanceof SExpression<?> nested
          && containsField(nested, fieldName, rootAlias, pathPrefix)) {
        return true;
      }
    }
    return false;
  }

  private List<String> extractFieldSegments(SExpression<?> fieldExpression) {
    return fieldExpression.getParams().stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .flatMap(segment -> List.of(segment.split("\\.")).stream())
        .collect(Collectors.toList());
  }

  private String rootAlias(QueryStatement statement) {
    return statement.getFrom() == null ? null : statement.getFrom().getName();
  }

  @Priority(-100)
  public static class Builder implements DataModelPluginBuilder {

    private static final String PLUGIN_NAME = "LogicalDelete";
    private static final String LEGACY_PLUGIN_NAME = "SoftDelete";
    private static final String CONFIG_IS_DELETED_FIELD = "isDeletedField";
    private static final String CONFIG_DELETED_AT_FIELD = "deletedAtField";
    private static final String CONFIG_DELETED_BY_FIELD = "deletedByField";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean support(String name) {
      return PLUGIN_NAME.equalsIgnoreCase(name) || LEGACY_PLUGIN_NAME.equalsIgnoreCase(name);
    }

    @Override
    public Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> config) {
      String isDeletedField = readStringConfig(config, CONFIG_IS_DELETED_FIELD).orElse("isDeleted");
      String deletedAtField = readStringConfig(config, CONFIG_DELETED_AT_FIELD).orElse(null);
      String deletedByField = readStringConfig(config, CONFIG_DELETED_BY_FIELD).orElse(null);

      List<String> fieldNames = Optional.ofNullable(dataModel.getFields())
          .orElseGet(Collections::emptyList)
          .stream()
          .map(DataModelField::getName)
          .collect(Collectors.toList());
      if (!hasLogicalDeleteField(dataModel, fieldNames, isDeletedField)) {
        return Optional.empty();
      }
      if (!hasAuditField(dataModel, fieldNames, deletedAtField)) {
        deletedAtField = null;
      }
      if (!hasAuditField(dataModel, fieldNames, deletedByField)) {
        deletedByField = null;
      }

      return Optional.of(new LogicalDeleteDataModelPlugin(isDeletedField, deletedAtField, deletedByField));
    }

    private Optional<String> readStringConfig(Map<String, Object> config, String key) {
      if (config == null) {
        return Optional.empty();
      }
      Object value = config.get(key);
      if (value == null) {
        return Optional.empty();
      }
      return Optional.of(String.valueOf(value));
    }

    private boolean hasLogicalDeleteField(DataModel dataModel, List<String> fieldNames, String fieldName) {
      if (fieldName == null || fieldName.isEmpty()) {
        logger.error("数据模型 {} 逻辑删除字段未配置，逻辑删除插件未生效！", dataModel.getFullName());
        return false;
      }
      if (!fieldNames.contains(fieldName)) {
        logger.error("数据模型 {} 不存在逻辑删除字段 {}，逻辑删除插件未生效！", dataModel.getFullName(), fieldName);
        return false;
      }
      return true;
    }

    private boolean hasAuditField(DataModel dataModel, List<String> fieldNames, String fieldName) {
      if (fieldName == null || fieldName.isEmpty()) {
        return true;
      }
      if (!fieldNames.contains(fieldName)) {
        logger.error("数据模型 {} 不存在逻辑删除审计字段 {}，该审计字段未生效！", dataModel.getFullName(), fieldName);
        return false;
      }
      return true;
    }
  }
}
