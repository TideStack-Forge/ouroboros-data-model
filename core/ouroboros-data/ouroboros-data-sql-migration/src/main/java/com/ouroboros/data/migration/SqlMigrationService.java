package com.ouroboros.data.migration;

import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import io.vavr.control.Either;

import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.util.DataServices;

public interface SqlMigrationService {
  SqlMigrationService DEFAULT_SERVICE = Objects.requireNonNull(
      DataServices.getPrimaryService(SqlMigrationService.class),
      "data-sql-migration must register a SqlMigrationService implementation"
  );

  static SqlMigrationService getDefault() {
    return DEFAULT_SERVICE;
  }

  Either<DataModelException, List<String>> generateMigrationSql(DataSource dataSource, String context, String author,
                                                                List<DataModel> dataModels);

  Either<DataModelException, Boolean> migrate(DataSource dataSource, String context, String author,
                                              List<DataModel> dataModels);
}
