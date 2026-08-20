package com.ouroboros.data.adapter;

import java.util.Optional;
import java.util.function.Function;

import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.util.DataServices;

/**
 * 数据访问适配器构建器
 *
 * @author Song Mingxu
 * @version 1.0.0
 */
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public interface DataAdapterBuilder<T> extends Function<DataStation<T>, Optional<DataAdapter>> {
  Function<DataStation, Optional<DataAdapter>> DATA_ACCESS_ADAPTER_BUILDER_CHAIN = DataServices.getSortedServiceStream(DataAdapterBuilder.class)
      .map(f -> (Function<DataStation, Optional<DataAdapter>>) f)
      .reduce(dataSource -> Optional.empty(), (prev, curr) -> ds -> {
        var adapter = curr.apply(ds);
        return adapter.isPresent() ? adapter : prev.apply(ds);
      });

  /**
   * 获取数据访问适配器
   *
   * @param dataStation 数据源
   * @return 数据访问适配器
   */
  static Optional<DataAdapter> getDataAdapter(DataStation dataStation) {
    return DATA_ACCESS_ADAPTER_BUILDER_CHAIN.apply(dataStation);
  }

  Optional<DataAdapter> apply(DataStation<?> dataStation);
}
