package com.ouroboros.data.dsl;

@FunctionalInterface
public interface SExpressionPredicate {
  boolean test(SExpression<?> expression, SExpressionTraversalContext context);
}
