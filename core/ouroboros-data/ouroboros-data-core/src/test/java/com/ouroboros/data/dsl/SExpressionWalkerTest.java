package com.ouroboros.data.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Operator;

@DisplayName("SExpression Walker/Transform 测试")
class SExpressionWalkerTest {

  @Test
  @DisplayName("canonical 工厂方法应构造 FIELD/ALIAS/COLUMNS 表达式")
  void canonicalFactoriesShouldBuildStructuredExpressions() {
    var field = SExpression.field("user", "name");
    var alias = SExpression.alias(field, "userName");
    var columns = SExpression.columns(field);

    assertEquals(Operators.FIELD, field.getOperator());
    assertEquals(2, field.getParams().size());
    assertEquals("user", field.getParam(0));
    assertEquals("name", field.getParam(1));

    assertEquals(Operators.ALIAS, alias.getOperator());
    assertSame(field, alias.getParamAsSExpression(0));
    assertEquals("userName", alias.getParam(1));

    assertEquals(Operators.COLUMNS, columns.getOperator());
    assertSame(field, columns.getParamAsSExpression(0));
  }

  @Test
  @DisplayName("mutable API should update operator and params")
  void mutableApiShouldUpdateOperatorAndParams() {
    var expression = SExpression.create(Operators.FIELD, "name");

    expression.setOperator((Operator) Ops.EQ);
    expression.setParams(new ArrayList<Object>(java.util.Arrays.asList("left", "right")));
    expression.setParam(1, "updated");
    expression.addParam("tail");
    expression.updateParam(0, "updated-left");
    expression.removeParam(2);

    assertEquals(Ops.EQ, expression.getOperator());
    assertEquals(2, expression.getParams().size());
    assertEquals("updated-left", expression.getParam(0));
    assertEquals("updated", expression.getParam(1));
    assertEquals(2, expression.getParamsAsSExpression().size());
  }

  @Test
  @DisplayName("field with list and empty factory paths")
  void fieldWithListAndEmptyFactoryPaths() {
    var fieldFromList = SExpression.field(java.util.Arrays.asList("user"));
    assertEquals(Operators.FIELD, fieldFromList.getOperator());
    assertEquals("user", fieldFromList.getParam(0));

    var emptyBoolean = SExpression.empty(Boolean.class);
    var emptyObject = SExpression.empty();
    assertTrue(emptyBoolean.isEmpty());
    assertTrue(emptyObject.isEmpty());
    assertEquals(Boolean.class, emptyBoolean.getDataType());
  }

  @Nested
  @DisplayName("walk() 方法测试")
  class WalkTests {

    @Test
    @DisplayName("单节点表达式：访问 1 次，depth=0")
    void walkSingleNode() {
      // Given
      var expr = SExpression.create(Operators.FIELD, "name");
      var depths = new ArrayList<>();

      // When
      expr.walk((e, context) -> depths.add(context.getDepth()));

      // Then
      assertEquals(1, depths.size());
      assertEquals(0, depths.get(0));
    }

    @Test
    @DisplayName("嵌套表达式：先序遍历顺序正确")
    void walkNestedExpression() {
      // Given: (AND (EQ field1 "a") (EQ field2 "b"))
      var eq1 = SExpression.create(Ops.EQ,
          SExpression.create(Operators.FIELD, "field1"),
          SExpression.constant("a"));
      var eq2 = SExpression.create(Ops.EQ,
          SExpression.create(Operators.FIELD, "field2"),
          SExpression.constant("b"));
      var and = SExpression.create(Ops.AND, eq1, eq2);

      var visitOrder = new ArrayList<>();

      // When
      and.walk((e, context) -> {
        if (e.getOperator() == Ops.AND) {
          visitOrder.add("AND");
        } else if (e.getOperator() == Ops.EQ) {
          visitOrder.add("EQ");
        } else if (e.getOperator() == Operators.FIELD) {
          visitOrder.add("FIELD:" + e.getParam(0));
        } else if (e.getOperator() == Operators.CONSTANT) {
          visitOrder.add("CONST:" + e.getParam(0));
        }
      });

      // Then: 先序遍历顺序
      // 结构: AND -> EQ -> FIELD, CONST -> EQ -> FIELD, CONST (共 7 个节点)
      assertEquals(7, visitOrder.size());
      assertEquals("AND", visitOrder.get(0));
      assertEquals("EQ", visitOrder.get(1));
      assertEquals("FIELD:field1", visitOrder.get(2));
      assertEquals("CONST:a", visitOrder.get(3));
      assertEquals("EQ", visitOrder.get(4));
      assertEquals("FIELD:field2", visitOrder.get(5));
      assertEquals("CONST:b", visitOrder.get(6));
    }

