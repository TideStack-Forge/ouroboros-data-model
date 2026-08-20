package com.ouroboros.data.normalize.builders;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.SExpressionBuilder;
import com.querydsl.core.types.Operator;

/**
 * 比较操作符表达式构建器
 * <p>
 * 处理: EQ, NE, GT, GTE, LT, LTE, BETWEEN
 */
public class ComparisonExpressionBuilder implements SExpressionBuilder {

  private static final Set<Operator> SUPPORTED = new HashSet<>(Arrays.asList(
      Operators.EQ, Operators.NE, Operators.GT, Operators.GTE,
      Operators.LT, Operators.LTE, Operators.BETWEEN,
      Operators.IS_NULL, Operators.IS_NOT_NULL
  ));

  @Override
  public boolean supports(Operator operator) {
    return SUPPORTED.contains(operator);
  }

  @Override
  public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
    if (operator == Operators.BETWEEN) {
      if (params.size() != 3) {
        throw new NormalizeException("BETWEEN操作符需要3个参数（字段, 下界, 上界），得到 " + params.size() + " 个");
      }
    } else if (operator == Operators.IS_NULL || operator == Operators.IS_NOT_NULL) {
      if (params.size() != 1) {
        throw new NormalizeException(operator + " 操作符需要1个参数，得到 " + params.size() + " 个");
      }
    } else if (params.size() != 2) {
      throw new NormalizeException(operator + " 操作符需要2个参数，得到 " + params.size() + " 个");
    }
    return SExpression.create(operator, params);
  }
}
