package com.ouroboros.data.dsl;

import java.util.ArrayList;
import java.util.List;

public record RawExpressionPath(List<Object> segments, Object value) {

  public RawExpressionPath {
    segments = List.copyOf(segments);
  }

  public RawExpressionPath append(Object segment, Object nextValue) {
    List<Object> nextSegments = new ArrayList<>(segments);
    nextSegments.add(segment);
    return new RawExpressionPath(nextSegments, nextValue);
  }

  public RawExpressionPath append(Object segment) {
    return append(segment, value);
  }
}
