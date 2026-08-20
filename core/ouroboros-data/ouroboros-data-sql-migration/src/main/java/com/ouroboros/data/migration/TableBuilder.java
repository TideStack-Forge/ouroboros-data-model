package com.ouroboros.data.migration;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.migration.datatype.DataTypeBuilder;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.RelatedValue;
import com.ouroboros.data.util.DataServices;

import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import liquibase.structure.core.*;

public class TableBuilder {
  private static final String CONFIG_START_WITH = "startWith";
  private static final String CONFIG_INCREMENT_BY = "incrementBy";
  private static final String PK_PREFIX = "PK_";
  private static final int MAX_CONSTRAINT_NAME_LENGTH = 64;

  static Table build(Database database, DataModel dataModel, CatalogAndSchema catalogAndSchema) {
    var tableName = dataModel.getRawName();
    var catalogName = catalogAndSchema.getCatalogName();
    var schemaName = catalogAndSchema.getSchemaName();

    var table = new Table(catalogName, schemaName, tableName);
    table.setRemarks(dataModel.getDescription() == null || dataModel.getDescription().isEmpty() ? dataModel.getLabel() : dataModel.getDescription());
    fillColumns(database, dataModel, table);
    fillPrimaryKey(dataModel, table);
    setUniqueConstraint(dataModel, table);
    return table;
  }

  private static void fillColumns(Database database, DataModel dataModel, Table table) {
    var fields = dataModel.getFields();
    var columns = fields.stream()
        .filter(field -> field.getValueType().isPhysical())
        .map(field -> {
          var column = new Column();
          column.setName(field.getRawName());
          column.setNullable(field.getIsNullable());
          column.setRelation(table);
          column.setType(buildDataType(database, field));

          var remarks = StringUtils.isNotEmpty(field.getDescription())
              ? field.getDescription()
              : StringUtils.isNotEmpty(field.getLabel())
              ? field.getLabel()
              : null;
          column.setRemarks(remarks);

          if (field.getIsAutoIncrement() && column.getType().getTypeName().toUpperCase().contains("INT")) {
            var startWith = field.getExtraProp(Integer.class, CONFIG_START_WITH).orElse(1);
            var incrementBy = field.getExtraProp(Integer.class, CONFIG_INCREMENT_BY).orElse(1);
            var autoIncrementInformation = new Column.AutoIncrementInformation(startWith, incrementBy);
            column.setAutoIncrementInformation(autoIncrementInformation);
          }

          return column;
        })
        .collect(Collectors.toList());
    table.getColumns().addAll(columns);
  }

  private static DataType buildDataType(Database database, DataModelField field) {
    String rawType = field.getRawType();
    if (StringUtils.isNotEmpty(rawType)) {
      return new DataType(rawType);
    }

    if (field.getValueType() instanceof RelatedValue<?> relatedType) {
      var referenceKey = relatedType.getReferenceKey();
      if (referenceKey.isPresent()) {
        return buildDataType(database, referenceKey.get());
      }
    }

    return DataServices.getCachedSortedServiceStream(DataTypeBuilder.class)
        .filter(builder -> builder.support(database, field))
        .findFirst()
        .map(builder -> builder.build(database, field))
        .orElseGet(() -> {
          var dataType = new DataType("VARCHAR");
          dataType.setColumnSize(255);
          return dataType;
        });
  }

  private static void fillPrimaryKey(DataModel dataModel, Table table) {
    var primaryKeys = dataModel.getPrimaryKeys();
    if (primaryKeys.isEmpty()) {
      return;
    }
    var primaryKey = new PrimaryKey();
    String pkName = PK_PREFIX + table.getName();
    if (pkName.length() > MAX_CONSTRAINT_NAME_LENGTH) {
      // 截掉表名前面的部分，保留结尾
      pkName = PK_PREFIX + table.getName().substring(table.getName().length() - (MAX_CONSTRAINT_NAME_LENGTH - PK_PREFIX.length()));
    }

    primaryKey.setName(pkName);
    for (int i = 0; i < primaryKeys.size(); i++) {
      var column = table.getColumn(primaryKeys.get(i).getRawName());
      column.setNullable(false);
      column.setDefaultValue(null);
      primaryKey.addColumn(i, column);
    }
    table.setPrimaryKey(primaryKey);
    primaryKey.setTable(table);
  }

  private static void setUniqueConstraint(DataModel dataModel, Table table) {
    var uniqueList = table.getUniqueConstraints();
    dataModel.getFields().stream()
        .filter(field -> UniqueConstraintScopePolicy.shouldMaterializeOrdinaryUniqueConstraint(dataModel, field))
        .map(field -> UniqueConstraintScopePolicy.buildOrdinaryUniqueConstraint(table, dataModel, field))
        .forEach(uniqueList::add);
  }
}
