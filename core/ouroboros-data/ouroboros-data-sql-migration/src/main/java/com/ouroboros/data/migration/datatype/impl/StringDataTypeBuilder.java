package com.ouroboros.data.migration.datatype.impl;

import java.util.Optional;

import com.ouroboros.data.migration.datatype.DataTypeBuilder;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.StringValue;

import liquibase.database.Database;
import liquibase.structure.core.DataType;

/**
 * @author liansz
 **/
public class StringDataTypeBuilder implements DataTypeBuilder {

  @Override
  public boolean support(Database database, DataModelField field) {
    return field.getValueType() instanceof StringValue;
  }

  @Override
  public DataType build(Database database, DataModelField field) {
    int size = Optional.ofNullable(field.getSize()).orElse(255);
    if (size < 1024) {
      var dataType = new DataType("VARCHAR");
      dataType.setColumnSize(size);
      return dataType;
    }

    return new DataType("TEXT");
  }
}
