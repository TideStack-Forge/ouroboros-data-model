package com.ouroboros.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves effective unique constraints for runtime consumers.
 */
public final class DataModelUniqueConstraints {
  private DataModelUniqueConstraints() {
  }

  public static List<DataModelUniqueConstraint> resolve(DataModel model) {
    Objects.requireNonNull(model, "model must not be null");
    List<DataModelUniqueConstraint> constraints = new ArrayList<>();
    constraints.addAll(resolveFieldLevelConstraints(model));
    constraints.addAll(resolveModelLevelConstraints(model));
    return Collections.unmodifiableList(constraints);
  }

  static String logicalId(
      DataModelUniqueConstraint.Source source,
      String declaredName,
      List<String> normalizedFields,
      UniquenessScope scope
  ) {
    if (declaredName != null && !declaredName.isBlank()) {
      return declaredName.trim();
    }
    return source.name().toLowerCase(Locale.ROOT)
        + ":"
        + String.join(",", normalizedFields)
        + ":"
        + scope.name().toLowerCase(Locale.ROOT);
  }

  static String normalizeFieldName(String fieldName) {
    return fieldName.trim().toLowerCase(Locale.ROOT);
  }

  static UniquenessScope modelDefaultScope(DataModel model) {
    return UniquenessScope.fromExtraProp(model.getExtraProp(UniquenessScope.EXTRA_PROP_NAME).orElse(null));
  }

  static UniquenessScope fieldScope(DataModel model, DataModelField field) {
    return explicitScopeOrModelDefault(model, field.getUniquenessScope());
  }

  static UniquenessScope modelConstraintScope(DataModel model, DataModelUniqueConstraintMeta constraint) {
    return explicitScopeOrModelDefault(model, constraint.getScope());
  }

  private static UniquenessScope explicitScopeOrModelDefault(DataModel model, UniquenessScope scope) {
    if (scope == null || scope == UniquenessScope.DEFAULT) {
      return modelDefaultScope(model);
    }
    return scope;
  }

  private static List<DataModelUniqueConstraint> resolveFieldLevelConstraints(DataModel model) {
    Set<String> primaryKeyFields = model.getPrimaryKeys().stream()
        .map(field -> normalizeFieldName(field.getName()))
        .collect(Collectors.toSet());

    return model.getFields().stream()
        .filter(field -> Boolean.TRUE.equals(field.getIsUnique()))
        .filter(field -> !primaryKeyFields.contains(normalizeFieldName(field.getName())))
        .map(field -> {
          String normalizedField = normalizeFieldName(field.getName());
          UniquenessScope scope = fieldScope(model, field);
          return new DataModelUniqueConstraint(
              logicalId(
                  DataModelUniqueConstraint.Source.FIELD,
                  null,
                  Collections.singletonList(normalizedField),
                  scope
              ),
              Collections.singletonList(field.getName()),
              scope,
              DataModelUniqueConstraint.Source.FIELD,
              field.getName()
          );
        })
        .collect(Collectors.toList());
  }

  private static List<DataModelUniqueConstraint> resolveModelLevelConstraints(DataModel model) {
    return model.getUniqueConstraints().stream()
        .map(constraint -> resolveModelLevelConstraint(model, constraint))
        .collect(Collectors.toList());
  }

  private static DataModelUniqueConstraint resolveModelLevelConstraint(
      DataModel model,
      DataModelUniqueConstraintMeta constraint
  ) {
    List<String> fields = constraint.getFields().stream()
        .map(fieldName -> model.getField(fieldName)
            .map(DataModelField::getName)
            .orElse(fieldName))
        .collect(Collectors.toList());
    List<String> normalizedFields = fields.stream()
        .map(DataModelUniqueConstraints::normalizeFieldName)
        .collect(Collectors.toList());
    UniquenessScope scope = modelConstraintScope(model, constraint);
    String name = logicalId(
        DataModelUniqueConstraint.Source.MODEL,
        constraint.getName(),
        normalizedFields,
        scope
    );
    return new DataModelUniqueConstraint(
        name,
        fields,
        scope,
        DataModelUniqueConstraint.Source.MODEL,
        name
    );
  }
}
