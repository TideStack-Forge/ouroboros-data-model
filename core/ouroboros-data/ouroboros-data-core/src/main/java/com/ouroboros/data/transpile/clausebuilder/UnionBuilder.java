package com.ouroboros.data.transpile.clausebuilder;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.exception.TranspileException;

public class UnionBuilder implements ClauseTranspiler {
  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(QueryStatement query, OuroborosQueryMetadata queryMetadata, TranspileContext context) {
    var unionList = query.getUnions();
    return Try.of(() -> {
      for (var union : unionList) {
        var unionQuery = context.getQueryTranspiler().applyWithContext(union.getQuery(), context);
        if (unionQuery.isFailure()) {
          throw new TranspileException("Union 子查询转译失败", unionQuery.getCause());
        }
        if (union.isAll()) {
          queryMetadata.addUnionAll(unionQuery.get());
        } else {
          queryMetadata.addUnion(unionQuery.get());
        }
      }
      return Tuple.of(queryMetadata, context);
    });
  }
}
