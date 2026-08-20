package com.ouroboros.data.station;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StaticDataStationProvider implements DataStationProvider {
  final static Map<String, DataStation<?>> stations = new HashMap<>();

  public static void register(String name, DataStation<?> station) {
    stations.put(name, station);
  }

  @Override
  public Optional<DataStation<?>> getDataStation(String name) {
    return Optional.ofNullable(stations.get(name));
  }

  @Override
  public Map<String, DataStation<?>> getDataStationMap() {
    return stations;
  }
}
