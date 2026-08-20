package com.ouroboros.data.model;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.ouroboros.data.station.DataStationCenter;

public class DataModelCenter {
  private static volatile Map<String, DataModel> dataModelMap;

  private DataModelCenter() {
  }

  public static Optional<DataModel> getDataModel(String name) {
    return Optional.ofNullable(dataModelMap).map(map -> map.get(name));
  }

  public static boolean hasSnapshot() {
    return dataModelMap != null;
  }

  public static Map<String, DataModel> getDataModelMap() {
    return Optional.ofNullable(dataModelMap).map(Collections::unmodifiableMap).orElse(Collections.emptyMap());
  }

  public static void refresh() {
    var newMap = new TreeMap<String, DataModel>(String::compareToIgnoreCase);
    DataStationCenter.getDataStationMap().forEach((name, dataStation) -> {
      dataStation.getDataModelList().forEach(model -> {
        newMap.put(model.getFullName(), model);
        newMap.put(name + ":" + model.getFullName(), model);
      });
    });
    dataModelMap = newMap;
  }
}
