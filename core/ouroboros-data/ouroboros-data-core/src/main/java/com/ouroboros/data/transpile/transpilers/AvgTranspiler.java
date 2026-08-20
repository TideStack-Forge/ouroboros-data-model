package com.ouroboros.data.transpile.transpilers;

import com.ouroboros.data.dsl.Operators;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;

/**
 * AVG 聚合操作符转译器
 * <p>
 * 将 AVG 表达式转换为 QueryDSL 的 avg() 方法调用。
 *
 * @since 1.0.0-beta.2
 */
public class AvgTranspiler extends AbstractAggregateTranspiler {

  @Override
  protected String getOperatorName() {
    return "AVG";
  }

  @Override
  protected Operator getOperator() {
    return Operators.AVG;
  }

  @Override
  protected Expression<?> trySpecificExpression(Expression<?> transpiled) {
    if (transpiled instanceof NumberExpression) {
      return ((NumberExpression<?>) transpiled).avg();
    }
    return null;
  }

  @Override
  protected Expression<?> getFallbackExpression(Expression<?> transpiled) {
    return Expressions.numberOperation(Double.class, Operators.AVG, transpiled);
  }
}
