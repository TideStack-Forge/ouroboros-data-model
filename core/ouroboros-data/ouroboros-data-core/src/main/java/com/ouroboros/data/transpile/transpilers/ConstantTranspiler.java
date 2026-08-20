package com.ouroboros.data.transpile.transpilers;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;

public class ConstantTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    var value = sExpr.getParam(0);
    if (value instanceof Boolean bool) {
      return Try.success(Expressions.asBoolean(bool));
    }
    return Try.success(Expressions.constant(value));
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.CONSTANT == sExpr.getOperator();
  }
}
