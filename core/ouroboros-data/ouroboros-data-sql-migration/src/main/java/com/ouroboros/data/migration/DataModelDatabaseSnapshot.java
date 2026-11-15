package com.ouroboros.data.migration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ouroboros.data.model.DataModel;

import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import liquibase.database.core.H2Database;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.DatabaseObjectCollection;
import liquibase.structure.core.*;

public class DataModelDatabaseSnapshot extends DatabaseSnapshot {
  private final static Logger logger = LoggerFactory.getLogger(DataModelDatabaseSnapshot.class);

  private final List<DataModel> models;
  private final DatabaseObjectCollection furtheredObjects;

  public DataModelDatabaseSnapshot(Database database, List<DataModel> models) throws DatabaseException, InvalidExampleException {
    super(null, database);
    this.models = models;
    furtheredObjects = getDatabaseObjectCollection(this);

    // 初始化数据
    // getDatabase().getDefaultSchema();
    var catalogAndSchema = new CatalogAndSchema("", "");
    models.stream().map(model -> TableBuilder.build(database, model, catalogAndSchema)).forEach(table -> {
      furtheredObjects.add(table);
      furtheredObjects.add(table.getPrimaryKey());
      table.getIndexes().forEach(furtheredObjects::add);
      table.getOutgoingForeignKeys().forEach(furtheredObjects::add);
      table.getUniqueConstraints().forEach(furtheredObjects::add);
      table.getColumns().forEach(furtheredObjects::add);
    });
  }

  public void mergeDatabaseSnapshot(DatabaseSnapshot referenceDatabaseSnapshot) {
    var referenceObjects = getDatabaseObjectCollection(referenceDatabaseSnapshot);
    // 库
    mergeObject(referenceObjects.get(Catalog.class), false);
    mergeObject(referenceObjects.get(Schema.class), false);

    // 表、字段
    mergeObject(referenceObjects.get(Table.class), true);
    mergeObject(referenceObjects.get(Column.class), true);

    // TODO: 主键考虑是否要合并
    mergeObject(referenceObjects.get(PrimaryKey.class), true);
    mergeObject(referenceObjects.get(Index.class), false);
    mergeObject(referenceObjects.get(Sequence.class), false);
    mergeObject(referenceObjects.get(StoredProcedure.class), false);

    mergeObject(referenceObjects.get(UniqueConstraint.class), false);
    mergeObject(referenceObjects.get(ForeignKey.class), true);

    // 视图
    mergeObject(referenceObjects.get(View.class), false);
  }

  static DatabaseObjectCollection getDatabaseObjectCollection(DatabaseSnapshot databaseSnapshot) {
    return (DatabaseObjectCollection) databaseSnapshot.getSerializableFieldValue("objects");
  }

  private void mergeObject(Set<? extends DatabaseObject> referenceObjects, boolean isSetSnapshotId) {
    for (var referenceObject : referenceObjects) {
      var furtheredObject = furtheredObjects.get(referenceObject, null);
      if (furtheredObject != null && isSetSnapshotId) {
        furtheredObject.setSnapshotId(referenceObject.getSnapshotId());
      }

      addOrMergeObject(referenceObject, furtheredObject);
    }
  }

  private void addOrMergeObject(DatabaseObject referenceObject, DatabaseObject furtheredObject) {
    if (furtheredObject == null) {
      if (referenceObject instanceof Column column) {
        // 主键列保留非空约束，其他删除列允许为空
        if (!column.isNullable()) {
          Column clone = new Column();
          clone.setName(column.getName());
          clone.setRelation(column.getRelation());
          clone.setType(column.getType());
          clone.setRemarks(column.getRemarks());
          clone.setNullable(!isPrimaryKeyColumn(column));
          clone.setOrder(column.getOrder());

          furtheredObjects.get(Table.class).stream()
              .filter(table -> table.getName().equalsIgnoreCase(clone.getRelation().getName()))
              .forEach(table -> table.addColumn(clone));
          furtheredObjects.add(clone);
          return;
        }
      }

      if (referenceObject instanceof UniqueConstraint uniqueConstraint) {
        if (!UniqueConstraintScopePolicy.shouldRetainExistingUniqueConstraint(models, uniqueConstraint)) {
          return;
        }
      }

      furtheredObjects.add(referenceObject);
      return;
    }

    for (String attribute : referenceObject.getAttributes()) {
      var newAttrValue = furtheredObject.getAttribute(attribute, Object.class);
      var referAttrValue = referenceObject.getAttribute(attribute, Object.class);
      if (newAttrValue == null) {
        furtheredObject.setAttribute(attribute, referAttrValue);
        continue;
      }

      // 字段默认值，默认值以数据库为准
      if ("defaultValue".equalsIgnoreCase(attribute)) {
        furtheredObject.setAttribute(attribute, referAttrValue != null ? referAttrValue : newAttrValue);
        continue;
      }

      // 字段
      if ("type".equalsIgnoreCase(attribute)) {
        if (!(newAttrValue instanceof DataType newType) || !(referAttrValue instanceof DataType referType)) {
          logger.error("字段类型不是DataType类型，新类型：{}，参考类型：{}", newAttrValue, referAttrValue);
          continue;
        }
        if (referenceObject instanceof Column existing && furtheredObject instanceof Column desired
            && canWidenString(existing, desired)) {
          continue;
        }
        var isDataTypeCompatible = isDataTypeCompatible(newType, referType);
        if (!isDataTypeCompatible) {
          var table = furtheredObject.getAttribute("relation", null);
          var tableName = table instanceof Table t ? t.getName() : "未知表";
          var fieldName = furtheredObject.getName();
          logger.warn("表 {} 的 {} 字段类型不兼容，新类型：{}，原类型：{}，将不会自动修改字段类型", tableName, fieldName, newAttrValue, referAttrValue);
          furtheredObject.setAttribute(attribute, referAttrValue);
          continue;
        }
        var isDataTypeUpgradable = isDataTypeUpgradable(newType, referType);
        furtheredObject.setAttribute(attribute, isDataTypeUpgradable ? newType : referType);
      }

      // 表、字段
      if ("remarks".equalsIgnoreCase(attribute)) {
        furtheredObject.setAttribute(attribute, referAttrValue);
      }

      // 唯一约束？
      if ("name".equalsIgnoreCase(attribute)) {
        furtheredObject.setAttribute(attribute, referAttrValue);
      }
    }
  }

