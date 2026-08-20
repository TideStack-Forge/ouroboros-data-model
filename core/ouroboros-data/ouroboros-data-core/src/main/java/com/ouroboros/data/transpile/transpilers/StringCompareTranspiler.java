package com.ouroboros.data.transpile.transpilers;

import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Constant;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;

public class StringCompareTranspiler implements SExpressionTranspiler {
  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    var params = sExpr.getParams().stream()
        .map(p -> context.transpile((SExpression<?>) p))
        .collect(Collectors.toList());

    return Try.sequence(params)
        .map(list -> {
          var operator = sExpr.getOperator();
          var column = list.get(0);
          var value = list.get(1);
          if (operator.equals(Operators.LIKE) || operator.equals(Operators.NOT_LIKE)) {
            if (value instanceof Constant<?> constant && constant.getConstant() instanceof CharSequence strValue && !strValue.toString().contains("%")) {
              value = Expressions.constant("%" + strValue + "%");
            }
          } else if (operator.equals(Operators.STARTS_WITH)) {
            if (value instanceof Constant<?> constant && constant.getConstant() instanceof CharSequence strValue && !strValue.toString().endsWith("%")) {
              value = Expressions.constant(strValue + "%");
            }
          } else if (operator.equals(Operators.ENDS_WITH)) {
            if (value instanceof Constant<?> constant && constant.getConstant() instanceof CharSequence strValue && !strValue.toString().startsWith("%")) {
              value = Expressions.constant("%" + strValue);
            }
          }
          if (operator == Operators.NOT_LIKE) {
            return Expressions.predicate(Operators.LIKE, column, value).not();
          }
          return Expressions.predicate(Operators.LIKE, column, value);
        });
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.isStringComparisonOperator(sExpr.getOperator());
  }
}
