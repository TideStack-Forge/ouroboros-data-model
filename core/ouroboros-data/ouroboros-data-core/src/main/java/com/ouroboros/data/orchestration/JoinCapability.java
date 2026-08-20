package com.ouroboros.data.orchestration;

import com.ouroboros.data.model.DataModel;

/**
 * JOIN 能力判断接口
 *
 * <p>通过 SPI 机制扩展，用于 Plan 阶段判断两个模型是否可以 JOIN。
 *
 * @since 1.0.0-beta.2
 */
public interface JoinCapability {

  /**
   * 判断两个模型是否可以 JOIN
   *
   * @param source 源模型（主表）
   * @param target 目标模型（关联表）
   * @return JOIN 能力判断结果
   */
  JoinCapabilityResult canJoin(DataModel source, DataModel target);

  /**
   * 优先级（数值越小优先级越高）
   *
   * @return 优先级值
   */
  default int getPriority() {
    return 100;
  }
}
