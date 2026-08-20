package com.ouroboros.data.expression;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import io.vavr.control.Either;

import com.ouroboros.data.util.DataServices;

public interface DataExpressionEvaluator extends BiFunction<String, Map<String, Object>, Either<Throwable, Object>> {
  DataExpressionEvaluator DEFAULT_EVALUATOR = Optional.ofNullable(DataServices.getPrimaryService(DataExpressionEvaluator.class))
      .orElseGet(FailingDataExpressionEvaluator::new);

  static DataExpressionEvaluator getDefault() {
    return DEFAULT_EVALUATOR;
  }

  static Either<Throwable, Object> eval(String expression, Map<String, Object> context) {
    return getDefault().evaluate(expression, context);
  }

  static String wrap(String expression) {
    return getDefault().wrapExpression(expression);
  }

  Either<Throwable, Object> evaluate(String expression, Map<String, Object> context);

  @Override
  default Either<Throwable, Object> apply(String expression, Map<String, Object> context) {
    return evaluate(expression, context);
  }

  default String wrapExpression(String expression) {
    if (expression == null || expression.startsWith("${") && expression.endsWith("}")) {
      return expression;
    }
    return String.format("${ %s }", expression);
  }
}
