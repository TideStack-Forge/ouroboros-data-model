package com.ouroboros.data.expression;

import java.util.Map;

import io.vavr.control.Either;

public class FailingDataExpressionEvaluator implements DataExpressionEvaluator {
  @Override
  public Either<Throwable, Object> evaluate(String expression, Map<String, Object> context) {
    return Either.left(new DataExpressionException("No DataExpressionEvaluator runtime adapter is available"));
  }
}
