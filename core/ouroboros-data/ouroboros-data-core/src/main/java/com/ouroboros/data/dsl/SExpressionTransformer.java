package com.ouroboros.data.dsl;

@FunctionalInterface
public interface SExpressionTransformer {
  SExpression<?> transform(SExpression<?> expression, SExpressionTraversalContext context);
}
