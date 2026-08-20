package com.ouroboros.data.model;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.ouroboros.data.util.DataServices;

public class DataModelMetaCenter {
  private DataModelMetaCenter() {
  }

  public static Optional<DataModelMeta> getDataModelMeta(String modelName) {
    return DataServices.getCachedReversedServiceStream(DataModelMetaProvider.class)
        .map(provider -> provider.getDataModelMeta(modelName))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  public static Map<String, DataModelMeta> getDataModelMetaMap() {
    return DataServices.getCachedSortedServiceStream(DataModelMetaProvider.class)
        .map(DataModelMetaProvider::getDataModelMetaMap)
        .collect(() -> new TreeMap<>(String::compareToIgnoreCase), Map::putAll, Map::putAll);
  }
}
