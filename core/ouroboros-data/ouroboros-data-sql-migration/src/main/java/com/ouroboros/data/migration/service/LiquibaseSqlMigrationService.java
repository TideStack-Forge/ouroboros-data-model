package com.ouroboros.data.migration.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import io.vavr.control.Either;
import io.vavr.control.Try;

import com.ouroboros.data.exception.ConnectionException;
import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.exception.DatabaseMigrationException;
import com.ouroboros.data.migration.DatabaseMigration;
import com.ouroboros.data.migration.SqlMigrationService;
import com.ouroboros.data.model.DataModel;

public class LiquibaseSqlMigrationService implements SqlMigrationService {
  private static final Object MIGRATION_LOCK = new Object();

  @Override
  public Either<DataModelException, List<String>> generateMigrationSql(DataSource dataSource, String context, String author,
                                                                       List<DataModel> dataModels) {
    var connEither = Try.of(dataSource::getConnection)
        .toEither()
        .<DataModelException>mapLeft(e -> new ConnectionException("获取数据库连接失败", e));
    var result = connEither
        .map(conn -> new DatabaseMigration(conn, context, author))
        .flatMap(dm -> dm.getMigrationSqls(dataModels));
    connEither.peek(conn -> Try.run(conn::close));
    return result;
  }

  @Override
  public Either<DataModelException, Boolean> hasRequiredUniqueColumn(DataSource dataSource, DataModel model,
      String rawColumnName) {
    try (var connection = dataSource.getConnection()) {
      return new DatabaseMigration(connection, "", "Ouroboros").hasRequiredUniqueColumn(model, rawColumnName);
    } catch (SQLException ex) {
      return Either.left(new ConnectionException("无法核验数据库字段约束", ex));
    }
  }

  @Override
  public Either<DataModelException, Boolean> migrate(DataSource dataSource, String context, String author,
                                                     List<DataModel> dataModels) {
    var connEither = Try.of(dataSource::getConnection)
        .toEither()
        .<DataModelException>mapLeft(e -> new ConnectionException("获取数据库连接失败", e));
    if (connEither.isLeft()) {
      return Either.left(connEither.getLeft());
    }
    Connection connection = connEither.get();
    try {
      synchronized (MIGRATION_LOCK) {
        var result = new DatabaseMigration(connection, context, author)
            .migrate(dataModels)
            .flatMap(v -> verifyMigratedTables(connection, dataModels))
            .map(v -> true);
        return result;
      }
    } finally {
      Try.run(connection::close);
    }
  }

  private Either<DataModelException, Void> verifyMigratedTables(Connection connection, List<DataModel> dataModels) {
    return Try.run(() -> {
      for (DataModel dataModel : dataModels == null ? List.<DataModel>of() : dataModels) {
        String tableName = dataModel == null ? null : dataModel.getRawName();
        if (StringUtils.isBlank(tableName)) {
          continue;
        }
        if (!tableExists(connection, tableName)) {
          throw new DatabaseMigrationException("数据库迁移后物理表不可见: " + tableName);
        }
      }
    }).toEither().mapLeft(e -> e instanceof DataModelException
        ? (DataModelException) e
        : new DatabaseMigrationException("验证数据库迁移结果失败", e));
  }

  private boolean tableExists(Connection connection, String tableName) throws SQLException {
    var metadata = connection.getMetaData();
    if (tableExists(connection, connection.getCatalog(), null, tableName)) {
      return true;
    }
    String schema = Try.of(connection::getSchema).getOrNull();
    if (StringUtils.isNotBlank(schema) && tableExists(connection, null, schema, tableName)) {
      return true;
    }
    try (var tables = metadata.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (tables.next()) {
        if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean tableExists(Connection connection, String catalog, String schema, String tableName) throws SQLException {
    try (var tables = connection.getMetaData().getTables(catalog, schema, tableName, new String[] {"TABLE"})) {
      return tables.next();
    }
  }
}
