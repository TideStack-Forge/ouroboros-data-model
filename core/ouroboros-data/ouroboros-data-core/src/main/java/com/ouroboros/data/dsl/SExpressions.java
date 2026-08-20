package com.ouroboros.data.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SExpressions {

  private SExpressions() {}

  public static void walk(SExpression<?> expression, SExpressionVisitor visitor) {
    if (expression != null) {
      expression.walk(visitor);
    }
  }

  public static SExpression<?> transform(SExpression<?> expression,
                                         SExpressionTransformer transformer) {
    if (expression == null) {
      return null;
    }
    return expression.transform(transformer);
  }

  public static Optional<SExpression<?>> filter(SExpression<?> expression, SExpressionPredicate keep) {
    return filter(expression, keep, SExpressionTraversalContext.root());
  }

  private static Optional<SExpression<?>> filter(SExpression<?> expression,
                                                 SExpressionPredicate keep,
                                                 SExpressionTraversalContext context) {
    if (expression == null) {
      return Optional.of(SExpression.empty(Boolean.class));
    }
    if (expression.isEmpty()) {
      return Optional.of(SExpression.empty(expression.getDataType()));
    }
    if (!keep.test(expression, context)) {
      return Optional.empty();
    }
    if (Operators.isLogicCombinationOperator(expression.getOperator())) {
      return filterLogicExpression(expression, keep, context);
    }
    return filterNonLogicExpression(expression, keep, context);
  }

  private static Optional<SExpression<?>> filterLogicExpression(SExpression<?> expression,
                                                               SExpressionPredicate keep,
                                                               SExpressionTraversalContext context) {
    List<Object> params = new ArrayList<>();
    for (int i = 0; i < expression.getParams().size(); i++) {
      Object param = expression.getParam(i);
      if (param instanceof SExpression<?> nested) {
        filter(nested, keep, context.child(expression, i))
            .filter(child -> !child.isEmpty())
            .ifPresent(params::add);
      } else {
        params.add(param);
      }
    }
    if (params.isEmpty()) {
      return Optional.empty();
    }
    if (params.size() == 1 && params.get(0) instanceof SExpression<?> single) {
      return Optional.of(single);
    }
    return Optional.of(SExpression.create(expression.getOperator(), params));
  }

  private static Optional<SExpression<?>> filterNonLogicExpression(SExpression<?> expression,
                                                                  SExpressionPredicate keep,
                                                                  SExpressionTraversalContext context) {
    List<Object> params = new ArrayList<>();
    for (int i = 0; i < expression.getParams().size(); i++) {
      Object param = expression.getParam(i);
      if (param instanceof SExpression<?> nested) {
        Optional<SExpression<?>> filtered = filter(nested, keep, context.child(expression, i));
        if (filtered.isEmpty()) {
          return Optional.empty();
        }
        params.add(filtered.get());
      } else {
        params.add(param);
      }
    }
    return Optional.of(SExpression.create(expression.getOperator(), params));
  }
}
