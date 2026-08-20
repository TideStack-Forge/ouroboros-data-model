package com.ouroboros.data.dsl;

import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;

public final class Operators {
  public static final Operator COLUMNS = ExtOps.COLUMNS;
  public static final Operator ALIAS = Ops.ALIAS;
  public static final Operator FIELD = ExtOps.FIELD;
  public static final Operator CONSTANT = ExtOps.CONSTANT;
  public static final Operator SUB_QUERY = ExtOps.SUB_QUERY;

  /**
   * 比较运算符
   */
  public static final Operator EQ = Ops.EQ;
  public static final Operator NE = Ops.NE;
  public static final Operator GT = Ops.GT;
  public static final Operator GTE = Ops.GOE;
  public static final Operator LT = Ops.LT;
  public static final Operator LTE = Ops.LOE;
  public static final Operator LIKE = Ops.LIKE;
  public static final Operator NOT_LIKE = ExtOps.NOT_LIKE;
  public static final Operator BETWEEN = Ops.BETWEEN;
  public static final Operator STARTS_WITH = Ops.STARTS_WITH;
  public static final Operator ENDS_WITH = Ops.ENDS_WITH;

  /**
   * 集合运算符
   */
  public static final Operator IN = Ops.IN;
  public static final Operator NOT_IN = Ops.NOT_IN;

  /**
   * 空值运算符
   */
  public static final Operator IS_NULL = Ops.IS_NULL;
  public static final Operator IS_NOT_NULL = Ops.IS_NOT_NULL;

  /**
   * 存在运算符
   */
  public static final Operator EXISTS = Ops.EXISTS;

  /**
   * 逻辑运算符
   */
  public static final Operator NOT = Ops.NOT;
  public static final Operator AND = Ops.AND;
  public static final Operator OR = Ops.OR;
  public static final Operator XOR = Ops.XOR;
  public static final Operator XNOR = Ops.XNOR;

  /**
   * 其他
   */
  public static final Operator CASE = Ops.CASE;
  public static final Operator WHEN = Ops.CASE_WHEN;
  public static final Operator ELSE = Ops.CASE_ELSE;
  public static final Operator CASE_EQ = Ops.CASE_EQ;
  public static final Operator CASE_EQ_WHEN = Ops.CASE_EQ_WHEN;
  public static final Operator CASE_EQ_ELSE = Ops.CASE_EQ_ELSE;

  /**
   * 聚合操作符
   *
   * <p><b>v1.2 实现状态</b>:
   * <ul>
   *   <li>✅ COUNT: 已实现（WP6）</li>
   *   <li>⏳ SUM, AVG, MAX, MIN: 计划在 v1.3 实现</li>
   * </ul>
   */
  public static final Operator COUNT = Ops.AggOps.COUNT_AGG;
  public static final Operator SUM = Ops.AggOps.SUM_AGG;
  public static final Operator AVG = Ops.AggOps.AVG_AGG;
  public static final Operator MAX = Ops.AggOps.MAX_AGG;
  public static final Operator MIN = Ops.AggOps.MIN_AGG;

  public static boolean isLogicCombinationOperator(Operator operator) {
    return operator == AND || operator == OR || operator == XOR || operator == XNOR || operator == NOT;
  }

  public static boolean isComparisonOperator(Operator operator) {
    return operator == EQ || operator == NE || operator == GT || operator == GTE || operator == LT || operator == LTE
        || operator == BETWEEN;
  }

  public static boolean isStringComparisonOperator(Operator operator) {
    return operator == LIKE || operator == NOT_LIKE || operator == STARTS_WITH || operator == ENDS_WITH;
  }

  /**
   * 判断是否为聚合操作符
   */
  public static boolean isAggregationOperator(Operator operator) {
    return operator == COUNT || operator == SUM || operator == AVG
        || operator == MAX || operator == MIN;
  }
}
