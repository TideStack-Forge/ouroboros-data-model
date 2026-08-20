package com.ouroboros.data.transpile.clausebuilder;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.TranspileContext;

public class WhereBuilder implements ClauseTranspiler {
  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(QueryStatement query, OuroborosQueryMetadata queryMetadata, TranspileContext context) {
    return Try.of(() -> {
      var whereSExpr = query.getWhere();
      if (whereSExpr != null && !whereSExpr.isEmpty()) {
        var where = context.transpilePredicate(whereSExpr).getOrElseThrow(e -> e);
        queryMetadata.addWhere(where);
      }
      return Tuple.of(queryMetadata, context);
    });
  }
}
