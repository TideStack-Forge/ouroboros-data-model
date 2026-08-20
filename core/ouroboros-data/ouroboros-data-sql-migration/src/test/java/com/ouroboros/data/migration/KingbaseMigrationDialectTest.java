package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.migration.kingbase.KingbaseDatabase;

import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;

class KingbaseMigrationDialectTest {

  @Test
  void shouldDiscoverAndSelectKingbaseDatabaseThroughLiquibaseSpi() throws Exception {
    boolean discovered = false;
    for (Database candidate : ServiceLoader.load(Database.class, KingbaseDatabase.class.getClassLoader())) {
      if (candidate instanceof KingbaseDatabase) {
        discovered = true;
        break;
      }
    }
    assertTrue(discovered);

    DatabaseFactory.reset();
    var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
        databaseConnection("KingbaseES", "jdbc:kingbase8://127.0.0.1:54321/test"));

    assertInstanceOf(KingbaseDatabase.class, database);
    assertEquals("kingbase", database.getShortName());
    assertEquals("KingbaseES", database.getDisplayName());
    assertEquals("com.kingbase8.Driver", database.getDefaultDriver("jdbc:kingbase8://127.0.0.1:54321/test"));
    assertEquals(54321, database.getDefaultPort());
  }

  private static DatabaseConnection databaseConnection(String productName, String url) {
    return (DatabaseConnection) Proxy.newProxyInstance(
        KingbaseMigrationDialectTest.class.getClassLoader(),
        new Class[]{DatabaseConnection.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getDatabaseProductName" -> productName;
          case "getDatabaseProductVersion" -> "V009R001C010";
          case "getDatabaseMajorVersion" -> 9;
          case "getDatabaseMinorVersion" -> 0;
          case "getURL" -> url;
          case "getConnectionUserName" -> "system";
          case "getCatalog" -> "test";
          case "isClosed" -> false;
          case "getPriority" -> 1;
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
    if (returnType == Double.TYPE) {
      return 0D;
    }
    if (returnType == Float.TYPE) {
      return 0F;
    }
    return null;
  }
}
