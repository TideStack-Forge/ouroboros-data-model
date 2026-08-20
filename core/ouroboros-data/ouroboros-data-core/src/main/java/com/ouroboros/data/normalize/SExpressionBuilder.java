package com.ouroboros.data.normalize;

import java.util.List;

import com.ouroboros.data.dsl.SExpression;
import com.querydsl.core.types.Operator;

/**
 * @deprecated 使用 {@link SExpressionNormalizer} 作为 canonical 契约。
 * 该接口保留一轮作为兼容桥。
 *
 * S表达式构建器接口
 * <p>
 * 接收类型化参数（Operator + params），直接组装 SExpression。
 * 不接触 raw expression，不做解析。
 */
@Deprecated
public interface SExpressionBuilder extends SExpressionNormalizer {

  /**
   * 检查是否支持指定的操作符
   *
   * @param operator 已解析的操作符
   * @return true如果支持，false否则
   */
  boolean supports(Operator operator);

  /**
   * 构建S表达式
   *
   * @param operator 已解析的操作符
   * @param params   已规范化的参数列表
   * @param context  表达式规范化上下文
   * @return 构建的S表达式
   */
  SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context);

  @Override
  default SExpression<?> normalize(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
    return build(operator, params, context);
  }
}
