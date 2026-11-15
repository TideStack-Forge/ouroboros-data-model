package com.ouroboros.data.migration;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelUniqueConstraint;
import com.ouroboros.data.model.DataModelUniqueConstraints;
import com.ouroboros.data.model.UniquenessScope;

import liquibase.structure.core.Column;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;

final class UniqueConstraintScopePolicy {
  private static final String UNIQUE_PREFIX = "un_";

  private UniqueConstraintScopePolicy() {
  }

  static boolean shouldMaterializeOrdinaryUniqueConstraint(DataModel model, DataModelField field) {
    return Boolean.TRUE.equals(field.getIsUnique())
        && uniquenessScopeOf(model, field) == UniquenessScope.ALL_RECORDS;
  }

  static UniqueConstraint buildOrdinaryUniqueConstraint(Table table, DataModel model, DataModelField field) {
    var unique = new UniqueConstraint();
    var column = table.getColumn(field.getRawName());
    unique.addColumn(0, column);
    unique.setRelation(table);
    unique.setDisabled(false);
    unique.setName(modelOwnedUniqueConstraintName(model, field));
    return unique;
  }

  static boolean shouldMaterializeModelUniqueConstraint(
      DataModel model,
      Table table,
      DataModelUniqueConstraint constraint) {
    return constraint.getScope() == UniquenessScope.ALL_RECORDS
        && !constraint.getFields().isEmpty()
        && constraint.getFields().stream()
            .map(fieldName -> findField(model, fieldName).map(field -> table.getColumn(field.getRawName())))
            .allMatch(Optional::isPresent);
  }

  static UniqueConstraint buildModelUniqueConstraint(
      DataModel model,
      Table table,
      DataModelUniqueConstraint constraint) {
    var unique = new UniqueConstraint();
    for (int i = 0; i < constraint.getFields().size(); i++) {
      DataModelField field = findField(model, constraint.getFields().get(i)).orElseThrow();
      unique.addColumn(i, table.getColumn(field.getRawName()));
    }
    unique.setRelation(table);
    unique.setDisabled(false);
    unique.setName(modelUniqueConstraintName(model, constraint));
    return unique;
  }

  static boolean shouldRetainExistingUniqueConstraint(List<DataModel> models, UniqueConstraint uniqueConstraint) {
    Optional<ModelOwnedUniqueConstraint> modelOwned = findModelOwnedUniqueConstraint(models, uniqueConstraint);
    if (modelOwned.isPresent()) {
      return modelOwned.get().constraint().getScope() == UniquenessScope.ALL_RECORDS;
    }
    return findFieldOwnedUniqueConstraint(models, uniqueConstraint)
        .map(owner -> shouldMaterializeOrdinaryUniqueConstraint(owner.model(), owner.field()))
        .orElse(true);
  }

  static String modelOwnedUniqueConstraintName(DataModel model, DataModelField field) {
    return UNIQUE_PREFIX
        + model.getFullName().replaceAll("\\.", "_").toLowerCase(Locale.ROOT)
        + "__"
        + field.getName().toLowerCase(Locale.ROOT);
  }

  static String modelUniqueConstraintName(DataModel model, DataModelUniqueConstraint constraint) {
    if (StringUtils.isNotBlank(constraint.getName()) && constraint.getName().matches("[A-Za-z0-9_]+")) {
      return constraint.getName();
    }
    String fields = constraint.getFields().stream()
        .map(field -> field.replaceAll("[^A-Za-z0-9_]", "_"))
        .reduce((left, right) -> left + "_" + right)
        .orElse("constraint");
    String generated = UNIQUE_PREFIX
        + model.getFullName().replaceAll("[^A-Za-z0-9_]", "_").toLowerCase(Locale.ROOT)
        + "__" + fields.toLowerCase(Locale.ROOT);
    return generated.length() <= 64 ? generated : generated.substring(0, 64);
  }

