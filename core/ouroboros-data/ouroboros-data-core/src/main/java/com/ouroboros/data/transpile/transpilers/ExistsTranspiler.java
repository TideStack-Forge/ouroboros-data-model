package com.ouroboros.data.transpile.transpilers;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.TranspileException;
import com.ouroboros.data.transpile.QueryTranspiler;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.SubqueryTranspileContext;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.SubQueryExpressionImpl;
import com.querydsl.core.types.dsl.Expressions;

public class ExistsTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    if (sExpr.getParams().isEmpty()) {
      return Try.failure(new TranspileException("EXISTS 表达式缺少参数"));
    }

    Object param = sExpr.getParam(0);
    if (param instanceof QueryStatement subQuery) {
      QueryTranspiler transpiler = context.getQueryTranspiler();
      SubqueryTranspileContext subCtx = new SubqueryTranspileContext(context);
      return transpiler.applyWithContext(subQuery, subCtx)
          .map(query -> Expressions.predicate(Ops.EXISTS, new SubQueryExpressionImpl<>(Object.class, query)));
    }

    if (param instanceof SExpression<?> subQueryExpr) {
      return context.transpile(subQueryExpr)
          .map(query -> Expressions.predicate(Ops.EXISTS, query));
    }

    return Try.failure(new TranspileException(
        "EXISTS 参数必须是 QueryStatement 或 SExpression，实际: " + param.getClass().getName()));
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.EXISTS == sExpr.getOperator();
  }
}
