package com.ouroboros.data.normalize;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.querydsl.core.types.Operator;

@DisplayName("ClauseNormalizeContext 异常传播语义测试")
public class ClauseNormalizeContextTest {

  @Test
  @DisplayName("normalizer 抛出非 NormalizeException 时链返回失败")
  void normalizeExpression_nonNormalizeException_failsFast() {
    RuntimeException cause = new RuntimeException("mock internal error");
    RawExpressionNormalizer failing = new RawExpressionNormalizer() {
      @Override
      public boolean supports(String clauseType, Class<?> rawExpressionType) { return true; }
      @Override
      public SExpression<?> normalize(Object rawExpression, ExpressionNormalizeContext context) { throw cause; }
    };

    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .addExpressionNormalizer(failing)
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.normalizeExpression("test", "test");
    assertTrue(result.isFailure());
    assertInstanceOf(NormalizeException.class, result.getCause());
    assertSame(cause, result.getCause().getCause());
  }

  @Test
  @DisplayName("SExpressionNormalizer 抛出非 NormalizeException 时链返回失败")
  void buildSExpression_nonNormalizeException_failsFast() {
    RuntimeException cause = new RuntimeException("mock normalizer error");
    SExpressionNormalizer failing = new SExpressionNormalizer() {
      @Override
      public boolean supports(Operator operator) { return true; }
      @Override
      public SExpression<?> normalize(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) { throw cause; }
    };

    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .addSExpressionNormalizer(failing)
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.buildSExpression(Operators.EQ, Collections.emptyList(), "test");
    assertTrue(result.isFailure());
    assertInstanceOf(NormalizeException.class, result.getCause());
    assertSame(cause, result.getCause().getCause());
  }

  @Test
  @DisplayName("NormalizeException 快速失败语义不变（不被二次包装）")
  void normalizeExpression_normalizeException_notDoubleWrapped() {
    NormalizeException original = new NormalizeException("expected");
    RawExpressionNormalizer failing = new RawExpressionNormalizer() {
      @Override
      public boolean supports(String clauseType, Class<?> rawExpressionType) { return true; }
      @Override
      public SExpression<?> normalize(Object rawExpression, ExpressionNormalizeContext context) { throw original; }
    };

    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .addExpressionNormalizer(failing)
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.normalizeExpression("test", "test");
    assertTrue(result.isFailure());
    assertSame(original, result.getCause());
  }

  @Test
  @DisplayName("默认上下文中的 AND 应通过 logic builder 做短路优化")
  void buildSExpression_and_usesLogicBuilderShortCircuit() {
    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.buildSExpression(
        Operators.AND,
        Arrays.asList(SExpression.constant(Boolean.TRUE), SExpression.constant(Boolean.FALSE)),
        "test");

    assertTrue(result.isSuccess());
    assertEquals(Operators.CONSTANT, result.get().getOperator(), "AND(TRUE, FALSE) 应短路折叠为常量");
    assertEquals(Boolean.FALSE, result.get().getParam(0));
  }

  @Test
  @DisplayName("默认上下文中的 AND 应拒绝非布尔参数")
  void buildSExpression_and_rejectsNonBooleanParams() {
    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.buildSExpression(
        Operators.AND,
        Arrays.asList(SExpression.constant(Boolean.TRUE), SExpression.constant("oops")),
        "test");

    assertTrue(result.isFailure(), "AND 的非布尔参数应被拒绝");
    assertInstanceOf(NormalizeException.class, result.getCause());
  }

  @Test
  @DisplayName("默认上下文中的 BETWEEN 应校验 typed 参数个数")
  void buildSExpression_between_validatesTypedArity() {
    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.buildSExpression(
        Operators.BETWEEN,
        Arrays.asList(SExpression.create(Operators.FIELD, "age"), SExpression.constant(18)),
        "test");

    assertTrue(result.isFailure(), "BETWEEN 缺少上界时应失败");
    assertInstanceOf(NormalizeException.class, result.getCause());
  }

  @Test
  @DisplayName("默认上下文中的 IN 应校验右值为列表常量")
  void buildSExpression_in_requiresListConstant() {
    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.buildSExpression(
        Operators.IN,
        Arrays.asList(SExpression.create(Operators.FIELD, "age"), SExpression.constant(18)),
        "test");

    assertTrue(result.isFailure(), "IN 的右值不是列表常量时应失败");
    assertInstanceOf(NormalizeException.class, result.getCause());
  }

  @Test
  @DisplayName("默认上下文中的 IS_NULL 应校验 typed 参数个数")
  void buildSExpression_isNull_validatesTypedArity() {
    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.buildSExpression(
        Operators.IS_NULL,
        Arrays.asList(SExpression.create(Operators.FIELD, "name"), SExpression.constant(Boolean.TRUE)),
        "test");

    assertTrue(result.isFailure(), "IS_NULL 多参数时应失败");
    assertInstanceOf(NormalizeException.class, result.getCause());
  }

  @Test
  @DisplayName("默认上下文中的 FIELD 应校验参数为字符串常量")
  void buildSExpression_field_requiresStringConstants() {
    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.buildSExpression(
        Operators.FIELD,
        Arrays.asList(SExpression.constant(123)),
        "test");

    assertTrue(result.isFailure(), "FIELD 参数不是字符串常量时应失败");
    assertInstanceOf(NormalizeException.class, result.getCause());
  }

  @Test
  @DisplayName("默认上下文中的 LIKE 应校验 typed 参数个数")
  void buildSExpression_like_validatesTypedArity() {
    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.buildSExpression(
        Operators.LIKE,
        Arrays.asList(SExpression.create(Operators.FIELD, "name")),
        "test");

    assertTrue(result.isFailure(), "LIKE 缺少右值时应失败");
    assertInstanceOf(NormalizeException.class, result.getCause());
  }

  @Test
  @DisplayName("默认上下文中的 CONSTANT 应校验参数个数")
  void buildSExpression_constant_validatesArity() {
    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "WHERE");

    Try<SExpression<?>> result = ctx.buildSExpression(
        Operators.CONSTANT,
        Arrays.asList(SExpression.constant("a"), SExpression.constant("b")),
        "test");

    assertTrue(result.isFailure(), "CONSTANT 多参数时应失败");
    assertInstanceOf(NormalizeException.class, result.getCause());
  }

  @Test
  @DisplayName("默认上下文中的 COUNT 应校验 typed 参数个数")
  void buildSExpression_count_validatesTypedArity() {
    QueryNormalizeContext qCtx = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    ClauseNormalizeContext ctx = new ClauseNormalizeContext(qCtx, "SELECT");

    Try<SExpression<?>> result = ctx.buildSExpression(
        Operators.COUNT,
        Arrays.asList(
            SExpression.create(Operators.FIELD, "id"),
            SExpression.create(Operators.FIELD, "name")),
        "test");

    assertTrue(result.isFailure(), "COUNT 多参数时应失败");
    assertInstanceOf(NormalizeException.class, result.getCause());
  }
}
