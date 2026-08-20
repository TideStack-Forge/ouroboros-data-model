package com.ouroboros.data.migration;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
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

  static boolean shouldRetainExistingUniqueConstraint(List<DataModel> models, UniqueConstraint uniqueConstraint) {
    return findModelOwnedUniqueConstraint(models, uniqueConstraint)
        .map(owner -> shouldMaterializeOrdinaryUniqueConstraint(owner.model(), owner.field()))
        .orElse(true);
  }

  static String modelOwnedUniqueConstraintName(DataModel model, DataModelField field) {
    return UNIQUE_PREFIX
        + model.getFullName().replaceAll("\\.", "_").toLowerCase(Locale.ROOT)
        + "__"
        + field.getName().toLowerCase(Locale.ROOT);
  }

  private static Optional<OwnedUniqueConstraint> findModelOwnedUniqueConstraint(List<DataModel> models,
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
}
