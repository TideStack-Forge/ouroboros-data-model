package com.ouroboros.data.migration.kingbase;

import jakarta.annotation.Priority;

import com.ouroboros.data.migration.datatype.DataTypeBuilder;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.BooleanValue;

import liquibase.database.Database;
import liquibase.structure.core.DataType;

@Priority(-100)
public final class KingbaseBooleanDataTypeBuilder implements DataTypeBuilder {

  @Override
  public boolean support(Database database, DataModelField field) {
    return database instanceof KingbaseDatabase && field.getValueType() instanceof BooleanValue;
  }

  @Override
  public DataType build(Database database, DataModelField field) {
    return new DataType("BOOLEAN");
  }
}
