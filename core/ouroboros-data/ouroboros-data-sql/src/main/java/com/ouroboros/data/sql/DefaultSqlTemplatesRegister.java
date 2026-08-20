package com.ouroboros.data.sql;

import java.util.Map;

import jakarta.annotation.Priority;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.util.DataServices;
import com.querydsl.sql.*;

@Priority(0)
public class DefaultSqlTemplatesRegister implements SqlTemplatesRegister {
  @Override
  public void register(Map<String, SQLTemplatesSupplier> registry) {
    registry.put("MYSQL", MySQLTemplates.builder().quote()::build);
    registry.put("ORACLE", OracleTemplates.builder().quote()::build);
    registry.put("DB2", DB2Templates.builder().quote()::build);
    registry.put("CUBRID", CUBRIDTemplates.builder().quote()::build);
    registry.put("FIREBIRD", FirebirdTemplates.builder().quote()::build);
    registry.put("H2", H2Templates.builder().quote()::build);
    registry.put("HSQLDB", HSQLDBTemplates.builder().quote()::build);
    registry.put("SQLITE", SQLiteTemplates.builder().quote()::build);
    registry.put("POSTGRES", PostgreSQLTemplates.builder().quote()::build);
    registry.put("POSTGRESQL", PostgreSQLTemplates.builder().quote()::build);
    registry.put("MICROSOFT SQL SERVER 8", SQLServerTemplates.builder().quote()::build);
    registry.put("MICROSOFT SQL SERVER 9", SQLServer2005Templates.builder().quote()::build);
    registry.put("MICROSOFT SQL SERVER 10", SQLServer2008Templates.builder().quote()::build);
    registry.put("MICROSOFT SQL SERVER 11", SQLServer2012Templates.builder().quote()::build);
    registry.put("DERBY", DerbyTemplates.builder().quote()::build);
    DataServices.getSortedServiceStream(SQLTemplatesSupplier.class)
        .filter(item -> !StringUtils.isBlank(item.getName()))
        .forEach(supplier -> registry.put(supplier.getName(), supplier));
  }
}
