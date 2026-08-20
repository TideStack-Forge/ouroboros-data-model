package com.ouroboros.data.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assumptions;

final class KingbaseIntegrationEnvironment {
  private static final String DRIVER_CLASS_NAME = "com.kingbase8.Driver";

  private KingbaseIntegrationEnvironment() {
  }

  static DataSource requireDataSource() throws Exception {
    var url = environment("KINGBASE_JDBC_URL");
    var username = environment("KINGBASE_JDBC_USERNAME");
    var password = environment("KINGBASE_JDBC_PASSWORD");
    var connectionValues = Arrays.asList(url, username, password);

    boolean allMissing = connectionValues.stream().allMatch(KingbaseIntegrationEnvironment::isBlank);
    Assumptions.assumeFalse(allMissing,
        "Set KINGBASE_JDBC_URL, KINGBASE_JDBC_USERNAME, KINGBASE_JDBC_PASSWORD and KINGBASE_JDBC_JAR to run the smoke test");
    assertFalse(connectionValues.stream().anyMatch(KingbaseIntegrationEnvironment::isBlank),
        "Kingbase connection environment is partially configured");

    var jar = environment("KINGBASE_JDBC_JAR");
    assertFalse(isBlank(jar), "KINGBASE_JDBC_JAR is required when Kingbase connection environment is configured");
    Path jarPath = Paths.get(jar.trim()).toAbsolutePath().normalize();
    assertTrue(Files.isRegularFile(jarPath), "KINGBASE_JDBC_JAR must point to a regular file");
    try (var jarFile = new JarFile(jarPath.toFile())) {
      assertTrue(jarFile.getEntry("com/kingbase8/Driver.class") != null,
          "KINGBASE_JDBC_JAR does not contain com.kingbase8.Driver");
    }

    Class.forName(DRIVER_CLASS_NAME);
    var dataSource = new SimpleDriverManagerDataSource(url, username, password);
    try (var connection = dataSource.getConnection()) {
      assertEquals("KingbaseES", connection.getMetaData().getDatabaseProductName());
    }
    return dataSource;
  }

  private static String environment(String name) {
    var value = System.getenv(name);
    return value == null ? "" : value;
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private record SimpleDriverManagerDataSource(String url, String username, String password) implements DataSource {
    @Override
    public Connection getConnection() throws SQLException {
      return DriverManager.getConnection(url, username, password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return DriverManager.getConnection(url, username, password);
    }

    @Override
    public PrintWriter getLogWriter() {
      return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) {
      DriverManager.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) {
      DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() {
      return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() {
      return Logger.getGlobal();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLException("Not a wrapper");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }
  }
}
