package com.ouroboros.data.transpile.clausebuilder;

import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.TranspileContext;

public class DistinctBuilder implements ClauseTranspiler {
  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(
      QueryStatement query,
      OuroborosQueryMetadata queryMetadata,
      TranspileContext context) {
    var isDistinct = query.getDistinct();
    queryMetadata.setDistinct(isDistinct);
    return Try.success(new Tuple2<>(queryMetadata, context));
  }
}
