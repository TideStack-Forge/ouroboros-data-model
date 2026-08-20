package com.ouroboros.data.station;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.ouroboros.data.util.DataServices;

public class DataStationDefineCenter {
  public static Optional<DataStationDefine> getDataStationDefine(String name) {
    return DataServices.getCachedReversedServiceStream(DataStationDefineProvider.class)
        .map(provider -> provider.getDataStationDefine(name))
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(Optional.empty());
  }

  public static Map<String, DataStationDefine> getDataStationDefineMap() {
    return DataServices.getCachedSortedServiceStream(DataStationDefineProvider.class)
        .map(DataStationDefineProvider::getDataStationDefineMap)
        .collect(() -> new TreeMap<>(String::compareToIgnoreCase), Map::putAll, Map::putAll);
  }
}
