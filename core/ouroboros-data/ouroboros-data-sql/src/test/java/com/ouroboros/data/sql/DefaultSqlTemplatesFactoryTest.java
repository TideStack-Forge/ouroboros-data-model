package com.ouroboros.data.sql;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import com.querydsl.sql.PostgreSQLTemplates;

class DefaultSqlTemplatesFactoryTest {

  @Test
  void shouldResolveByAutoAndExplicitDialect() {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:factory_test;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");

    var factory = new DefaultSqlTemplatesFactory();
    assertNotNull(factory.apply(ds));
    assertNotNull(factory.apply(ds, "H2"));
    assertNotNull(factory.apply(ds, "auto"));
  }

  @Test
  void shouldThrowForUnsupportedDialect() {
    var factory = new DefaultSqlTemplatesFactory();
    var ex = assertThrows(SqlTemplatesException.class,
        () -> factory.apply(failingDataSource(), "NOT_SUPPORTED"));
    assertTrue(ex.getMessage().contains("Unsupported database type"));
  }

  @Test
  void shouldResolveKingbaseAndPostgresqlAliasesThroughSpi() {
    var factory = new DefaultSqlTemplatesFactory();

    assertInstanceOf(PostgreSQLTemplates.class, factory.apply(failingDataSource(), " kingbase "));
    assertInstanceOf(PostgreSQLTemplates.class, factory.apply(failingDataSource(), "KINGBASE"));
    assertInstanceOf(PostgreSQLTemplates.class, factory.apply(failingDataSource(), "KingbaseES"));
    assertInstanceOf(PostgreSQLTemplates.class, factory.getSQLTemplates(metadata("Kingbase ES")));
    assertInstanceOf(PostgreSQLTemplates.class, factory.getSQLTemplates(metadata("PostgreSQL")));
  }

  private static DatabaseMetaData metadata(String productName) {
    return (DatabaseMetaData) Proxy.newProxyInstance(
        DefaultSqlTemplatesFactoryTest.class.getClassLoader(),
        new Class[]{DatabaseMetaData.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getDatabaseProductName" -> productName;
          case "getDatabaseMajorVersion" -> 9;
          default -> defaultValue(method.getReturnType());
        });
  }

  private static Object defaultValue(Class<?> returnType) {
    if (returnType == Boolean.TYPE) {
      return false;
    }
    if (returnType == Integer.TYPE) {
      return 0;
    }
    if (returnType == Long.TYPE) {
      return 0L;
    }
    return null;
  }

  private static DataSource failingDataSource() {
    return new DataSource() {
      @Override public Connection getConnection() throws SQLException { throw new SQLException("boom"); }
      @Override public Connection getConnection(String username, String password) throws SQLException { throw new SQLException("boom"); }
      @Override public <T> T unwrap(Class<T> iface) { return null; }
      @Override public boolean isWrapperFor(Class<?> iface) { return false; }
      @Override public java.io.PrintWriter getLogWriter() { return null; }
      @Override public void setLogWriter(java.io.PrintWriter out) { }
      @Override public void setLoginTimeout(int seconds) { }
      @Override public int getLoginTimeout() { return 0; }
      @Override public java.util.logging.Logger getParentLogger() { return java.util.logging.Logger.getGlobal(); }
    };
  }
}
