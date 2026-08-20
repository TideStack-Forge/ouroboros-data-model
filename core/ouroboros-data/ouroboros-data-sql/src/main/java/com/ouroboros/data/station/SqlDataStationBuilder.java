package com.ouroboros.data.station;

import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.model.DataModelMetaCenter;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.sql.DataSourceBuilder;

public class SqlDataStationBuilder implements DataStationBuilder {

  @Override
  public Optional<DataStation<?>> apply(DataStationDefine dataStationDefine) {
    if ("sql".equalsIgnoreCase(dataStationDefine.getType())) {
      return DataSourceBuilder.build(dataStationDefine)
          .map(dataSource -> {
            var stationName = dataStationDefine.getName();
            var modelMetas = DataModelMetaCenter.getDataModelMetaMap().values().stream()
                .filter(meta -> stationName.equalsIgnoreCase(Optional
                    .ofNullable(meta.getDataStation())
                    .filter(s -> !s.isEmpty())
                    .orElse("default")))
                .collect(Collectors.toList());
            var migrationStrategy = dataStationDefine.getProperty(String.class, "migrationStrategy").orElse(MigrationStrategy.AUTO.name());
            var migrationStrategyEnum = MigrationStrategy.valueOf(migrationStrategy);
            var dialect = dataStationDefine.getProperty(String.class, "sqlDialect")
                .filter(StringUtils::isNoneBlank)
                .orElse(null);
            return new SqlDataStation(stationName, dataSource, modelMetas, migrationStrategyEnum, dialect);
          });
    }
    return Optional.empty();
  }
}
