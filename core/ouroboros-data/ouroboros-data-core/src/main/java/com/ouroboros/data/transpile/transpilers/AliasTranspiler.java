package com.ouroboros.data.transpile.transpilers;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;

public class AliasTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    var alias = sExpr.getParam(1).toString();
    var expr = context.transpile(sExpr.getParamAsSExpression(0));
    return expr.map(origin -> Expressions.as(origin, alias));
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.ALIAS == sExpr.getOperator();
  }
}
