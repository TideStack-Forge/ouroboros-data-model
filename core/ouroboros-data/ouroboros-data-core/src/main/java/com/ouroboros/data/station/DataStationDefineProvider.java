package com.ouroboros.data.station;

import java.util.Map;
import java.util.Optional;

public interface DataStationDefineProvider {
  Optional<DataStationDefine> getDataStationDefine(String name);

  Map<String, DataStationDefine> getDataStationDefineMap();
}
