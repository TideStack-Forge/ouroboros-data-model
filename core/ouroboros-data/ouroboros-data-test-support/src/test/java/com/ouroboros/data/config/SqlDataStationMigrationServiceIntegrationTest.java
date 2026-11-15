package com.ouroboros.data.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.station.SqlDataStation;
import com.ouroboros.data.util.DataMaps;

class SqlDataStationMigrationServiceIntegrationTest {

  @Test
  void shouldUseSqlMigrationServiceProviderFromSqlMigrationModule() throws SQLException {
    var dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:sql_migration_service_provider;MODE=MySQL;DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    dataSource.setPassword("");

    var meta = DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", "SqlMigrationServiceSmoke",
        "rawName", "t_sql_migration_service_smoke",
        "primaryKeys", List.of("id"),
        "fields", List.of(
            Map.of("name", "id", "type", "Long"),
            Map.of("name", "code", "type", "String", "size", 64, "isNullable", true)))).get();
    var station = new SqlDataStation("test", dataSource, Collections.singletonList(meta), MigrationStrategy.AUTO, "H2");

    var sqls = station.generateMigrationSql(station.getDataModelList());
    assertTrue(sqls.isRight(), () -> sqls.getLeft().getMessage());
    assertFalse(sqls.get().isEmpty());

    var migrated = station.migrate();
    assertTrue(migrated.isRight(), () -> migrated.getLeft().getMessage());
    assertTrue(migrated.get());

    try (var connection = dataSource.getConnection();
         var statement = connection.createStatement();
         var resultSet = statement.executeQuery("select count(*) from t_sql_migration_service_smoke")) {
      assertTrue(resultSet.next());
      assertEquals(0, resultSet.getInt(1));
    }
  }

  @Test
  void shouldAddNullableRevisionColumnWithoutBackfillingExistingRows() throws SQLException {
    var dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:nullable_revision_migration;MODE=MySQL;DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    dataSource.setPassword("");
    try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
      statement.execute("create table t_configuration_value (config_key varchar(255) primary key, config_value varchar(255))");
      statement.execute("insert into t_configuration_value(config_key, config_value) values ('legacy', 'stored')");
    }

    var meta = DataMaps.tryToBean(DataModelMeta.class, Map.of(
        "name", "ConfigurationValue",
        "rawName", "t_configuration_value",
        "primaryKeys", List.of("configKey"),
        "fields", List.of(
            Map.of("name", "configKey", "type", "String", "rawName", "config_key"),
            Map.of("name", "configValue", "type", "String", "rawName", "config_value", "isNullable", true),
            Map.of("name", "revision", "type", "Long", "isNullable", true)))).get();
    var station = new SqlDataStation("test", dataSource, Collections.singletonList(meta), MigrationStrategy.AUTO, "H2");

    var migrated = station.migrate();
    assertTrue(migrated.isRight(), () -> migrated.getLeft().getMessage());
    try (var connection = dataSource.getConnection();
         var statement = connection.createStatement();
         var resultSet = statement.executeQuery("select revision from t_configuration_value where config_key = 'legacy'")) {
      assertTrue(resultSet.next());
      assertEquals(null, resultSet.getObject("revision"));
    }
  }
}
