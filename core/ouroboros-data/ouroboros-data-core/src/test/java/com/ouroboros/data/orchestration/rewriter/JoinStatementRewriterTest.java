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
 * JoinStatementRewriter 测试
 *
 * <p>Round 3 更新：验证 JOIN 和字段替换的正确性
 */
class JoinStatementRewriterTest {

  private OrchestrationContext context;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
  }

  @Test
  void testAddInnerJoin() {
    // Given: 含 WHERE 的语句
    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "department.name"),
        SExpression.constant("IT")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(whereCondition)
        .build();

    JoinStatementRewriter rewriter = new JoinStatementRewriter("department", null, false, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: 应该包含 INNER JOIN
    assertNotNull(rewritten);
    assertFalse(rewritten.getJoins().isEmpty());
  }

  @Test
  void testReplaceFieldReferences() {
    // Given: WHERE 含关联字段引用
    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "department.name"),
        SExpression.constant("IT")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(whereCondition)
        .build();

    JoinStatementRewriter rewriter = new JoinStatementRewriter("department", null, false, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: 字段引用应被替换（department.name → department.name with alias）
    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertFalse(where.isEmpty());
  }

  @Test
  void testNoWhereNoReplace() {
    // Given: 无 WHERE 的语句
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .build();

    JoinStatementRewriter rewriter = new JoinStatementRewriter("department", null, false, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: 应该添加 JOIN 但 WHERE 仍为空
    assertNotNull(rewritten);
    assertFalse(rewritten.getJoins().isEmpty());
    assertTrue(rewritten.getWhere().isEmpty());
  }

  @Test
  void testJoinAliasGeneration() {
    // Given: 含点号的关联字段路径
    JoinStatementRewriter rewriter = new JoinStatementRewriter("user.department", "user", false, "id", "id");

    // When: 获取字段路径
    String fieldPath = rewriter.relationFieldPath();

    // Then: 字段路径正确
    assertEquals("user.department", fieldPath);
  }

  @Test
  void testExplicitRelationTargetNameOverridesFieldPathSegment() {
    QueryStatement statement = QueryStatement.builder()
        .from("account")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "owner.displayName"),
            SExpression.constant("Alice")
        ))
        .build();

    JoinStatementRewriter rewriter =
        new JoinStatementRewriter("owner", null, false, "ownerId", "id", "UserProfile");

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    assertEquals(1, rewritten.getJoins().size());
    assertEquals("UserProfile", rewritten.getJoins().get(0).getTableName());
    assertEquals("owner", rewritten.getJoins().get(0).getAlias());
  }

  @Test
  void testMultipleFieldReferences() {
    // Given: WHERE 含多个关联字段引用
    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.AND,
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "department.name"),
            SExpression.constant("IT")
        ),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "department.code"),
            SExpression.constant("D001")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(whereCondition)
        .build();

    JoinStatementRewriter rewriter = new JoinStatementRewriter("department", null, false, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: 所有关联字段都应被替换，且包含 JOIN
    assertNotNull(rewritten);
    assertFalse(rewritten.getJoins().isEmpty());
    assertFalse(rewritten.getWhere().isEmpty());
  }

  @Test
  void testAddLeftJoin() {
    // Given: useLeftJoin=true
    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "department.name"),
        SExpression.constant("IT")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(whereCondition)
        .build();

    JoinStatementRewriter rewriter = new JoinStatementRewriter("department", null, true, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: 应该包含 LEFT JOIN
    assertNotNull(rewritten);
    assertFalse(rewritten.getJoins().isEmpty());
  }

  @Test
  void testLeftJoinWithFieldReferences() {
    // Given: useLeftJoin=true + WHERE 含多个关联字段引用
    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.AND,
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "department.name"),
            SExpression.constant("IT")
        ),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "department.code"),
            SExpression.constant("D001")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(whereCondition)
        .build();

    JoinStatementRewriter rewriter = new JoinStatementRewriter("department", null, true, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: LEFT JOIN + 字段引用替换均正常
    assertNotNull(rewritten);
    assertFalse(rewritten.getJoins().isEmpty());
    assertFalse(rewritten.getWhere().isEmpty());
  }

  @Test
  void testInnerJoinDefault() {
    // Given: useLeftJoin=false（明确 INNER JOIN）
    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "department.name"),
        SExpression.constant("Engineering")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(whereCondition)
        .build();

    JoinStatementRewriter rewriter = new JoinStatementRewriter("department", null, false, "id", "id");

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: 应该包含 INNER JOIN
    assertNotNull(rewritten);
    assertFalse(rewritten.getJoins().isEmpty());
  }

  @Test
  void testReplaceStructuredFieldReferences() {
    SExpression<Boolean> whereCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "user", "department", "name"),
        SExpression.constant("IT")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(whereCondition)
        .build();

    JoinStatementRewriter rewriter = new JoinStatementRewriter("user.department", "user", false, "departmentId", "id");

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertEquals(Operators.EQ, where.getOperator());

    SExpression<?> rewrittenField = where.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, rewrittenField.getOperator());
    assertEquals(2, rewrittenField.getParams().size());
    assertEquals("user_department", rewrittenField.getParam(0));
    assertEquals("name", rewrittenField.getParam(1));
  }

  @Test
  void testRewriteRelationAnyToInnerCondition() {
    SExpression<Boolean> whereCondition = SExpression.create(
        ExtOps.REL_ANY,
        SExpression.create(Operators.FIELD, "department"),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "name"),
            SExpression.constant("IT")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(whereCondition)
        .build();

    JoinStatementRewriter rewriter = new JoinStatementRewriter("department", null, false, "departmentId", "id");

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    assertNotNull(rewritten);
    assertFalse(rewritten.getJoins().isEmpty());
    assertEquals(Operators.EQ, rewritten.getWhere().getOperator());
    SExpression<?> rewrittenField = rewritten.getWhere().getParamAsSExpression(0);
    assertEquals(Operators.FIELD, rewrittenField.getOperator());
    assertEquals(2, rewrittenField.getParams().size());
    assertEquals("department", rewrittenField.getParam(0));
    assertEquals("name", rewrittenField.getParam(1));
  }
}
