package com.ouroboros.data.normalize;

import java.util.List;

import com.ouroboros.data.dsl.SExpression;
import com.querydsl.core.types.Operator;

/**
 * Normalize 阶段的 S 表达式规范化契约。
 *
 * <p>接收已经类型化的参数（Operator + params），返回规范化后的 {@link SExpression}。
 * 不接触 raw expression，不负责原始结构解析。
 */
public interface SExpressionNormalizer {

  /**
   * 检查是否支持指定操作符。
   *
   * @param operator 已解析的操作符
   * @return true 表示支持
   */
  boolean supports(Operator operator);

  /**
   * 规范化为 SExpression。
   *
   * @param operator 已解析的操作符
   * @param params   已规范化的参数列表
   * @param context  表达式规范化上下文
   * @return 规范化结果
   */
  SExpression<?> normalize(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context);
}
