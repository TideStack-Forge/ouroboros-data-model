package com.ouroboros.data.orchestration.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ouroboros.data.orchestration.step.PopulateStep;
import com.ouroboros.data.orchestration.step.QueryStep;

/**
 * 独立查询填充策略
 *
 * <p>职责：
 * <ul>
 *   <li>创建 PopulateStep 执行独立查询</li>
 *   <li>适用于跨数据源关联或 ToMany 关联</li>
 * </ul>
 *
 * <p>执行流程：
 * <ul>
 *   <li>PopulateStep 提取外键列表</li>
 *   <li>PopulateStep 执行批量查询（WHERE remotePrimaryKey IN (...)）</li>
 *   <li>PopulateStep 按外键分组</li>
 *   <li>PopulateStep 创建转换器存入 Context</li>
 * </ul>
 *
 * <p>设计要点：
 * <ul>
 *   <li>不直接修改结果，只创建转换器</li>
 *   <li>支持批量查询（避免 N+1）</li>
 *   <li>自动分批处理（IN > 1000）</li>
 * </ul>
 *
 * @author Claude Code
 */
public class SeparatePopulateStrategy implements PopulateStrategy {

  @Override
  public List<QueryStep> createSteps(PopulateField populate, String sourceStepName) {
    // 创建单个 PopulateStep
    String stepName = "populate_" + populate.fieldName();
    PopulateStep step = new PopulateStep(stepName, populate, sourceStepName);

    return Collections.singletonList(step);
  }
}
