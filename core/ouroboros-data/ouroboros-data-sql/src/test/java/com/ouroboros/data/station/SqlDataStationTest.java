package com.ouroboros.data.station;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.MigrationStrategy;

class SqlDataStationTest {

  @Test
  void shouldReturnFalseWhenMigrationDisabled() {
    var station = new SqlDataStation("s1", failingDataSource(), Collections.emptyList(), MigrationStrategy.DISABLED, "H2");
    var result = station.migrate();
    assertTrue(result.isRight());
    assertEquals(false, result.get());
  }

  @Test
  void shouldSurfaceConnectionFailureWhenMigrationRuns() {
    var station = new SqlDataStation("s2", failingDataSource(), Collections.emptyList(), MigrationStrategy.AUTO, "H2");
    var migrate = station.migrate(Collections.emptyList());
    assertTrue(migrate.isLeft());
    assertTrue(migrate.getLeft().getMessage().contains("获取数据库连接失败"));

    var sqls = station.generateMigrationSql(Collections.emptyList());
    assertTrue(sqls.isLeft());
    assertTrue(sqls.getLeft().getMessage().contains("获取数据库连接失败"));
  }

  @Test
  void shouldExposeCtorPropertiesAndDispatchByNames() {
    var ds = failingDataSource();
    var station = new SqlDataStation("s3", ds, Collections.emptyList(), MigrationStrategy.AUTO, "H2");
    assertEquals("H2", station.getDialect());
    assertEquals(MigrationStrategy.AUTO, station.getMigrationStrategy());
    assertEquals(ds, station.getDataSource());

    var byNames = station.migrateByNames(Collections.emptyList());
    assertTrue(byNames.isLeft());
  }

  private static DataSource failingDataSource() {
    return new DataSource() {
      @Override
      public Connection getConnection() throws SQLException {
        throw new SQLException("boom");
      }

      @Override
      public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLException("boom");
      }

      @Override
      public <T> T unwrap(Class<T> iface) {
        return null;
      }

      @Override
      public boolean isWrapperFor(Class<?> iface) {
        return false;
      }

      @Override
      public java.io.PrintWriter getLogWriter() {
        return null;
      }

      @Override
      public void setLogWriter(java.io.PrintWriter out) {
      }

      @Override
      public void setLoginTimeout(int seconds) {
      }

      @Override
      public int getLoginTimeout() {
        return 0;
      }

      @Override
      public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getGlobal();
      }
    };
  }
}
