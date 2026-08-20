package com.ouroboros.data.orchestration.rewriter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;

/**
 * ExistsStatementRewriter 测试
 *
 * <p>Round 3 更新：验证 EXISTS 子查询和条件替换的正确性
 */
class ExistsStatementRewriterTest {

  private OrchestrationContext context;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
  }

  @Test
  void testBuildExistsSubQuery() {
    // Given: 含 REL_ANY 条件的语句
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "orders.status"),
            SExpression.constant("completed")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(relCondition)
        .build();

    ExistsStatementRewriter rewriter = new ExistsStatementRewriter("orders", null, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: WHERE 应包含 EXISTS 条件
    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertFalse(where.isEmpty());
    assertEquals(Operators.EXISTS, where.getOperator());
  }

  @Test
  void testReplaceWithExists() {
    // Given: AND 组合条件（普通条件 AND 关联条件）
    SExpression<Boolean> normalCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "status"),
        SExpression.constant("active")
    );
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "orders.amount"),
            SExpression.constant(100)
        )
    );
    SExpression<Boolean> combinedWhere = SExpression.create(
        Operators.AND,
        normalCondition,
        relCondition
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(combinedWhere)
        .build();

    ExistsStatementRewriter rewriter = new ExistsStatementRewriter("orders", null, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: WHERE 应为 AND(普通条件, EXISTS(...))
    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertFalse(where.isEmpty());
    assertEquals(Operators.AND, where.getOperator());
  }

  @Test
  void testNoRelationCondition() {
    // Given: 无关联条件的语句
    SExpression<Boolean> normalCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "status"),
        SExpression.constant("active")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(normalCondition)
        .build();

    ExistsStatementRewriter rewriter = new ExistsStatementRewriter("orders", null, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: WHERE 不变（无关联条件可替换）
    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertFalse(where.isEmpty());
    assertEquals(Operators.EQ, where.getOperator());
  }

  @Test
  void testExistsSubQueryStructure() {
    // Given: 含 REL_ANY 条件
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "orders.status"),
            SExpression.constant("paid")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(relCondition)
        .build();

    ExistsStatementRewriter rewriter = new ExistsStatementRewriter("orders", null, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: 改写后应包含 EXISTS
    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertFalse(where.isEmpty());
    // EXISTS 操作符的参数是子查询
    assertEquals(Operators.EXISTS, where.getOperator());
  }

  @Test
  void testExistsSubQueryJoinConditionUsesSegmentedFieldParams() {
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orders"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "orders.status"),
            SExpression.constant("paid")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(relCondition)
        .build();

    ExistsStatementRewriter rewriter = new ExistsStatementRewriter("orders", null, "id", "orderId");

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    SExpression<Boolean> existsExpr = rewritten.getWhere();
    assertEquals(Operators.EXISTS, existsExpr.getOperator());

    QueryStatement subQuery = (QueryStatement) existsExpr.getParam(0);
    SExpression<?> subQueryWhere = subQuery.getWhere();
    assertEquals(Operators.AND, subQueryWhere.getOperator());

    SExpression<?> joinCondition = subQueryWhere.getParamAsSExpression(0);
    assertEquals(Operators.EQ, joinCondition.getOperator());

    SExpression<?> leftField = joinCondition.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, leftField.getOperator());
    assertEquals(2, leftField.getParams().size());
    assertEquals("orders", leftField.getParam(0));
    assertEquals("orderId", leftField.getParam(1));

    SExpression<?> rightField = joinCondition.getParamAsSExpression(1);
    assertEquals(Operators.FIELD, rightField.getOperator());
    assertEquals(2, rightField.getParams().size());
    assertEquals("user", rightField.getParam(0));
    assertEquals("id", rightField.getParam(1));
  }

  @Test
  void testNestedRelationUsesLeafTargetTableAndParentAliasCorrelation() {
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department", "employees"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("Alice")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(relCondition)
        .build();

    ExistsStatementRewriter rewriter = new ExistsStatementRewriter("department.employees", "department", "employeesId", "id");

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    QueryStatement subQuery = (QueryStatement) rewritten.getWhere().getParam(0);
    assertEquals("employees", subQuery.getFrom().getTableName(),
        "嵌套 relation 的子查询 FROM 应使用叶子 relation 名称");

    SExpression<?> subQueryWhere = subQuery.getWhere();
    assertEquals(Operators.AND, subQueryWhere.getOperator());

    SExpression<?> joinCondition = subQueryWhere.getParamAsSExpression(0);
    SExpression<?> leftField = joinCondition.getParamAsSExpression(0);
    SExpression<?> rightField = joinCondition.getParamAsSExpression(1);
    assertEquals("employees", leftField.getParam(0),
        "子查询侧应指向当前 relation target，而不是完整结构路径");
    assertEquals("department", rightField.getParam(0),
        "关联侧应指向父级 JOIN alias");
  }

  @Test
  void testExplicitRelationTargetNameOverridesLeafFieldName() {
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "orderItems"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "productName"),
            SExpression.constant("iPhone")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("order")
        .where(relCondition)
        .build();

    ExistsStatementRewriter rewriter = new ExistsStatementRewriter(
        "orderItems", null, "id", "orderId", "order_item");

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    QueryStatement subQuery = (QueryStatement) rewritten.getWhere().getParam(0);
    assertEquals("order_item", subQuery.getFrom().getTableName(),
        "显式 relation target 应优先于字段路径叶子名");
  }

  @Test
  void testRelNoneRewritesToNotExists() {
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_NONE,
        SExpression.create(Operators.FIELD, "reviews"),
        SExpression.create(
            Operators.LT,
            SExpression.create(Operators.FIELD, "score"),
            SExpression.constant(3)
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("order")
        .where(relCondition)
        .build();

    ExistsStatementRewriter rewriter = new ExistsStatementRewriter(
        "reviews", null, "id", "orderId", "review");

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    assertEquals(Operators.NOT, rewritten.getWhere().getOperator());
    SExpression<?> existsExpr = rewritten.getWhere().getParamAsSExpression(0);
    assertEquals(Operators.EXISTS, existsExpr.getOperator());
  }

  @Test
  void testRelAllRewritesToNotExistsOfNegatedCondition() {
    SExpression<Boolean> relCondition = SExpression.create(
        ExtOps.REL_ALL,
        SExpression.create(Operators.FIELD, "reviews"),
        SExpression.create(
            Operators.GTE,
            SExpression.create(Operators.FIELD, "score"),
            SExpression.constant(4)
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("order")
        .where(relCondition)
        .build();

    ExistsStatementRewriter rewriter = new ExistsStatementRewriter(
        "reviews", null, "id", "orderId", "review");

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    assertEquals(Operators.NOT, rewritten.getWhere().getOperator());
    SExpression<?> existsExpr = rewritten.getWhere().getParamAsSExpression(0);
    assertEquals(Operators.EXISTS, existsExpr.getOperator());

    QueryStatement subQuery = (QueryStatement) existsExpr.getParam(0);
    SExpression<?> subQueryWhere = subQuery.getWhere();
    assertEquals(Operators.AND, subQueryWhere.getOperator());
    assertEquals(Operators.NOT, subQueryWhere.getParamAsSExpression(1).getOperator(),
        "$all 应改写为 NOT EXISTS(... WHERE NOT condition)");
  }
}
