package com.ouroboros.data.transpile.clausebuilder;

import java.util.function.BiConsumer;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.CTETranspileContext;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.exception.TranspileException;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;

public class WithBuilder implements ClauseTranspiler {

  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(QueryStatement query, OuroborosQueryMetadata queryMetadata, TranspileContext context) {
    var withList = query.getWith();
    var isRecursive = withList.stream().anyMatch(QueryStatement.CTEDefinition::isRecursive);
    BiConsumer<Path<?>, QueryMetadata> withAdder = isRecursive ? queryMetadata::addWithRecursive : queryMetadata::addWith;
    return Try.of(() -> {
      var cteContext = new CTETranspileContext(context);

      for (var with : withList) {
        var alias = Expressions.simplePath(Object.class, with.getAlias());
        var subQuery = cteContext.getQueryTranspiler().applyWithContext(with.getQuery(), cteContext);
        if (subQuery.isFailure()) {
          throw new TranspileException("With 子查询转译失败", subQuery.getCause());
        }
        withAdder.accept(alias, subQuery.get());
        cteContext = cteContext.withCTE(with.getAlias(), subQuery.get());
      }
      return Tuple.of(queryMetadata, cteContext);
    });
  }
}
