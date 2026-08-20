package com.ouroboros.data.migration.kingbase;

import liquibase.database.DatabaseConnection;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.DatabaseException;

public final class KingbaseDatabase extends PostgresDatabase {
  public static final String SHORT_NAME = "kingbase";
  public static final String DRIVER_CLASS_NAME = "com.kingbase8.Driver";
  public static final String JDBC_URL_PREFIX = "jdbc:kingbase8:";
  private static final String PRODUCT_NAME = "KingbaseES";
  private static final int DEFAULT_PORT = 54321;

  @Override
  public String getShortName() {
    return SHORT_NAME;
  }

  @Override
  public String getDisplayName() {
    return PRODUCT_NAME;
  }

  @Override
  protected String getDefaultDatabaseProductName() {
    return PRODUCT_NAME;
  }

  @Override
  public Integer getDefaultPort() {
    return DEFAULT_PORT;
  }

  @Override
  public int getPriority() {
    return PRIORITY_DATABASE + 1;
  }

  @Override
  public boolean isCorrectDatabaseImplementation(DatabaseConnection connection) throws DatabaseException {
    return PRODUCT_NAME.equalsIgnoreCase(connection.getDatabaseProductName());
  }

  @Override
  public String getDefaultDriver(String url) {
    if (url != null && url.startsWith(JDBC_URL_PREFIX)) {
      return DRIVER_CLASS_NAME;
    }
    return super.getDefaultDriver(url);
  }
}
