package com.ouroboros.data.orchestration;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.station.DataStation;

/**
 * 默认 JOIN 能力判断：基于 DataStation 是否相同
 *
 * @since 1.0.0-beta.2
 */
public class DefaultJoinCapability implements JoinCapability {

  @Override
  public JoinCapabilityResult canJoin(DataModel source, DataModel target) {
    DataStation<?> sourceStation = source.getDataStation();
    DataStation<?> targetStation = target.getDataStation();

    if (sourceStation == null || targetStation == null) {
      return JoinCapabilityResult.notJoinable("DataStation 为空");
    }

    if (sourceStation.getName().equals(targetStation.getName())) {
      return JoinCapabilityResult.joinable();
    }

    return JoinCapabilityResult.notJoinable(
        "模型位于不同 DataStation: " + sourceStation.getName() + " vs " + targetStation.getName()
    );
  }

  @Override
  public int getPriority() {
    return 1000;
  }
}
