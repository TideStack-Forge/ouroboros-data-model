package com.ouroboros.data.pkgenerator;

import java.util.Map;
import java.util.Optional;

import io.vavr.control.Either;

import com.ouroboros.data.expression.DataExpressionEvaluator;

public class TestDataExpressionEvaluator implements DataExpressionEvaluator {
  @Override
  public Either<Throwable, Object> evaluate(String expression, Map<String, Object> context) {
    var key = unwrapExpression(expression);
    if (key.isEmpty()) {
      return Either.right("");
    }
    return Optional.ofNullable(context.get(key))
        .<Either<Throwable, Object>>map(Either::right)
        .orElseGet(() -> Either.left(new IllegalArgumentException("Missing expression variable: " + key)));
  }

  @Override
  public String wrapExpression(String expression) {
    if (expression == null || expression.startsWith("${") && expression.endsWith("}")) {
      return expression;
    }
    return "${ " + expression + " }";
  }

  private String unwrapExpression(String expression) {
    if (expression == null) {
      return "";
    }
    var trimmed = expression.trim();
    if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
      return trimmed.substring(2, trimmed.length() - 1).trim();
    }
    return trimmed;
  }
}
