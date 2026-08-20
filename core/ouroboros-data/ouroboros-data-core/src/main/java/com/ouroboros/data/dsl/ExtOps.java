package com.ouroboros.data.dsl;

import com.querydsl.core.types.Operator;

/**
 * 扩展操作符
 *
 * <p>定义了 QueryDSL 标准操作符之外的自定义操作符
 */
public enum ExtOps implements Operator {
  // === 基础操作符 ===
  FIELD(Object.class),
  COLUMNS(Object.class),
  CONSTANT(Object.class),
  NOT_LIKE(Boolean.class),
  SUB_QUERY(Object.class),

  // === 关联查询操作符 (v1.2) ===
  /**
   * 任意子记录满足条件
   * <p>
   * 生成 EXISTS 子查询。
   *
   * @since 1.0.0-beta.2
   */
  REL_ANY(Boolean.class),

  /**
   * 所有子记录满足条件
   * <p>
   * 生成 NOT EXISTS ... WHERE NOT 子查询。
   *
   * @since 1.0.0-beta.2
   */
  REL_ALL(Boolean.class),

  /**
   * 无子记录满足条件
   * <p>
   * 生成 NOT EXISTS 子查询。
   *
   * @since 1.0.0-beta.2
   */
  REL_NONE(Boolean.class);

  private final Class<?> type;

  ExtOps(Class<?> clazz) {
    type = clazz;
  }

  @Override
  public Class<?> getType() {
    return type;
  }
}