  private static Optional<ModelOwnedUniqueConstraint> findModelOwnedUniqueConstraint(List<DataModel> models,
                                                                                      UniqueConstraint uniqueConstraint) {
    if (uniqueConstraint == null || uniqueConstraint.getRelation() == null) {
      return Optional.empty();
    }
    String tableName = uniqueConstraint.getRelation().getName();
    if (StringUtils.isBlank(tableName)) {
      return Optional.empty();
    }
    return models.stream()
        .filter(model -> tableName.equalsIgnoreCase(model.getRawName()))
        .flatMap(model -> DataModelUniqueConstraints.resolve(model).stream()
            .filter(constraint -> constraint.getSource() == DataModelUniqueConstraint.Source.MODEL)
            .filter(constraint -> modelUniqueConstraintName(model, constraint)
                .equalsIgnoreCase(uniqueConstraint.getName()))
            .filter(constraint -> sameColumns(model, uniqueConstraint, constraint))
            .map(ModelOwnedUniqueConstraint::new))
        .findFirst();
  }

  private static Optional<OwnedUniqueConstraint> findFieldOwnedUniqueConstraint(List<DataModel> models,
                                                                                 UniqueConstraint uniqueConstraint) {
    if (uniqueConstraint == null
        || uniqueConstraint.getRelation() == null
        || StringUtils.isBlank(uniqueConstraint.getName())
        || uniqueConstraint.getColumns() == null
        || uniqueConstraint.getColumns().size() != 1) {
      return Optional.empty();
    }

    Column column = uniqueConstraint.getColumns().get(0);
    if (column == null || StringUtils.isBlank(column.getName())) {
      return Optional.empty();
    }

    String tableName = uniqueConstraint.getRelation().getName();
    if (StringUtils.isBlank(tableName)) {
      return Optional.empty();
    }

    return models.stream()
        .filter(model -> tableName.equalsIgnoreCase(model.getRawName()))
        .flatMap(model -> model.getFields().stream()
            .filter(field -> column.getName().equalsIgnoreCase(field.getRawName()))
            .filter(field -> uniqueConstraint.getName().equalsIgnoreCase(modelOwnedUniqueConstraintName(model, field)))
            .map(field -> new OwnedUniqueConstraint(model, field)))
        .findFirst();
  }

  private static boolean sameColumns(
      DataModel model,
      UniqueConstraint uniqueConstraint,
      DataModelUniqueConstraint constraint) {
    if (uniqueConstraint.getColumns() == null
        || uniqueConstraint.getColumns().size() != constraint.getFields().size()) {
      return false;
    }
    for (int i = 0; i < constraint.getFields().size(); i++) {
      Optional<DataModelField> field = findField(model, constraint.getFields().get(i));
      if (field.isEmpty()
          || uniqueConstraint.getColumns().get(i) == null
          || !field.get().getRawName().equalsIgnoreCase(uniqueConstraint.getColumns().get(i).getName())) {
        return false;
      }
    }
    return true;
  }

  private static Optional<DataModelField> findField(DataModel model, String fieldName) {
    return model.getFields().stream()
        .filter(field -> field.getName().equalsIgnoreCase(fieldName)
            || field.getRawName().equalsIgnoreCase(fieldName))
        .findFirst();
  }

  private static UniquenessScope uniquenessScopeOf(DataModel model) {
    return UniquenessScope.fromExtraProp(model.getExtraProp(UniquenessScope.EXTRA_PROP_NAME).orElse(null));
  }

  private static UniquenessScope uniquenessScopeOf(DataModel model, DataModelField field) {
    UniquenessScope fieldScope = field.getUniquenessScope();
    if (fieldScope == null || fieldScope == UniquenessScope.DEFAULT) {
      return uniquenessScopeOf(model);
    }
    return fieldScope;
  }

  private record OwnedUniqueConstraint(DataModel model, DataModelField field) {
  }

  private record ModelOwnedUniqueConstraint(DataModelUniqueConstraint constraint) {
  }
}
