package com.ouroboros.data.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class DataExpressionEvaluatorTest {
  @Test
  void wrapsPlainExpressionAndKeepsWrappedExpression() {
    var evaluator = new FailingDataExpressionEvaluator();

    assertEquals("${ name }", evaluator.wrapExpression("name"));
    assertEquals("${name}", evaluator.wrapExpression("${name}"));
    assertEquals(null, evaluator.wrapExpression(null));
  }

  @Test
  void failsExplicitlyWhenNoRuntimeAdapterIsAvailable() {
    var result = new FailingDataExpressionEvaluator().evaluate("${ name }", Map.of("name", "Ada"));

    assertTrue(result.isLeft());
    assertInstanceOf(DataExpressionException.class, result.getLeft());
  }

  @Test
  void wrapsContextWithReadOnlyOverlay() {
    var context = DataExpressionContext.wrap(Map.of("name", "Ada"), Map.of("$record", Map.of("name", "Ada")));

    assertEquals("Ada", context.get("name"));
    assertEquals(Map.of("name", "Ada"), context.get("$record"));
    assertThrows(UnsupportedOperationException.class, () -> context.put("name", "Bob"));
  }
}
