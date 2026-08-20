package com.ouroboros.data.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Collections;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.model.ModelMetaBuilder;
import com.ouroboros.data.station.SqlDataStation;

class SqlDataStationMigrationServiceIntegrationTest {

  @Test
  void shouldUseSqlMigrationServiceProviderFromSqlMigrationModule() throws SQLException {
    var dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:sql_migration_service_provider;MODE=MySQL;DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    dataSource.setPassword("");

    var meta = ModelMetaBuilder.create("SqlMigrationServiceSmoke")
        .table("t_sql_migration_service_smoke")
        .fields()
        .longField("id").isPrimaryKey().end()
        .stringField("code").size(64).nullable().end()
        .end()
        .build();
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
}
