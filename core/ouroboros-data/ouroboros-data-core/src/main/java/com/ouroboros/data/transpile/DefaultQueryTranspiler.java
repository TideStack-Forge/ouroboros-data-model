package com.ouroboros.data.transpile;

import java.util.stream.Stream;

import jakarta.annotation.Priority;

import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.clausebuilder.*;

@Priority(0)
public class DefaultQueryTranspiler implements QueryTranspiler {
  final ClauseTranspiler CLAUSE_TRANSPILER_CHAIN;

  public DefaultQueryTranspiler() {
    CLAUSE_TRANSPILER_CHAIN = Stream.<ClauseTranspiler>of(
            new WithBuilder(),
            new UnionBuilder(),
            new FromBuilder(),
            new JoinBuilder(),
            new WhereBuilder(),
            new GroupBuilder(),
            new HavingBuilder(),
            new DistinctBuilder(),
            new SelectBuilder(),
            new OrderBuilder(),
            new PaginationBuilder())
        .reduce((prev, curr) -> new ClauseTranspiler() {
          @Override
          public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(
              QueryStatement query,
              OuroborosQueryMetadata metadata,
              TranspileContext context) {
            var prevResult = prev.applyWithContext(query, metadata, context);
            if (prevResult.isFailure()) {
              return prevResult;
            }
            var newMetadata = prevResult.get()._1();
            var newContext = prevResult.get()._2();
            return curr.applyWithContext(query, newMetadata, newContext);
          }
        }).get();
  }

  @Override
  public Try<OuroborosQueryMetadata> applyWithContext(QueryStatement query, TranspileContext context) {
    var metadata = new DefaultOuroborosQueryMetadata();
    return CLAUSE_TRANSPILER_CHAIN.applyWithContext(query, metadata, context)
        .map(Tuple2::_1);
  }
}
