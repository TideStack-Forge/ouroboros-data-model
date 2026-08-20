package com.ouroboros.data.orchestration;

import com.ouroboros.data.model.DataModel;

/**
 * 测试用 JoinCapability：模拟跨 DataStation 但仍允许原生 JOIN 的扩展场景。
 */
public class TestFederatedJoinCapability implements JoinCapability {

  @Override
  public JoinCapabilityResult canJoin(DataModel source, DataModel target) {
    if (source.getDataStation() == null || target.getDataStation() == null) {
      return JoinCapabilityResult.notJoinable("test: data station missing");
    }

    String sourceName = source.getDataStation().getName();
    String targetName = target.getDataStation().getName();
    if ("FederatedA".equals(sourceName) && "FederatedB".equals(targetName)) {
      return JoinCapabilityResult.joinable();
    }
    if (sourceName != null && sourceName.equals(targetName)) {
      return JoinCapabilityResult.joinable();
    }

    return JoinCapabilityResult.notJoinable("test: different data station");
  }

  @Override
  public int getPriority() {
    return 0;
  }
}