    @Test
    @DisplayName("深度验证：depth 值正确递增")
    void walkDepthCorrect() {
      // Given: (AND (EQ (FIELD name) "a"))
      var field = SExpression.create(Operators.FIELD, "name");
      var constant = SExpression.constant("a");
      var eq = SExpression.create(Ops.EQ, field, constant);
      var and = SExpression.create(Ops.AND, eq);

      var depths = new ArrayList<>();

      // When
      and.walk((e, context) -> depths.add(context.getDepth()));

      // Then
      assertEquals(4, depths.size());
      assertEquals(0, depths.get(0)); // AND
      assertEquals(1, depths.get(1)); // EQ
      assertEquals(2, depths.get(2)); // FIELD
      assertEquals(2, depths.get(3)); // CONSTANT
    }

    @Test
    @DisplayName("混合参数：只访问 SExpression 类型参数")
    void walkMixedParams() {
      // Given: (FIELD "name") - 参数是字符串，不是 SExpression
      var field = SExpression.create(Operators.FIELD, "name");
      var visitCount = new ArrayList<>();

      // When
      field.walk((e, context) -> visitCount.add(1));

      // Then: 只访问 FIELD 节点本身，不访问字符串参数
      assertEquals(1, visitCount.size());
    }

    @Test
    @DisplayName("收集所有字段名")
    void walkCollectFields() {
      // Given: (AND (EQ (FIELD a) 1) (EQ (FIELD b) 2))
      var expr = SExpression.create(Ops.AND,
          SExpression.create(Ops.EQ,
              SExpression.create(Operators.FIELD, "a"),
              SExpression.constant(1)),
          SExpression.create(Ops.EQ,
              SExpression.create(Operators.FIELD, "b"),
              SExpression.constant(2)));

      var fields = new ArrayList<>();

      // When
      expr.walk((e, context) -> {
        if (e.getOperator() == Operators.FIELD) {
          fields.add((String) e.getParam(0));
        }
      });

      // Then
      assertEquals(2, fields.size());
      assertTrue(fields.contains("a"));
      assertTrue(fields.contains("b"));
    }

    @Test
    @DisplayName("遍历上下文包含路径、父节点和祖先")
    void walkContextShouldExposePathParentAndAncestors() {
      var left = SExpression.create(Ops.EQ,
          SExpression.field("a"),
          SExpression.constant(1));
      var right = SExpression.create(Ops.EQ,
          SExpression.field("b"),
          SExpression.constant(2));
      var expr = SExpression.create(Ops.AND, left, right);

      var matchedContexts = new ArrayList<SExpressionTraversalContext>();

      expr.walk((node, context) -> {
        if (node.getOperator() == Operators.FIELD && "b".equals(node.getParam(0))) {
          matchedContexts.add(context);
        }
      });

      assertEquals(1, matchedContexts.size());
      SExpressionTraversalContext context = matchedContexts.get(0);
      assertFalse(context.isRoot());
      assertEquals(2, context.getDepth());
      assertEquals(List.of(1, 0), context.getPath());
      assertEquals(0, context.getParamIndex().orElseThrow());
      assertEquals(Ops.EQ, context.getParent().orElseThrow().getOperator());
      assertEquals(List.of(Ops.AND, Ops.EQ), context.getAncestors().stream()
          .map(SExpression::getOperator)
          .toList());
    }
  }

  @Nested
  @DisplayName("transform() 方法测试")
  class TransformTests {

    @Test
    @DisplayName("单节点表达式：返回新表达式")
    void transformSingleNode() {
      // Given
      var original = SExpression.create(Operators.FIELD, "name");

      // When: 恒等转换
      var transformed = original.transform((e, context) -> e);

      // Then
      assertNotSame(original, transformed);
      assertEquals(original.getOperator(), transformed.getOperator());
      assertEquals(original.getParams(), transformed.getParams());
    }

