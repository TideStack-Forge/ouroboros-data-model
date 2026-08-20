package com.ouroboros.data.migration.datatype.impl;

import com.ouroboros.data.migration.datatype.DataTypeBuilder;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.LongValue;
import com.ouroboros.data.model.valuetypes.SnowflakeIdValue;

import liquibase.database.Database;
import liquibase.structure.core.DataType;

/**
 * @author liansz
 **/
public class LongDataTypeBuilder implements DataTypeBuilder {

  @Override
  public boolean support(Database database, DataModelField field) {
    return field.getValueType() instanceof LongValue || field.getValueType() instanceof SnowflakeIdValue;
  }

  @Override
  public DataType build(Database database, DataModelField field) {
    return new DataType("BIGINT");
  }
}
