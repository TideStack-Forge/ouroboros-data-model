package com.ouroboros.data.orchestration.strategy;

/**
 * 填充策略选择器
 *
 * <p>根据 {@link PopulateField} 元数据选择合适的 {@link PopulateStrategy}。
 *
 * <p>SPI 扩展点：用户可通过 {@code META-INF/services/} 注册自定义实现。
 *
 * @author Claude Code
 */
public interface PopulateStrategySelector {

  /**
   * 选择填充策略
   *
   * @param populate 填充字段信息
   * @return 选择的策略
   */
  PopulateStrategy select(PopulateField populate);
}
