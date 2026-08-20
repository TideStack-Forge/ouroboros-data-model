package com.ouroboros.data.dsl.transpile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.*;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.*;
import com.querydsl.core.types.dsl.Expressions;

@DisplayName("SubqueryFieldSource 集成测试")
class SubqueryFieldSourceIntegrationTest {

  private DummyTranspileContext context;
  private QueryTranspiler transpiler;

  @BeforeEach
  void setUp() {
    FieldSource usersSource = mock(FieldSource.class);
    doReturn(Expressions.path(Object.class, "users")).when(usersSource).getSelfPath();
    doReturn("users").when(usersSource).getName();
    doReturn(Optional.of(Expressions.numberPath(Long.class, "id"))).when(usersSource).getField("id");
    doReturn(Optional.of(Expressions.stringPath("name"))).when(usersSource).getField("name");

    FieldSource ordersSource = mock(FieldSource.class);
    doReturn(Expressions.path(Object.class, "orders")).when(ordersSource).getSelfPath();
    doReturn("orders").when(ordersSource).getName();
    doReturn(Optional.of(Expressions.numberPath(Long.class, "id"))).when(ordersSource).getField("id");
    doReturn(Optional.of(Expressions.numberPath(Long.class, "userId"))).when(ordersSource).getField("userId");

    context = new DummyTranspileContext()
        .withTable("users", usersSource)
        .withTable("orders", ordersSource);

    transpiler = QueryTranspiler.DEFAULT;
  }

  @Test
  @DisplayName("T1: FROM (subquery) alias — 基本子查询作为数据源")
  void testFromSubqueryAlias() {
    // 内层: SELECT a.id, a.name FROM users a
    QueryStatement subQuery = QueryStatement.builder()
        .from("users", "a")
        .select(
            SExpression.create(ExtOps.FIELD, "a", "id"),
            SExpression.create(ExtOps.FIELD, "a", "name"))
        .build();

    // 外层: FROM (subQuery) sub SELECT sub.id, sub.name
    QueryStatement outer = QueryStatement.builder()
        .from(subQuery, "sub")
        .select(
            SExpression.create(ExtOps.FIELD, "sub", "id"),
            SExpression.create(ExtOps.FIELD, "sub", "name"))
        .build();

    var result = transpiler.applyWithContext(outer, context);
    assertTrue(result.isSuccess(),
        "FROM subquery 应成功: " + (result.isFailure() ? result.getCause().getMessage() : ""));
  }

  @Test
  @DisplayName("T2: JOIN (subquery) alias ON condition")
  void testJoinSubqueryAlias() {
    // 内层: SELECT a.id, a.name FROM users a
    QueryStatement subQuery = QueryStatement.builder()
        .from("users", "a")
        .select(
            SExpression.create(ExtOps.FIELD, "a", "id"),
            SExpression.create(ExtOps.FIELD, "a", "name"))
        .build();

    // 外层: FROM orders o JOIN (subQuery) sub ON o.userId = sub.id SELECT o.id, sub.name
    SExpression<Boolean> onCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(ExtOps.FIELD, "o", "userId"),
        SExpression.create(ExtOps.FIELD, "sub", "id"));

    QueryStatement outer = QueryStatement.builder()
        .from("orders", "o")
        .innerJoin(subQuery, "sub", onCondition)
        .select(
            SExpression.create(ExtOps.FIELD, "o", "id"),
            SExpression.create(ExtOps.FIELD, "sub", "name"))
        .build();

    var result = transpiler.applyWithContext(outer, context);
    assertTrue(result.isSuccess(),
        "JOIN subquery 应成功: " + (result.isFailure() ? result.getCause().getMessage() : ""));
  }

  @Test
  @DisplayName("T3: 子查询投影列使用 AS 别名")
  void testSubqueryProjectionAlias() {
    // 内层: SELECT a.id AS uid FROM users a
    QueryStatement subQuery = QueryStatement.builder()
        .from("users", "a")
        .select(
            SExpression.create(Operators.ALIAS,
                SExpression.create(ExtOps.FIELD, "a", "id"),
                "uid"))
        .build();

    // 外层: FROM (subQuery) sub SELECT sub.uid
    QueryStatement outer = QueryStatement.builder()
        .from(subQuery, "sub")
        .select(SExpression.create(ExtOps.FIELD, "sub", "uid"))
        .build();

    var result = transpiler.applyWithContext(outer, context);
    assertTrue(result.isSuccess(),
        "AS 别名应可解析: " + (result.isFailure() ? result.getCause().getMessage() : ""));
  }

  @Test
  @DisplayName("T4: 嵌套子查询")
  void testNestedSubquery() {
    // 最内层: SELECT a.id AS uid FROM users a
    QueryStatement innerSub = QueryStatement.builder()
        .from("users", "a")
        .select(
            SExpression.create(Operators.ALIAS,
                SExpression.create(ExtOps.FIELD, "a", "id"),
                "uid"))
        .build();

    // 中间层: FROM (innerSub) inner_sub SELECT inner_sub.uid
    QueryStatement middleSub = QueryStatement.builder()
        .from(innerSub, "inner_sub")
        .select(SExpression.create(ExtOps.FIELD, "inner_sub", "uid"))
        .build();

    // 最外层: FROM (middleSub) outer_sub SELECT outer_sub.uid
    QueryStatement outer = QueryStatement.builder()
        .from(middleSub, "outer_sub")
        .select(SExpression.create(ExtOps.FIELD, "outer_sub", "uid"))
        .build();

    var result = transpiler.applyWithContext(outer, context);
    assertTrue(result.isSuccess(),
        "嵌套子查询应成功: " + (result.isFailure() ? result.getCause().getMessage() : ""));
  }
}
