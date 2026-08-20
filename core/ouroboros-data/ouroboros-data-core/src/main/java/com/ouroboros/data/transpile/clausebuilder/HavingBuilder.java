package com.ouroboros.data.transpile.clausebuilder;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;

public class HavingBuilder implements ClauseTranspiler {
  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(QueryStatement query, OuroborosQueryMetadata queryMetadata, TranspileContext context) {
    var havingSExpr = query.getHaving();
    if (havingSExpr == null || havingSExpr.isEmpty()) {
      return
          Try.success(Tuple.of(queryMetadata, context));
    }

    var havingExpr = SExpressionTranspiler.transpilePredicate(havingSExpr, context);

    return havingExpr.map(having -> {
      queryMetadata.addHaving(having);
      return Tuple.of(queryMetadata, context);
    });
  }
}
