package com.ouroboros.data.station;

import java.util.Map;
import java.util.Optional;

/**
 * 数据源工厂
 *
 * @author Song Mingxu
 * @version 1.0.0
 */
public interface DataStationProvider {
  Optional<DataStation<?>> getDataStation(String name);

  Map<String, DataStation<?>> getDataStationMap();

}
