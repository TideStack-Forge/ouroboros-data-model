package com.ouroboros.data.orchestration;

import java.util.List;
import java.util.stream.Collectors;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.util.DataServices;

/**
 * JOIN 能力判断聚合类
 *
 * @since 1.0.0-beta.2
 */
public class JoinCapabilities {

  private static final List<JoinCapability> CAPABILITIES;

  static {
    CAPABILITIES = DataServices.getSortedServiceStream(JoinCapability.class)
        .collect(Collectors.toList());
  }

  /**
   * 判断两个模型是否可以 JOIN
   */
  public static JoinCapabilityResult canJoin(DataModel source, DataModel target) {
    for (JoinCapability capability : CAPABILITIES) {
      JoinCapabilityResult result = capability.canJoin(source, target);
      if (result.isJoinable() || result.getReason() != null) {
        return result;
      }
    }
    return JoinCapabilityResult.notJoinable("无法确定 JOIN 能力");
  }
}
