package com.ouroboros.data.model;

import java.util.*;
import java.util.stream.Collectors;

import com.ouroboros.data.exception.ModelMetadataException;

/**
 * Validates model-level unique constraint definitions.
 */
public class DataModelUniqueConstraintValidator implements DataModelValidator {

  @Override
  public void validate(DataModel model) {
    Set<String> fieldLevelUniqueFields = model.getFields().stream()
        .filter(field -> Boolean.TRUE.equals(field.getIsUnique()))
        .map(field -> DataModelUniqueConstraints.normalizeFieldName(field.getName()))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    Set<String> logicalIds = new LinkedHashSet<>();
    for (DataModelUniqueConstraintMeta constraint : model.getUniqueConstraints()) {
      validateConstraint(model, fieldLevelUniqueFields, logicalIds, constraint);
    }
  }

  private void validateConstraint(
      DataModel model,
      Set<String> fieldLevelUniqueFields,
      Set<String> logicalIds,
      DataModelUniqueConstraintMeta constraint
  ) {
    List<String> fields = constraint.getFields();
    if (fields == null || fields.isEmpty()) {
      throw metadataException(model, "唯一约束字段列表不能为空");
    }

    List<String> normalizedFields = new ArrayList<>();
    Set<String> uniqueFields = new LinkedHashSet<>();
    for (String fieldName : fields) {
      if (fieldName == null || fieldName.isBlank()) {
        throw metadataException(model, "唯一约束字段名不能为空");
      }
      if (model.getField(fieldName).isEmpty()) {
        throw metadataException(model, String.format("唯一约束字段不存在: %s", fieldName));
      }
      String normalizedField = DataModelUniqueConstraints.normalizeFieldName(fieldName);
      if (!uniqueFields.add(normalizedField)) {
        throw metadataException(model, String.format("唯一约束字段重复: %s", fieldName));
      }
      normalizedFields.add(normalizedField);
    }

    if (normalizedFields.size() == 1 && fieldLevelUniqueFields.contains(normalizedFields.get(0))) {
      throw metadataException(model, String.format(
          "模型级单字段唯一约束与字段级唯一重复: %s",
          fields.get(0)
      ));
    }

    String logicalId = logicalId(model, constraint, normalizedFields);
    if (!logicalIds.add(logicalId)) {
      throw metadataException(model, String.format("唯一约束逻辑标识重复: %s", displayName(constraint, fields)));
    }
  }

  private ModelMetadataException metadataException(DataModel model, String message) {
    return new ModelMetadataException(message, model.getFullName());
  }

  private String logicalId(
      DataModel model,
      DataModelUniqueConstraintMeta constraint,
      List<String> normalizedFields
  ) {
    return DataModelUniqueConstraints.logicalId(
        DataModelUniqueConstraint.Source.MODEL,
        constraint.getName(),
        normalizedFields,
        DataModelUniqueConstraints.modelConstraintScope(model, constraint)
    ).toLowerCase(Locale.ROOT);
  }

  private String displayName(DataModelUniqueConstraintMeta constraint, List<String> fields) {
    String name = constraint.getName();
    if (name != null && !name.isBlank()) {
      return name.trim();
    }
    return String.join(",", fields);
  }
}
