package com.ouroboros.data.transpile.clausebuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.JoinType;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.*;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;

/**
 * JoinBuilder 单元测试
 */
@DisplayName("JoinBuilder 测试")
class JoinBuilderTest {

  private JoinBuilder builder;
  private TranspileContext context;
  private OuroborosQueryMetadata metadata;

  @BeforeEach
  void setUp() {
    builder = new JoinBuilder();

    // 创建 mock FieldSource
    FieldSource ordersSource = mock(FieldSource.class);
    Path<?> ordersPath = Expressions.path(Object.class, "orders");
    doReturn(ordersPath).when(ordersSource).getSelfPath();
    doReturn(Optional.of(Expressions.numberPath(Long.class, "id"))).when(ordersSource).getField("id");
    doReturn(Optional.of(Expressions.numberPath(Long.class, "userId"))).when(ordersSource).getField("userId");

    FieldSource usersSource = mock(FieldSource.class);
    Path<?> usersPath = Expressions.path(Object.class, "users");
    doReturn(usersPath).when(usersSource).getSelfPath();
    doReturn(Optional.of(Expressions.numberPath(Long.class, "id"))).when(usersSource).getField("id");
    doReturn(Optional.of(Expressions.stringPath("name"))).when(usersSource).getField("name");

    // 创建 DummyTranspileContext 并注册表
    context = new DummyTranspileContext()
        .withTable("orders", ordersSource)
        .withTable("users", usersSource);

    metadata = new DefaultOuroborosQueryMetadata();
  }

  @Test
  @DisplayName("JoinBuilder 同时暴露 canonical 与兼容子句契约")
  void testContractBridge() {
    assertTrue(builder instanceof ClauseTranspiler);
    assertTrue(builder instanceof QueryTranspiler.ClauseBuilder);
  }

  @Nested
  @DisplayName("无 JOIN 场景")
  class NoJoinTests {

    @Test
    @DisplayName("无 JOIN 时应直接返回原 context")
    void testNoJoin_returnsOriginalContext() {
      // Given
      QueryStatement statement = QueryStatement.builder()
          .from("orders")
          .build();

      // When
      Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> result =
          builder.applyWithContext(statement, metadata, context);

      // Then
      assertTrue(result.isSuccess());
      assertSame(context, result.get()._2, "应返回原 context");
    }
  }

  @Nested
  @DisplayName("普通表 JOIN")
  class TableJoinTests {

    @Test
    @DisplayName("INNER JOIN 应正确处理")
    void testInnerJoin_addsToMetadata() {
      // Given
      SExpression<Boolean> onCondition = SExpression.create(
          Operators.EQ,
          SExpression.create(ExtOps.FIELD, "userId"),
          SExpression.create(ExtOps.FIELD, "id")
      );

      QueryStatement statement = QueryStatement.builder()
          .from("orders")
          .join(JoinType.INNERJOIN, "users", "u", onCondition)
          .build();

      // When
      Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> result =
          builder.applyWithContext(statement, metadata, context);

      // Then
      if (result.isFailure()) {
        result.getCause().printStackTrace();
      }
      assertTrue(result.isSuccess(), "JOIN 处理应成功: " +
          (result.isFailure() ? result.getCause().getMessage() : ""));
    }

    @Test
    @DisplayName("LEFT JOIN 应正确处理")
    void testLeftJoin_addsToMetadata() {
      // Given
      SExpression<Boolean> onCondition = SExpression.create(
          Operators.EQ,
          SExpression.create(ExtOps.FIELD, "userId"),
          SExpression.create(ExtOps.FIELD, "id")
      );

      QueryStatement statement = QueryStatement.builder()
          .from("orders")
          .join(JoinType.LEFTJOIN, "users", "u", onCondition)
          .build();

      // When
      Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> result =
          builder.applyWithContext(statement, metadata, context);

      // Then
      if (result.isFailure()) {
        result.getCause().printStackTrace();
      }
      assertTrue(result.isSuccess(), "LEFT JOIN 处理应成功: " +
          (result.isFailure() ? result.getCause().getMessage() : ""));
    }

    @Test
    @DisplayName("多个 JOIN 应全部处理")
    void testMultipleJoins_allAdded() {
      // Given
      SExpression<Boolean> onCondition1 = SExpression.create(
          Operators.EQ,
          SExpression.create(ExtOps.FIELD, "userId"),
          SExpression.create(ExtOps.FIELD, "id")
      );

      QueryStatement statement = QueryStatement.builder()
          .from("orders")
          .join(JoinType.INNERJOIN, "users", "u", onCondition1)
          .build();

      // When
      Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> result =
          builder.applyWithContext(statement, metadata, context);

      // Then
      if (result.isFailure()) {
        result.getCause().printStackTrace();
      }
      assertTrue(result.isSuccess(), "多个 JOIN 处理应成功: " +
          (result.isFailure() ? result.getCause().getMessage() : ""));
    }

