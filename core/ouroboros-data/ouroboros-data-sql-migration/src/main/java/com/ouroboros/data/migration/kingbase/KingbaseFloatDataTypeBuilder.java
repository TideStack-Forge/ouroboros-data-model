package com.ouroboros.data.migration.kingbase;

import jakarta.annotation.Priority;

import com.ouroboros.data.migration.datatype.DataTypeBuilder;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.FloatValue;

import liquibase.database.Database;
import liquibase.structure.core.DataType;

@Priority(-100)
public final class KingbaseFloatDataTypeBuilder implements DataTypeBuilder {

  @Override
  public boolean support(Database database, DataModelField field) {
    return database instanceof KingbaseDatabase && field.getValueType() instanceof FloatValue;
  }

  @Override
  public DataType build(Database database, DataModelField field) {
    return new DataType("REAL");
  }
}
