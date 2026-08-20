package com.ouroboros.data.migration.datatype.impl;

import com.ouroboros.data.migration.datatype.DataTypeBuilder;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.MapValue;

import liquibase.database.Database;
import liquibase.structure.core.DataType;

/**
 * @author liansz
 **/
public class MapDataTypeBuilder implements DataTypeBuilder {

  @Override
  public boolean support(Database database, DataModelField field) {
    return field.getValueType() instanceof MapValue;
  }

  @Override
  public DataType build(Database database, DataModelField field) {
    return new DataType("TEXT");
  }
}
