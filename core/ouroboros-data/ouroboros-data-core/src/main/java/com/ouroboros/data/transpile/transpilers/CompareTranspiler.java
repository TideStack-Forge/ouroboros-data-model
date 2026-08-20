package com.ouroboros.data.transpile.transpilers;

import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;

public class CompareTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    var params = sExpr.getParams().stream()
        .map(p -> context.transpile((SExpression<?>) p))
        .collect(Collectors.toList());
    return Try.sequence(params)
        .map(list -> Expressions.predicate(sExpr.getOperator(), list.get(0), list.get(1)));
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.isComparisonOperator(sExpr.getOperator());
  }
}
