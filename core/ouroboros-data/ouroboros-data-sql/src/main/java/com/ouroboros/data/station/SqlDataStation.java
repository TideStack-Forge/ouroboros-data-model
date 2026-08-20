package com.ouroboros.data.station;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.control.Either;

import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.migration.SqlMigrationService;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.MigrationStrategy;

@SuppressWarnings("unused")
public class SqlDataStation extends AbstractDataStation<DataSource> implements Migratable {

  private final static Logger logger = LoggerFactory.getLogger(SqlDataStation.class);
  private final MigrationStrategy migrationStrategy;
  private final String dialect;
  protected DataSource dataSource;

  public SqlDataStation(String name, DataSource dataSource, Collection<DataModelMeta> modelMetas) {
    this(name, dataSource, modelMetas, MigrationStrategy.DISABLED, null);
  }

  public SqlDataStation(String name, DataSource dataSource, Collection<DataModelMeta> modelMetas, MigrationStrategy migrationStrategy) {
    this(name, dataSource, modelMetas, migrationStrategy, null);
  }

  public SqlDataStation(String name, DataSource dataSource, Collection<DataModelMeta> modelMetas, MigrationStrategy migrationStrategy, String dialect) {
    super();
    this.migrationStrategy = migrationStrategy;
    this.dialect = dialect;
    setName(name);
    this.dataSource = dataSource;
    setModelMetas(modelMetas);
  }

  public String getDialect() {
    return dialect;
  }

  @Override
  public MigrationStrategy getMigrationStrategy() {
    return migrationStrategy;
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  protected void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  protected void setModelMetas(Collection<DataModelMeta> modelMetas) {
    super.setModelMetas(modelMetas);
  }

  public Either<DataModelException, List<String>> generateMigrationSql(List<DataModel> dataModels) {
    return SqlMigrationService.getDefault().generateMigrationSql(dataSource, "", "Ouroboros", dataModels);
  }

  @Override
  public Either<DataModelException, Boolean> migrate() {
    if (migrationStrategy == MigrationStrategy.DISABLED) {
      return Either.right(false);
    }

    List<DataModel> DataModels = this.getDataModelList().stream()
        .filter(meta -> meta.getMigrationStrategy() == MigrationStrategy.AUTO)
        .collect(Collectors.toList());
    return migrate(DataModels);
  }

  public Either<DataModelException, Boolean> migrateByNames(List<String> modelNames) {
    List<DataModel> dataModelMetas = this.getDataModelList().stream()
        .filter(model -> modelNames.contains(model.getFullName()))
        .collect(Collectors.toList());
    return migrate(dataModelMetas);
  }

  public Either<DataModelException, Boolean> migrate(List<DataModel> dataModels) {
    return SqlMigrationService.getDefault().migrate(dataSource, "", "Ouroboros", dataModels);
  }

}
