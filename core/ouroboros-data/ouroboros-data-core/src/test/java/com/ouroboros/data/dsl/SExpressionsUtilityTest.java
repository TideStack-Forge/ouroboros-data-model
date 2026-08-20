package com.ouroboros.data.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.statement.QueryStatement;

class SExpressionsUtilityTest {

  @Test
  void filterShouldPruneDroppedBooleanChildrenAndCollapseSingleChild() {
    SExpression<Boolean> keep = SExpression.create(Operators.EQ,
        SExpression.field("name"),
        SExpression.constant("Alice"));
    SExpression<Boolean> drop = SExpression.create(Operators.EQ,
        SExpression.field("unknown"),
        SExpression.constant(""));
    SExpression<Boolean> where = SExpression.create(Operators.AND, keep, drop);

    SExpression<?> filtered = SExpressions.filter(where, (expression, context) ->
        !(expression.getOperator() == Operators.FIELD && "unknown".equals(expression.getParam(0)))
            && !(expression.getOperator() == Operators.CONSTANT && "".equals(expression.getParam(0))))
        .orElseThrow();

    assertEquals(Operators.EQ, filtered.getOperator());
    assertEquals("name", ((SExpression<?>) filtered.getParam(0)).getParam(0));
  }

  @Test
  void filterShouldExposeTraversalContext() {
    SExpression<Boolean> where = SExpression.create(Operators.AND,
        SExpression.create(Operators.EQ, SExpression.field("keep"), SExpression.constant("v")),
        SExpression.create(Operators.EQ, SExpression.field("drop"), SExpression.constant("v")));

    SExpression<?> filtered = SExpressions.filter(where, (expression, context) ->
        !(expression.getOperator() == Operators.FIELD
            && "drop".equals(expression.getParam(0))
            && context.getPath().equals(List.of(1, 0))))
        .orElseThrow();

    assertTrue(containsField(filtered, "keep"));
    assertFalse(containsField(filtered, "drop"));
  }

  @Test
  void filterShouldReturnNewExpressionAndPreserveEmptyType() {
    SExpression<?> expression = SExpression.create(Operators.EQ,
        SExpression.field("name"),
        SExpression.constant("Alice"));
    SExpression<?> filtered = SExpressions.filter(expression, (current, context) -> true)
        .orElseThrow();

    assertNotSame(expression, filtered);
    assertNotSame(expression.getParam(0), filtered.getParam(0));

    SExpression<?> emptyObject = SExpression.empty(Object.class);
    SExpression<?> filteredEmpty = SExpressions.filter(emptyObject, (current, context) -> true)
        .orElseThrow();

    assertNotSame(emptyObject, filteredEmpty);
    assertEquals(Object.class, filteredEmpty.getDataType());
  }

  @Test
  void rawRewriteShouldVisitMapsListsSExpressionsAndEmbeddedStatements() {
    Map<String, Object> raw = new LinkedHashMap<>();
    raw.put("where", SExpression.create(Operators.EQ, SExpression.field("old"), SExpression.constant("v")));
    raw.put("list", List.of(SExpression.field("old")));
    raw.put("statement", QueryStatement.builder()
        .from("users")
        .where(SExpression.create(Operators.EQ, SExpression.field("old"), SExpression.constant("nested")))
        .build());

    Object rewritten = RawExpressions.rewrite(raw, path -> {
      if (path.value() instanceof SExpression<?> expression
          && expression.getOperator() == Operators.FIELD
          && "old".equals(expression.getParam(0))) {
        return SExpression.field("renamed");
      }
      return path.value();
    });

    assertFalse(containsField((Map<?, ?>) rewritten, "old"));
    assertTrue(containsField((Map<?, ?>) rewritten, "renamed"));
  }

  private boolean containsField(Object value, String fieldName) {
    if (value instanceof SExpression<?> expression) {
      if (expression.getOperator() == Operators.FIELD && fieldName.equals(expression.getParam(0))) {
        return true;
      }
      return expression.getParams().stream().anyMatch(param -> containsField(param, fieldName));
    }
    if (value instanceof Map<?, ?> map) {
      return map.values().stream().anyMatch(item -> containsField(item, fieldName));
    }
    if (value instanceof List<?> list) {
      return list.stream().anyMatch(item -> containsField(item, fieldName));
    }
    return false;
  }
}