  static boolean hasFieldOption(List<DataModel> models, Column column, String option) {
    return models.stream().filter(model -> column.getRelation().getName().equalsIgnoreCase(model.getRawName()))
        .flatMap(model -> model.getFields().stream())
        .filter(field -> column.getName().equalsIgnoreCase(field.getRawName()))
        .anyMatch(field -> field.getExtraProp(String.class, option).filter("true"::equals).isPresent());
  }

  private boolean canWidenString(Column existing, Column desired) {
    if (!(isShortVarcharToText(existing.getType(), desired.getType())
            || (getDatabase() instanceof H2Database && isShortVarchar(existing.getType())
                && "CLOB".equalsIgnoreCase(desired.getType().getTypeName())))
        || existing.getDefaultValue() != null || existing.getComputed() == Boolean.TRUE
        || !Objects.equals(existing.isNullable(), desired.isNullable())
        || (existing.getRemarks() != null && !existing.getRemarks().isBlank())
        || !(existing.getRelation() instanceof Table source) || !(desired.getRelation() instanceof Table target)) {
      return false;
    }
    return !isKeyColumn(source, existing) && !isKeyColumn(target, desired)
        && hasFieldOption(models, desired, "allowVarcharToText");
  }

  static boolean isShortVarcharToText(DataType existing, DataType desired) {
    return isShortVarchar(existing) && desired != null && "TEXT".equalsIgnoreCase(desired.getTypeName());
  }

  private static boolean isShortVarchar(DataType type) {
    return type != null
        && ("VARCHAR".equalsIgnoreCase(type.getTypeName()) || "CHARACTER VARYING".equalsIgnoreCase(type.getTypeName()))
        && type.getColumnSize() != null && type.getColumnSize() > 0 && type.getColumnSize() < 1024;
  }

  private boolean isKeyColumn(Table table, Column column) {
    return isPrimaryKeyColumn(column)
        || table.getIndexes().stream().flatMap(index -> index.getColumns().stream()).anyMatch(c -> sameColumn(c, column))
        || table.getUniqueConstraints().stream().flatMap(key -> key.getColumns().stream()).anyMatch(c -> sameColumn(c, column))
        || table.getOutgoingForeignKeys().stream().flatMap(key -> key.getForeignKeyColumns().stream()).anyMatch(c -> sameColumn(c, column));
  }

  private boolean sameColumn(Column left, Column right) {
    return right.getName().equalsIgnoreCase(left.getName());
  }

  private boolean isPrimaryKeyColumn(Column column) {
    if (!(column.getRelation() instanceof Table table)) {
      return false;
    }

    var primaryKey = table.getPrimaryKey();
    if (primaryKey == null || primaryKey.getColumns() == null) {
      return false;
    }

    return primaryKey.getColumns().stream().anyMatch(primaryKeyColumn ->
        primaryKeyColumn == column
            || (primaryKeyColumn.getName() != null
            && primaryKeyColumn.getName().equalsIgnoreCase(column.getName())));
  }

  // 判断两种类型是否兼容
  private boolean isDataTypeCompatible(DataType newType, DataType referType) {
    var newTypeName = newType.getTypeName().toLowerCase();
    var referTypeName = referType.getTypeName().toLowerCase();
    if (newTypeName.equalsIgnoreCase(referTypeName)) {
      return true;
    }
    var isBothChar = newTypeName.endsWith("char") && referTypeName.endsWith("char");
    var isBothText = newTypeName.endsWith("text") && referTypeName.endsWith("text");
    var istBothInteger = "integer".equals(newTypeName) && "int".equals(referTypeName);

    return isBothChar || isBothText || istBothInteger;
  }

  private boolean isDataTypeUpgradable(DataType newType, DataType referType) {
    var newSize = newType.getColumnSize();
    var referSize = referType.getColumnSize();
    var newDecimalDigits = Optional.ofNullable(newType.getDecimalDigits()).orElse(0);
    var referDecimalDigits = Optional.ofNullable(referType.getDecimalDigits()).orElse(0);
    if (ObjectUtils.allNotNull(newSize, referSize)) {
      return newSize > referSize && newDecimalDigits >= referDecimalDigits
          || newSize >= referSize && newDecimalDigits > referDecimalDigits;
    }
    return false;
  }
}
