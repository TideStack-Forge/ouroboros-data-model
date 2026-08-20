package com.ouroboros.data.transpile.transpilers;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.Expressions;

public class IsNullTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    var field = context.transpile(sExpr.getParamAsSExpression(0));
    return field.map(f -> Expressions.predicate(Ops.IS_NULL, f));
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.IS_NULL == sExpr.getOperator();
  }
}
