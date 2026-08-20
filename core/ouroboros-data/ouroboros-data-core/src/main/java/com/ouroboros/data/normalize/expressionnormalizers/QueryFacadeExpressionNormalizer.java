package com.ouroboros.data.normalize.expressionnormalizers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.query.QueryCondition;
import com.ouroboros.data.dsl.query.QueryExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ExpressionNormalizeContext;
import com.ouroboros.data.normalize.RawExpressionNormalizer;

/**
 * Query facade 表达式规范化器。
 */
public class QueryFacadeExpressionNormalizer implements RawExpressionNormalizer {

  private static final Set<String> EXPRESSION_CLAUSE_TYPES =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("WHERE", "HAVING", "QUERY", "EXPRESSION")));

  @Override
  public boolean supports(String clauseType, Class<?> rawExpressionType) {
    return EXPRESSION_CLAUSE_TYPES.contains(clauseType.toUpperCase())
        && (QueryCondition.class.isAssignableFrom(rawExpressionType)
        || QueryExpression.class.isAssignableFrom(rawExpressionType));
  }

  @Override
  public SExpression<?> normalize(Object rawExpression, ExpressionNormalizeContext context) {
    if (rawExpression instanceof QueryCondition condition) {
      Object rawCondition = condition.toRawCondition();
      if (rawCondition == rawExpression) {
        throw new NormalizeException("QueryCondition 不能返回自身作为 raw condition: "
            + rawExpression.getClass().getName());
      }
      return context.getClauseContext().normalizeExpression(rawCondition, context.getExpressionPath()).get();
    }

    if (rawExpression instanceof QueryExpression<?> expression) {
      Object rawValue = expression.toRawValue();
      if (rawValue == rawExpression) {
        throw new NormalizeException("QueryExpression 不能返回自身作为 raw value: "
            + rawExpression.getClass().getName());
      }
      return normalizeQueryExpressionRawValue(rawValue, context);
    }

    return null;
  }

  private SExpression<?> normalizeQueryExpressionRawValue(Object rawValue, ExpressionNormalizeContext context) {
    if (rawValue instanceof CharSequence fieldPath) {
      return buildFieldExpression(fieldPath.toString(), context);
    }
    return context.getClauseContext().normalizeExpression(rawValue, context.getExpressionPath()).get();
  }

  private SExpression<?> buildFieldExpression(String rawFieldPath, ExpressionNormalizeContext context) {
    List<SExpression<?>> segments = new ArrayList<>();
    Arrays.stream(rawFieldPath.split("\\."))
        .map(String::trim)
        .filter(segment -> !segment.isEmpty())
        .forEach(segment -> segments.add(SExpression.constant(segment)));
    if (segments.isEmpty()) {
      throw new NormalizeException("字段表达式不能为空: " + context.getExpressionPath());
    }
    return context.getClauseContext().buildSExpression(
        Operators.FIELD,
        segments,
        context.getExpressionPath()).get();
  }
}
