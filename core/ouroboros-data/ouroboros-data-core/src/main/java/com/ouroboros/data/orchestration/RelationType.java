package com.ouroboros.data.orchestration;

/**
 * 关系类型枚举
 * <p>
 * 定义数据模型之间的关系类型。
 * </p>
 */
public enum RelationType {
  /**
   * 一对一或多对一关系
   */
  TO_ONE,

  /**
   * 一对多关系
   */
  TO_MANY
}
