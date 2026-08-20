package com.ouroboros.data.model.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.util.DataConverters;

public class Scd2HistoryConfig {
  private final String historyModelFullName;
  private final String businessKeyField;
  private final String validFromField;
  private final String validToField;
  private final String isCurrentField;
  private final String opField;
  private final String operatorField;
  private final boolean storeDiff;
  private final String changedFieldsField;
  private final String changeSetField;
  private final List<String> ignoreFields;
  private final Integer maxRows;

  private Scd2HistoryConfig(String historyModelFullName,
                            String businessKeyField,
                            String validFromField,
                            String validToField,
                            String isCurrentField,
                            String opField,
                            String operatorField,
                            boolean storeDiff,
                            String changedFieldsField,
                            String changeSetField,
                            List<String> ignoreFields,
                            Integer maxRows) {
    this.historyModelFullName = historyModelFullName;
    this.businessKeyField = businessKeyField;
    this.validFromField = validFromField;
    this.validToField = validToField;
    this.isCurrentField = isCurrentField;
    this.opField = opField;
    this.operatorField = operatorField;
    this.storeDiff = storeDiff;
    this.changedFieldsField = changedFieldsField;
    this.changeSetField = changeSetField;
    this.ignoreFields = ignoreFields;
    this.maxRows = maxRows;
  }

  public static Try<Scd2HistoryConfig> from(Map<String, Object> configMap) {
    return Try.of(() -> {
      String historyModelFullName = readString(configMap, "historyModelFullName", null);
      if (historyModelFullName == null || historyModelFullName.trim().isEmpty()) {
        throw new IllegalArgumentException("historyModelFullName is required");
      }

      return new Scd2HistoryConfig(
          historyModelFullName,
          readString(configMap, "businessKeyField", "businessKey"),
          readString(configMap, "validFromField", "validFrom"),
          readString(configMap, "validToField", "validTo"),
          readString(configMap, "isCurrentField", "isCurrent"),
          readString(configMap, "opField", "opType"),
          readString(configMap, "operatorField", "operator"),
          readBoolean(configMap, "storeDiff", false),
          readString(configMap, "changedFieldsField", "changedFields"),
          readString(configMap, "changeSetField", "changeSet"),
          readStringList(configMap.get("ignoreFields")),
          Optional.ofNullable(configMap)
              .map(config -> config.get("maxRows"))
              .map(DataConverters::toInteger)
              .filter(value -> value != null && value > 0)
              .orElse(200)
      );
    });
  }

  public List<String> validateAgainst(DataModel sourceModel, DataModel historyModel) {
    DataModelField primaryKeyField = sourceModel.getPrimaryKeys().isEmpty() ? null : sourceModel.getPrimaryKeys().get(0);
    List<String> requiredFields = new ArrayList<String>();
    requiredFields.addAll(sourceModel.getFields().stream()
        .filter(field -> field.getValueType().isPhysical())
        .filter(field -> primaryKeyField == null || !field.getName().equals(primaryKeyField.getName()))
        .map(DataModelField::getName)
        .collect(Collectors.toList()));
    requiredFields.add(getBusinessKeyField());
    requiredFields.add(getValidFromField());
    requiredFields.add(getValidToField());
    requiredFields.add(getIsCurrentField());
    requiredFields.add(getOpField());
    requiredFields.add(getOperatorField());
    if (isStoreDiff()) {
      requiredFields.add(getChangedFieldsField());
      requiredFields.add(getChangeSetField());
    }

    return requiredFields.stream()
        .distinct()
        .filter(fieldName -> !historyModel.getField(fieldName).isPresent())
        .collect(Collectors.toList());
  }

  public String getHistoryModelFullName() {
    return historyModelFullName;
  }

  public String getBusinessKeyField() {
    return businessKeyField;
  }

  public String getValidFromField() {
    return validFromField;
  }

  public String getValidToField() {
    return validToField;
  }

  public String getIsCurrentField() {
    return isCurrentField;
  }

  public String getOpField() {
    return opField;
  }

  public String getOperatorField() {
    return operatorField;
  }

  public boolean isStoreDiff() {
    return storeDiff;
  }

  public String getChangedFieldsField() {
    return changedFieldsField;
  }

  public String getChangeSetField() {
    return changeSetField;
  }

  public List<String> getIgnoreFields() {
    return ignoreFields;
  }

  public Integer getMaxRows() {
    return maxRows;
  }

  private static String readString(Map<String, Object> configMap, String key, String defaultValue) {
    return Optional.ofNullable(configMap)
        .map(config -> config.get(key))
        .map(DataConverters::toString)
        .filter(value -> value != null && !value.trim().isEmpty())
        .orElse(defaultValue);
  }

  private static boolean readBoolean(Map<String, Object> configMap, String key, boolean defaultValue) {
    return Optional.ofNullable(configMap)
        .map(config -> config.get(key))
        .map(DataConverters::toBoolean)
        .orElse(defaultValue);
  }

  private static List<String> readStringList(Object rawValue) {
    if (rawValue instanceof Collection<?> collection) {
      return collection.stream()
          .map(DataConverters::toString)
          .filter(value -> value != null && !value.trim().isEmpty())
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
