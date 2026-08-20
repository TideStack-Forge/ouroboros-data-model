package com.ouroboros.data.orchestration;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.orchestration.JoinCapability;
import com.ouroboros.data.orchestration.JoinCapabilityResult;

/**
 * 测试用 JoinCapability 实现
 *
 * <p>当两个 DataModel 的 DataStation 名称相同且非 null 时返回 joinable。
 * 用于集成测试中使 DefaultPopulateStrategySelector 选择 JoinPopulateStrategy。
 */
public class TestJoinCapability implements JoinCapability {

  @Override
  public JoinCapabilityResult canJoin(DataModel source, DataModel target) {
    if (source.getDataStation() != null && target.getDataStation() != null) {
      String sourceName = source.getDataStation().getName();
      String targetName = target.getDataStation().getName();
      if (sourceName != null && sourceName.equals(targetName)) {
        return JoinCapabilityResult.joinable();
      }
    }
    return JoinCapabilityResult.notJoinable("test: different data station");
  }

  @Override
  public int getPriority() {
    return 1;
  }
}
