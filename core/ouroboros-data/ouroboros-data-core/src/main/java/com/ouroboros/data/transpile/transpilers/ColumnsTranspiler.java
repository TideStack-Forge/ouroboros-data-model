package com.ouroboros.data.transpile.transpilers;

import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;

public class ColumnsTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    var params = sExpr.getParams()
        .stream()
        .map(p -> context.transpile((SExpression<?>) p))
        .collect(Collectors.toList());
    return Try.sequence(params)
        .map(list -> {
          if (list.size() == 1) {
            return list.get(0);
          }
          return Projections.list(list.toJavaList());
        });
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return sExpr.getOperator() == Operators.COLUMNS;
  }
}
