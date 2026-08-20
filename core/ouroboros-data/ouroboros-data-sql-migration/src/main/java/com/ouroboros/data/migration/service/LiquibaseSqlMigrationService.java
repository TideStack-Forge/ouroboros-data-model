package com.ouroboros.data.migration.service;

import java.util.List;

import javax.sql.DataSource;

import io.vavr.control.Either;
import io.vavr.control.Try;

import com.ouroboros.data.exception.ConnectionException;
import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.migration.DatabaseMigration;
import com.ouroboros.data.migration.SqlMigrationService;
import com.ouroboros.data.model.DataModel;
public class LiquibaseSqlMigrationService implements SqlMigrationService {
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
  public Either<DataModelException, Boolean> migrate(DataSource dataSource, String context, String author,
                                                     List<DataModel> dataModels) {
    var connEither = Try.of(dataSource::getConnection)
        .toEither()
        .<DataModelException>mapLeft(e -> new ConnectionException("获取数据库连接失败", e));
    var result = connEither
        .map(conn -> new DatabaseMigration(conn, context, author))
        .flatMap(dm -> dm.migrate(dataModels))
        .map(v -> true);
    connEither.peek(conn -> Try.run(conn::close));
    return result;
  }
}
