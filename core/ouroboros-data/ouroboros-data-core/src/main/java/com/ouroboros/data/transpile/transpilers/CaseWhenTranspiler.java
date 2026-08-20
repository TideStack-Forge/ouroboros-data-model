package com.ouroboros.data.transpile.transpilers;

import java.util.ArrayList;
import java.util.List;

import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;

public class CaseWhenTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    List<Try<Tuple2<Predicate, Expression<?>>>> whenList = new ArrayList<>();
    Try<Expression<?>> otherwise = Try.success(Expressions.nullExpression());

    // 生成 when then 元组和 else
    for (int i = 0; i < sExpr.getParams().size(); i++) {
      var param = sExpr.getParamAsSExpression(i);
      if (Operators.WHEN == param.getOperator()) {
        var condition = context.transpilePredicate(param.getParamAsSExpression(0));
        var result = context.transpile(param.getParamAsSExpression(1));
        Try<Tuple2<Predicate, Expression<?>>> when = Try.of(() -> new Tuple2<>(condition.get(), result.get()));
        whenList.add(when);
      } else if (Operators.ELSE == param.getOperator()) {
        otherwise = context.transpile(param.getParamAsSExpression(0));
      }
    }

    if (otherwise.isFailure()) {
      return otherwise;
    }

    Try<Expression<?>> finalOtherwise = otherwise;
    return Try.sequence(whenList)
        .flatMap(list -> {
          CaseBuilder caseBuilder = new CaseBuilder();
          CaseBuilder.Cases cases = null;
          for (var when : list) {
            cases = caseBuilder.when(when._1)
                .then(when._2);
          }
          if (cases == null) {
            return finalOtherwise;
          }
          return finalOtherwise.<Expression<?>>map(cases::otherwise);
        });
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.CASE == sExpr.getOperator();
  }
}
