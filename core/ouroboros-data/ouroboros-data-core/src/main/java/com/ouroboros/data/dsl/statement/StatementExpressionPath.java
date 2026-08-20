package com.ouroboros.data.dsl.statement;

import java.util.ArrayList;
import java.util.List;

import com.ouroboros.data.dsl.SExpression;

public record StatementExpressionPath(QueryStatement statement, List<Object> segments, SExpression<?> expression) {

  public StatementExpressionPath {
    segments = List.copyOf(segments);
  }

  public StatementExpressionPath append(Object segment, SExpression<?> nextExpression) {
    List<Object> nextSegments = new ArrayList<>(segments);
    nextSegments.add(segment);
    return new StatementExpressionPath(statement, nextSegments, nextExpression);
  }
}