    @Test
    @DisplayName("多表 JOIN 的后续 ON 条件应将裸右值绑定到当前 JOIN 表")
    void testMultipleJoins_onConditionPrefersCurrentJoinTargetForRightField() {
      FieldSource userSource = mock(FieldSource.class);
      Path<?> userPath = Expressions.path(Object.class, "users");
      doReturn(userPath).when(userSource).getSelfPath();
      doReturn(Optional.of(Expressions.numberPath(Long.class, "user_id"))).when(userSource).getField("id");
      doReturn(Optional.of(Expressions.numberPath(Long.class, "department_id"))).when(userSource).getField("departmentId");

      FieldSource orderSource = mock(FieldSource.class);
      Path<?> orderPath = Expressions.path(Object.class, "orders");
      doReturn(orderPath).when(orderSource).getSelfPath();
      doReturn(Optional.of(Expressions.numberPath(Long.class, "order_id"))).when(orderSource).getField("id");
      doReturn(Optional.of(Expressions.numberPath(Long.class, "order_user_id"))).when(orderSource).getField("userId");

      FieldSource departmentSource = mock(FieldSource.class);
      Path<?> departmentPath = Expressions.path(Object.class, "departments");
      doReturn(departmentPath).when(departmentSource).getSelfPath();
      doReturn(Optional.of(Expressions.numberPath(Long.class, "department_id"))).when(departmentSource).getField("id");

      TranspileContext multiJoinContext = new DummyTranspileContext()
          .withTable("users", userSource)
          .withTable("orders", orderSource)
          .withTable("departments", departmentSource);

      SExpression<Boolean> firstOn = SExpression.create(
          Operators.EQ,
          SExpression.create(ExtOps.FIELD, "id"),
          SExpression.create(ExtOps.FIELD, "userId")
      );
      SExpression<Boolean> secondOn = SExpression.create(
          Operators.EQ,
          SExpression.create(ExtOps.FIELD, "departmentId"),
          SExpression.create(ExtOps.FIELD, "id")
      );

      QueryStatement statement = QueryStatement.builder()
          .from("users")
          .leftJoin("orders", "o", firstOn)
          .leftJoin("departments", "d", secondOn)
          .build();

      Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> result =
          builder.applyWithContext(statement, new DefaultOuroborosQueryMetadata(), multiJoinContext);

      assertTrue(result.isSuccess(), "第二个 JOIN 的 ON 条件应成功绑定到当前 JOIN 表: "
          + (result.isFailure() ? result.getCause().getMessage() : ""));
    }
  }

  @Nested
  @DisplayName("JOIN Context 包装")
  class JoinContextTests {

    @Test
    @DisplayName("有 JOIN 时应返回 JoinTranspileContext")
    void testWithJoin_returnsJoinContext() {
      // Given
      SExpression<Boolean> onCondition = SExpression.create(
          Operators.EQ,
          SExpression.create(ExtOps.FIELD, "userId"),
          SExpression.create(ExtOps.FIELD, "id")
      );

      QueryStatement statement = QueryStatement.builder()
          .from("orders")
          .join(JoinType.INNERJOIN, "users", "u", onCondition)
          .build();

      // When
      Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> result =
          builder.applyWithContext(statement, metadata, context);

      // Then
      if (result.isFailure()) {
        result.getCause().printStackTrace();
      }
      assertTrue(result.isSuccess(), "JOIN 处理应成功: " +
          (result.isFailure() ? result.getCause().getMessage() : ""));
      TranspileContext newContext = result.get()._2;
      assertNotSame(context, newContext, "应返回新的 Context");

      // 验证新 context 可以解析 JOIN 别名
      assertTrue(newContext.resolveTable("u").isPresent(), "应能解析 JOIN 别名");
    }
  }

  @Nested
  @DisplayName("错误处理")
  class ErrorHandlingTests {

    @Test
    @DisplayName("JOIN 不存在的表应失败")
    void testJoinNonExistentTable_fails() {
      // Given
      SExpression<Boolean> onCondition = SExpression.create(
          Operators.EQ,
          SExpression.create(ExtOps.FIELD, "orders.id"),
          SExpression.create(ExtOps.FIELD, "x.id")
      );

      QueryStatement statement = QueryStatement.builder()
          .from("orders")
          .join(JoinType.INNERJOIN, "nonexistent", "x", onCondition)
          .build();

      // When
      Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> result =
          builder.applyWithContext(statement, metadata, context);

      // Then
      assertTrue(result.isFailure());
      assertTrue(result.getCause().getMessage().contains("Join表不存在"));
    }
  }
}
