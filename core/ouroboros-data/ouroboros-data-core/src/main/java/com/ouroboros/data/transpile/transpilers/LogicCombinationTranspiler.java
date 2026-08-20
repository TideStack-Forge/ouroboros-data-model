package com.ouroboros.data.transpile.transpilers;

import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.TranspileException;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;

public class LogicCombinationTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    if (sExpr.getOperator() == Operators.NOT) {
      if (sExpr.getParams().size() != 1) {
        return Try.failure(new TranspileException(
            "NOT 表达式只接受 1 个参数，实际: " + sExpr.getParams().size()));
      }
      Object param = sExpr.getParam(0);
      if (!(param instanceof SExpression<?> predicateExpr)) {
        return Try.failure(new TranspileException(
            "NOT 参数必须是 SExpression，实际: " + param.getClass().getName()));
      }
      return context.transpilePredicate(predicateExpr)
          .map(predicate -> Expressions.predicate(Operators.NOT, predicate));
    }

    var params = sExpr.getParams()
        .stream()
        .map(param -> context.transpilePredicate((SExpression<?>) param))
        .collect(Collectors.toList());
    return Try.sequence(params)
        .map(list -> list.reduce((a, b) -> Expressions.predicate(sExpr.getOperator(), a, b)));
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.isLogicCombinationOperator(sExpr.getOperator());
  }
}
