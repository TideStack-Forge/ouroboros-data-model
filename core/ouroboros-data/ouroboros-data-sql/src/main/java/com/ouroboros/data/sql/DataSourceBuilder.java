package com.ouroboros.data.sql;

import java.util.Optional;
import java.util.function.Function;

import javax.sql.DataSource;

import com.ouroboros.data.station.DataStationDefine;
import com.ouroboros.data.util.DataServices;

public interface DataSourceBuilder extends Function<DataStationDefine, Optional<DataSource>> {
  Function<DataStationDefine, Optional<DataSource>> DATA_SOURCE_BUILDER_CHAIN = DataServices.getSortedServiceStream(DataSourceBuilder.class)
      .map(builder -> (Function<DataStationDefine, Optional<DataSource>>) builder)
      .reduce(define -> Optional.empty(), DataSourceBuilder::concat);

  static Optional<DataSource> build(DataStationDefine dataStationDefine) {
    return DATA_SOURCE_BUILDER_CHAIN.apply(dataStationDefine);
  }

  static Function<DataStationDefine, Optional<DataSource>> concat(
      Function<DataStationDefine, Optional<DataSource>> first,
      Function<DataStationDefine, Optional<DataSource>> second
  ) {
    return define -> {
      var result = first.apply(define);
      return result.isPresent() ? result : second.apply(define);
    };
  }
}
