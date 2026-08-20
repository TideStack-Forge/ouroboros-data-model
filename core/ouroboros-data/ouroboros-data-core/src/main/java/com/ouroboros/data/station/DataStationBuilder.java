package com.ouroboros.data.station;

import java.util.Optional;
import java.util.function.Function;

import com.ouroboros.data.util.DataServices;

public interface DataStationBuilder extends Function<DataStationDefine, Optional<DataStation<?>>> {
  Function<DataStationDefine, Optional<DataStation<?>>> DATA_STATION_BUILDER_CHAIN = DataServices.getSortedServiceStream(DataStationBuilder.class)
      .map(f -> (Function<DataStationDefine, Optional<DataStation<?>>>) f)
      .reduce((define -> Optional.empty()), DataStationBuilder::concat);

  static Optional<DataStation<?>> build(DataStationDefine define) {
    return DATA_STATION_BUILDER_CHAIN.apply(define);
  }

  static Function<DataStationDefine, Optional<DataStation<?>>> concat(
      Function<DataStationDefine, Optional<DataStation<?>>> first,
      Function<DataStationDefine, Optional<DataStation<?>>> second
  ) {
    return define -> {
      var result = first.apply(define);
      return result.isPresent() ? result : second.apply(define);
    };
  }
}
