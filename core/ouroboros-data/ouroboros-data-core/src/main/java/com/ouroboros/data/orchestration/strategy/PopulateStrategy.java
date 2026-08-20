package com.ouroboros.data.orchestration.strategy;

import java.util.List;

import com.ouroboros.data.orchestration.step.QueryStep;

/**
 * 填充策略
 *
 * <p>职责：
 * <ul>
 *   <li>根据填充字段信息创建执行步骤</li>
 *   <li>封装填充逻辑的差异（JOIN vs Separate）</li>
 * </ul>
 *
 * <p>不负责：
 * <ul>
 *   <li>策略选择（由 PopulateStrategySelector 负责）</li>
 *   <li>具体执行（由创建的 QueryStep 负责）</li>
 * </ul>
 *
 * @author Claude Code
 */
public interface PopulateStrategy {

  /**
   * 创建填充相关的 Step 列表
   *
   * @param populate       填充字段信息
   * @param sourceStepName 数据源 Step 名称（通常是 "main"）
   * @return Step 列表
   */
  List<QueryStep> createSteps(PopulateField populate, String sourceStepName);
}
