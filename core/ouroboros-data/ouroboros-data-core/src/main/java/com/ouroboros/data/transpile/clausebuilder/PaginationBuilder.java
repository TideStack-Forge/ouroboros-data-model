package com.ouroboros.data.transpile.clausebuilder;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.dsl.Expressions;

public class PaginationBuilder implements ClauseTranspiler {
  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(
      QueryStatement query,
      OuroborosQueryMetadata queryMetadata,
      TranspileContext context) {
    return Try.of(() -> {
      var offset = query.getOffset();
      var limit = query.getLimit();
      if (offset != null) {
        queryMetadata.setOffset(offset);
      }
      if (limit != null) {
        if (limit == 0L) {
          queryMetadata.addWhere(Expressions.ONE.eq(Expressions.ZERO));
        } else {
          queryMetadata.setLimit(limit);
        }
      }
      return Tuple.of(queryMetadata, context);
    });
  }
}
