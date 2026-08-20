package com.ouroboros.data.normalize;

import com.ouroboros.data.dsl.SExpression;

/**
 * 原始表达式规范化器接口
 * <p>
 * 负责将原始的表达式对象规范化为S表达式
 */
public interface RawExpressionNormalizer {

  /**
   * 检查是否支持指定的子句类型和原始表达式类型
   *
   * @param clauseType        子句类型（如"WHERE"、"SELECT"等）
   * @param rawExpressionType 原始表达式的Java类型
   * @return true如果支持，false否则
   */
  boolean supports(String clauseType, Class<?> rawExpressionType);

  /**
   * 规范化原始表达式
   *
   * @param rawExpression 原始表达式对象
   * @param context       表达式规范化上下文
   * @return 规范化后的S表达式
   */
  SExpression<?> normalize(Object rawExpression, ExpressionNormalizeContext context);
}
