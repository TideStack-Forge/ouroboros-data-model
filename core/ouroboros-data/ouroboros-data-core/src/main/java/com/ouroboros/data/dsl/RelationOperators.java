package com.ouroboros.data.dsl;

import com.querydsl.core.types.Operator;

/**
 * 关联查询操作符便捷访问类
 * <p>
 * 用于表达复杂的子表查询条件，支持"任意"、"所有"、"无"等语义。
 * <p>
 * <b>使用示例</b>:
 * <pre>
 * // 输入: {"orderItems": {"$any": {"productName": "iPhone"}}}
 * // 输出: REL_ANY(FIELD("orderItems"), EQ(FIELD("productName"), "iPhone"))
 * </pre>
 *
 * @see ExtOps
 * @since 1.0.0-beta.2
 */
public class RelationOperators {

  /**
   * 任意子记录满足条件
   * <p>
   * 生成 EXISTS 子查询。
   *
   * @see ExtOps#REL_ANY
   */
  public static final Operator REL_ANY = ExtOps.REL_ANY;

  /**
   * 所有子记录满足条件
   * <p>
   * 生成 NOT EXISTS ... WHERE NOT 子查询。
   *
   * @see ExtOps#REL_ALL
   */
  public static final Operator REL_ALL = ExtOps.REL_ALL;

  /**
   * 无子记录满足条件
   * <p>
   * 生成 NOT EXISTS 子查询。
   *
   * @see ExtOps#REL_NONE
   */
  public static final Operator REL_NONE = ExtOps.REL_NONE;

  private RelationOperators() {
    // 工具类，禁止实例化
  }
}
