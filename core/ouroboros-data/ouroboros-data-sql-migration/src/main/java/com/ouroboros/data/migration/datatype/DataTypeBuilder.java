package com.ouroboros.data.migration.datatype;

import com.ouroboros.data.model.DataModelField;

import liquibase.database.Database;
import liquibase.structure.core.DataType;

/**
 * @author liansz
 **/
public interface DataTypeBuilder {

  boolean support(Database database, DataModelField dataModelField);

  DataType build(Database database, DataModelField dataModelField);
}
