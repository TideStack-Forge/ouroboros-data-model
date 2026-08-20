package com.ouroboros.data.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SExpressionTraversalContext {
  private static final int ROOT_PARAM_INDEX = -1;

  private final int depth;
  private final List<Integer> path;
  private final SExpression<?> parent;
  private final int paramIndex;
  private final List<SExpression<?>> ancestors;

  private SExpressionTraversalContext(
      int depth,
      List<Integer> path,
      SExpression<?> parent,
      int paramIndex,
      List<SExpression<?>> ancestors) {
    this.depth = depth;
    this.path = List.copyOf(path);
    this.parent = parent;
    this.paramIndex = paramIndex;
    this.ancestors = List.copyOf(ancestors);
  }

  public static SExpressionTraversalContext root() {
    return new SExpressionTraversalContext(0, List.of(), null, ROOT_PARAM_INDEX, List.of());
  }

  SExpressionTraversalContext child(SExpression<?> parent, int paramIndex) {
    List<Integer> childPath = new ArrayList<>(path);
    childPath.add(paramIndex);
    List<SExpression<?>> childAncestors = new ArrayList<>(ancestors);
    childAncestors.add(parent);
    return new SExpressionTraversalContext(depth + 1, childPath, parent, paramIndex, childAncestors);
  }

  public int getDepth() {
    return depth;
  }

  public List<Integer> getPath() {
    return path;
  }

  public Optional<SExpression<?>> getParent() {
    return Optional.ofNullable(parent);
  }

  public Optional<Integer> getParamIndex() {
    return isRoot() ? Optional.empty() : Optional.of(paramIndex);
  }

  public List<SExpression<?>> getAncestors() {
    return ancestors;
  }

  public boolean isRoot() {
    return depth == 0;
  }
}
