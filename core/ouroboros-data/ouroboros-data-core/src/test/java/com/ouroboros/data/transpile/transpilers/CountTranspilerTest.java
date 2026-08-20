package com.ouroboros.data.transpile.transpilers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.DummyTranspileContext;
import com.ouroboros.data.transpile.FieldSource;
import com.ouroboros.data.transpile.TranspileContext;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;

/**
 * CountTranspiler 单元测试
 */
class CountTranspilerTest {

  private CountTranspiler transpiler;
  private TranspileContext context;

  @BeforeEach
  void setUp() {
    transpiler = new CountTranspiler();

    // 创建一个 mock FieldSource，能够解析 "id" 字段
    FieldSource mockFieldSource = mock(FieldSource.class);
    Path<?> idPath = Expressions.numberPath(Long.class, "id");
    when(mockFieldSource.getField("id")).thenReturn(Optional.of(idPath));

    // 创建 DummyTranspileContext 并注册表
    context = new DummyTranspileContext().withTable("test", mockFieldSource);
  }

  @Nested
  @DisplayName("support() 测试")
  class SupportTests {

    @Test
    @DisplayName("支持 COUNT 操作符")
    void testSupports_COUNT() {
      // Given
      SExpression<?> expr = SExpression.create(Operators.COUNT,
          SExpression.create(ExtOps.FIELD, "id"));

      // When
      Boolean result = transpiler.support(expr);

      // Then
      assertTrue(result);
    }

    @Test
    @DisplayName("不支持其他操作符")
    void testDoesNotSupport_OtherOperator() {
      // Given
      SExpression<?> expr = SExpression.create(Operators.EQ, "a", "b");

      // When
      Boolean result = transpiler.support(expr);

      // Then
      assertFalse(result);
    }

    @Test
    @DisplayName("不支持 SUM 操作符")
    void testDoesNotSupport_SUM() {
      // Given
      SExpression<?> expr = SExpression.create(Operators.SUM,
          SExpression.create(ExtOps.FIELD, "amount"));

      // When
      Boolean result = transpiler.support(expr);

      // Then
      assertFalse(result);
    }
  }

  @Nested
  @DisplayName("apply() 测试")
  class ApplyTests {

    @Test
    @DisplayName("参数缺失时失败")
    void testApply_noParams_fails() {
      // Given - 无参数
      SExpression<?> expr = SExpression.create(Operators.COUNT);

      // When
      Try<Expression<?>> result = transpiler.apply(expr, context);

      // Then
      assertTrue(result.isFailure());
      assertTrue(result.getCause().getMessage().contains("缺少参数"));
    }

    @Test
    @DisplayName("参数过多时失败")
    void testApply_tooManyParams_fails() {
      // Given - 2 个参数
      SExpression<?> field1 = SExpression.create(ExtOps.FIELD, "id");
      SExpression<?> field2 = SExpression.create(ExtOps.FIELD, "name");
      SExpression<?> expr = SExpression.create(Operators.COUNT, field1, field2);

      // When
      Try<Expression<?>> result = transpiler.apply(expr, context);

      // Then
      assertTrue(result.isFailure());
      assertTrue(result.getCause().getMessage().contains("只接受 1 个参数"));
    }

    @Test
    @DisplayName("参数不是 SExpression 时失败")
    void testApply_invalidParamType_fails() {
      // Given - 参数是字符串
      SExpression<?> expr = SExpression.create(Operators.COUNT, "fieldName");

      // When
      Try<Expression<?>> result = transpiler.apply(expr, context);

      // Then
      assertTrue(result.isFailure());
      assertTrue(result.getCause().getMessage().contains("必须是 SExpression"));
    }

    @Test
    @DisplayName("COUNT(FIELD) 成功转译")
    void testApply_countField_succeeds() {
      // Given
      SExpression<?> fieldExpr = SExpression.create(ExtOps.FIELD, "id");
      SExpression<?> expr = SExpression.create(Operators.COUNT, fieldExpr);

      // When
      Try<Expression<?>> result = transpiler.apply(expr, context);

      // Then
      assertTrue(result.isSuccess());
      assertNotNull(result.get());
      // 验证返回 NumberExpression<Long>
      assertTrue(result.get() instanceof NumberExpression);
    }

    @Test
    @DisplayName("COUNT(*) 成功转译")
    void testApply_countStar_succeeds() {
      // Given - COLUMNS 代表 *
      SExpression<?> columnsExpr = SExpression.create(ExtOps.COLUMNS);
      SExpression<?> expr = SExpression.create(Operators.COUNT, columnsExpr);

      // When
      Try<Expression<?>> result = transpiler.apply(expr, context);

      // Then
      assertTrue(result.isSuccess());
      assertNotNull(result.get());
      // COUNT(*) 应该使用 Expressions.ONE.count()
    }

    @Test
    @DisplayName("COUNT(FIELD(\"*\")) 应兼容规范化后的 COUNT(*) 形态")
    void testApply_countFieldStar_succeeds() {
      SExpression<?> fieldExpr = SExpression.create(ExtOps.FIELD, "*");
      SExpression<?> expr = SExpression.create(Operators.COUNT, fieldExpr);

      Try<Expression<?>> result = transpiler.apply(expr, context);

      assertTrue(result.isSuccess(), () -> "COUNT(FIELD(\"*\")) 应被视为 COUNT(*): "
          + (result.isFailure() ? result.getCause().getMessage() : ""));
      assertNotNull(result.get());
      assertTrue(result.get() instanceof NumberExpression);
    }
  }
}
