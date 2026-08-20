package com.ouroboros.data.orchestration.step;

import com.ouroboros.data.orchestration.OrchestrationContext;

/**
 * 查询执行步骤
 *
 * <p>职责：
 * <ul>
 *   <li>执行单个查询操作</li>
 *   <li>从 OrchestrationContext 获取输入</li>
 *   <li>将结果存入 OrchestrationContext</li>
 * </ul>
 *
 * <p>不负责：
 * <ul>
 *   <li>执行顺序控制（由 QueryPlan 负责）</li>
 *   <li>结果聚合（由 OrchestrationContext 负责）</li>
 * </ul>
 *
 * @author Claude Code
 */
public interface QueryStep {

  /**
   * 获取步骤名称（必须唯一）
   *
   * @return 步骤名称
   */
  String getName();

  /**
   * 执行步骤
   *
   * <p>注意：
   * <ul>
   *   <li>不返回结果，结果存入 context</li>
   *   <li>从 context 获取前置步骤的结果</li>
   *   <li>执行失败抛出异常（由调用方捕获）</li>
   * </ul>
   *
   * @param context 编排上下文
   */
  void execute(OrchestrationContext context);
}
