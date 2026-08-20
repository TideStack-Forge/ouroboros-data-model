package com.ouroboros.data.orchestration;

/**
 * JOIN 策略枚举
 *
 * @since 1.0.0-beta.2
 */
public enum JoinStrategy {
  /**
   * 同一数据源，可合并查询（具体用 JOIN 还是 SUBQUERY 由 Transpile 决定）
   */
  NATIVE,

  /**
   * 跨数据源，需分开查询后内存合并
   */
  SEPARATE
}
