package com.ouroboros.data.adapter;

import java.util.Optional;

import javax.sql.DataSource;

import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.station.SqlDataStation;

public class SqlDataAdapterBuilder implements DataAdapterBuilder<DataSource> {

  @Override
  public Optional<DataAdapter> apply(DataStation dataStation) {
    if (dataStation.getDataSource() instanceof DataSource dataSource) {
      String dialect = dataStation instanceof SqlDataStation sqlStation ? sqlStation.getDialect() : null;
      return Optional.of(new SqlDataAdapter(dataSource, dialect));
    }
    return Optional.empty();
  }
}
