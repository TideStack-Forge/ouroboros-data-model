package com.ouroboros.data.migration.datatype.impl;

import java.util.Optional;

import com.ouroboros.data.migration.datatype.DataTypeBuilder;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.DecimalValue;

import liquibase.database.Database;
import liquibase.structure.core.DataType;

/**
 * @author liansz
 **/
public class DecimalDataTypeBuilder implements DataTypeBuilder {

  @Override
  public boolean support(Database database, DataModelField field) {
    return field.getValueType() instanceof DecimalValue;
  }

  @Override
  public DataType build(Database database, DataModelField field) {
    var dataType = new DataType("DECIMAL");
    dataType.setColumnSize(Optional.ofNullable(field.getSize()).orElse(10));
    dataType.setDecimalDigits(Optional.ofNullable(field.getDecimalDigits()).orElse(2));

    return dataType;
  }
}
