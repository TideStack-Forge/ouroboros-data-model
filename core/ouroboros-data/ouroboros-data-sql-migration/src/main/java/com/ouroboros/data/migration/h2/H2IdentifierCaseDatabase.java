package com.ouroboros.data.migration.h2;

import java.sql.SQLException;

import liquibase.CatalogAndSchema.CatalogAndSchemaCase;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.H2Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.UnexpectedLiquibaseException;

/** Liquibase 4.31.1 assumes upper-case H2 identifiers even when DATABASE_TO_LOWER is enabled. */
public final class H2IdentifierCaseDatabase extends H2Database {
  @Override
  public int getPriority() {
    return PRIORITY_DATABASE + 1;
  }

  @Override
  public void setConnection(DatabaseConnection connection) {
    super.setConnection(connection);
    if (connection instanceof JdbcConnection jdbcConnection) {
      try {
        var metadata = jdbcConnection.getWrappedConnection().getMetaData();
        unquotedObjectsAreUppercased = !metadata.storesLowerCaseIdentifiers();
      } catch (SQLException error) {
        throw new UnexpectedLiquibaseException("无法读取 H2 标识符大小写规则", error);
      }
    }
  }

  @Override
  public CatalogAndSchemaCase getSchemaAndCatalogCase() {
    return Boolean.FALSE.equals(unquotedObjectsAreUppercased)
        ? CatalogAndSchemaCase.LOWER_CASE : super.getSchemaAndCatalogCase();
  }
}
