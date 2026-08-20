package com.ouroboros.data.transpile.transpilers;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.QueryTranspiler;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.SubqueryTranspileContext;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.SubQueryExpressionImpl;

public class SubQueryTranspiler implements SExpressionTranspiler {

  @Override
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    var subQuery = (QueryStatement) sExpr.getParams().get(0);
    QueryTranspiler transpiler = context.getQueryTranspiler();
    SubqueryTranspileContext subCtx = new SubqueryTranspileContext(context);
    return transpiler.applyWithContext(subQuery, subCtx)
        .map(q -> new SubQueryExpressionImpl<>(Object.class, q));
  }


  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.SUB_QUERY == sExpr.getOperator();
  }
}
