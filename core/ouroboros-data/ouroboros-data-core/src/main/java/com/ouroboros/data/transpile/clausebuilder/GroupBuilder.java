package com.ouroboros.data.transpile.clausebuilder;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;

public class GroupBuilder implements ClauseTranspiler {
  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(QueryStatement query, OuroborosQueryMetadata queryMetadata, TranspileContext context) {
    return Try.of(() -> {
      var group = query.getGroup();
      if (group != null && !group.isEmpty()) {
        var groupExpr = SExpressionTranspiler.transpile(group, context).getOrElseThrow(e -> e);
        queryMetadata.addGroupBy(groupExpr);
      }
      return Tuple.of(queryMetadata, context);
    });
  }
}
