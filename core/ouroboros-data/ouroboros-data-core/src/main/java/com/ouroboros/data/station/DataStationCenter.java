package com.ouroboros.data.station;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.ouroboros.data.util.DataServices;

public class DataStationCenter {
  private DataStationCenter() {
  }

  public static Optional<DataStation<?>> getDataStation(String name) {
    return DataServices.getCachedReversedServiceStream(DataStationProvider.class)
        .map(provider -> provider.getDataStation(name))
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(Optional.empty());
  }

  public static Map<String, DataStation<?>> getDataStationMap() {
    return DataServices.getCachedReversedServiceStream(DataStationProvider.class)
        .map(DataStationProvider::getDataStationMap)
        .reduce(new TreeMap<>(String::compareToIgnoreCase), (map1, map2) -> {
          map1.putAll(map2);
          return map1;
        });
  }

  public static void refresh() {
    DataStationProviderByDefine.refresh();
  }

  public static class DataStationProviderByDefine implements DataStationProvider {
    static Map<String, DataStation<?>> dataStationMap;

    public DataStationProviderByDefine() {

    }

    private static void refresh() {
      dataStationMap = DataStationDefineCenter.getDataStationDefineMap().values().stream()
          .map(DataStationBuilder::build)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(() -> new TreeMap<>(String::compareToIgnoreCase), (map, dataStation) -> map.put(dataStation.getName(), dataStation), Map::putAll);
    }

    @Override
    public Optional<DataStation<?>> getDataStation(String name) {
      return Optional.ofNullable(dataStationMap).map(map -> map.get(name));
    }

    @Override
    public Map<String, DataStation<?>> getDataStationMap() {
      return Optional.ofNullable(dataStationMap).map(Collections::unmodifiableMap).orElse(Collections.emptyMap());
    }
  }
}
