package com.ouroboros.data.transpile.transpilers;

import com.ouroboros.data.dsl.Operators;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.Expressions;

/**
 * MAX 聚合操作符转译器
 * <p>
 * 将 MAX 表达式转换为 QueryDSL 的 max() 方法调用。
 *
 * @since 1.0.0-beta.2
 */
public class MaxTranspiler extends AbstractAggregateTranspiler {

  @Override
  protected String getOperatorName() {
    return "MAX";
  }

  @Override
  protected Operator getOperator() {
    return Operators.MAX;
  }

  @Override
  protected Expression<?> trySpecificExpression(Expression<?> transpiled) {
    if (transpiled instanceof ComparableExpressionBase) {
      return ((ComparableExpressionBase<?>) transpiled).max();
    }
    return null;
  }

  @Override
  protected Expression<?> getFallbackExpression(Expression<?> transpiled) {
    return Expressions.comparableOperation(Comparable.class, Operators.MAX, transpiled);
  }
}