    @Test
    @DisplayName("嵌套表达式：子节点先被转换")
    void transformNestedExpression() {
      // Given: (AND (FIELD a))
      var field = SExpression.create(Operators.FIELD, "a");
      var and = SExpression.create(Ops.AND, field);

      var transformOrder = new ArrayList<>();

      // When
      and.transform((e, context) -> {
        if (e.getOperator() == Operators.FIELD) {
          transformOrder.add("FIELD");
        } else if (e.getOperator() == Ops.AND) {
          transformOrder.add("AND");
        }
        return e;
      });

      // Then: 后序遍历，子节点先被转换
      assertEquals(2, transformOrder.size());
      assertEquals("FIELD", transformOrder.get(0));
      assertEquals("AND", transformOrder.get(1));
    }

    @Test
    @DisplayName("不修改原树")
    void transformDoesNotModifyOriginal() {
      // Given
      var field = SExpression.create(Operators.FIELD, "original");
      var original = SExpression.create(Ops.AND, field);

      // When: 修改字段名
      var transformed = original.transform((e, context) -> {
        if (e.getOperator() == Operators.FIELD) {
          return SExpression.create(Operators.FIELD, "modified");
        }
        return e;
      });

      // Then: 原表达式不变
      var originalField = (SExpression<?>) original.getParam(0);
      assertEquals("original", originalField.getParam(0));

      // 新表达式已修改
      var transformedField = (SExpression<?>) transformed.getParam(0);
      assertEquals("modified", transformedField.getParam(0));
    }

    @Test
    @DisplayName("恒等转换：返回结构相同的新树")
    void transformIdentity() {
      // Given: (EQ (FIELD a) (CONSTANT 1))
      var original = SExpression.create(Ops.EQ,
          SExpression.create(Operators.FIELD, "a"),
          SExpression.constant(1));

      // When
      var transformed = original.transform((e, context) -> e);

      // Then
      assertEquals(original, transformed);
      assertNotSame(original, transformed);
    }

    @Test
    @DisplayName("实际转换：常量折叠示例")
    void transformConstantFolding() {
      // Given: (AND (CONSTANT true) (FIELD a))
      var original = SExpression.create(Ops.AND,
          SExpression.constant(true),
          SExpression.create(Operators.FIELD, "a"));

      // When: 简化 (AND true x) -> x
      var transformed = original.transform((e, context) -> {
        if (e.getOperator() == Ops.AND && e.getParams().size() == 2) {
          var first = e.getParam(0);
          var second = e.getParam(1);

          // (AND true x) -> x
          if (first instanceof SExpression) {
            var firstExpr = (SExpression<?>) first;
            if (firstExpr.getOperator() == Operators.CONSTANT
                && Boolean.TRUE.equals(firstExpr.getParam(0))) {
              return (SExpression<?>) second;
            }
          }
        }
        return e;
      });

      // Then
      assertEquals(Operators.FIELD, transformed.getOperator());
      assertEquals("a", transformed.getParam(0));
    }

    @Test
    @DisplayName("深层嵌套转换")
    void transformDeeplyNested() {
      // Given: (AND (OR (FIELD a) (FIELD b)) (FIELD c))
      var original = SExpression.create(Ops.AND,
          SExpression.create(Ops.OR,
              SExpression.create(Operators.FIELD, "a"),
              SExpression.create(Operators.FIELD, "b")),
          SExpression.create(Operators.FIELD, "c"));

      // When: 将所有字段名转为大写
      var transformed = original.transform((e, context) -> {
        if (e.getOperator() == Operators.FIELD) {
          var fieldName = (String) e.getParam(0);
          return SExpression.create(Operators.FIELD, fieldName.toUpperCase());
        }
        return e;
      });

      // Then: 验证所有字段名都已转为大写
      var fields = new ArrayList<>();
      transformed.walk((e, context) -> {
        if (e.getOperator() == Operators.FIELD) {
          fields.add((String) e.getParam(0));
        }
      });

      assertEquals(3, fields.size());
      assertTrue(fields.contains("A"));
      assertTrue(fields.contains("B"));
      assertTrue(fields.contains("C"));
    }

    @Test
    @DisplayName("转换上下文包含当前节点路径")
    void transformContextShouldExposePath() {
      var original = SExpression.create(Ops.AND,
          SExpression.create(Ops.OR,
              SExpression.field("a"),
              SExpression.field("b")),
          SExpression.field("c"));
      var paths = new ArrayList<List<Integer>>();

      original.transform((expression, context) -> {
        if (expression.getOperator() == Operators.FIELD && "b".equals(expression.getParam(0))) {
          paths.add(context.getPath());
        }
        return expression;
      });

      assertEquals(List.of(List.of(0, 1)), paths);
    }
  }
}
