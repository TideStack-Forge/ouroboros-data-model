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
import com.querydsl.core.types.dsl.CaseForEqBuilder;
import com.querydsl.core.types.dsl.Expressions;

public class CaseEqTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    List<Try<Tuple2<Expression<?>, Expression<?>>>> whenList = new ArrayList<>();
    Try<Expression<?>> otherwise = Try.success(Expressions.nullExpression());
    Try<Expression<?>> value = context.transpile(sExpr.getParamAsSExpression(0));

    // 生成 when then 元组和 else
    for (int i = 1; i < sExpr.getParams().size(); i++) {
      var param = sExpr.getParamAsSExpression(i);
      if (Operators.CASE_EQ_WHEN == param.getOperator()) {
        var condition = context.transpile(param.getParamAsSExpression(0));
        var result = context.transpile(param.getParamAsSExpression(1));
        Try<Tuple2<Expression<?>, Expression<?>>> when = Try.of(() -> new Tuple2<>(condition.get(), result.get()));
        whenList.add(when);
      } else if (Operators.ELSE == param.getOperator()) {
        otherwise = context.transpile(param.getParamAsSExpression(0));
      }
    }

    if (value.isFailure()) {
      return value;
    }

    if (otherwise.isFailure()) {
      return otherwise;
    }

    if (whenList.isEmpty()) {
      return otherwise;
    }

    Expression<?> finalOtherwise = otherwise.get();
    Expression<?> finalValue = value.get();
    return Try.sequence(whenList)
        .map(list -> {
          var first = list.get(0);
          var cases = new CaseForEqBuilder<Object>((Expression<Object>) finalValue, first._1)
              .then(first._2);
          for (int i = 1; i < list.size(); i++) {
            var when = list.get(i);
            cases = cases.when(when._1)
                .then((Expression) when._2);
          }
          return cases.otherwise((Expression) finalOtherwise);
        });
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.CASE_EQ == sExpr.getOperator();
  }
}
