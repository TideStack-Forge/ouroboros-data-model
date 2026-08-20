package com.ouroboros.data.orchestration.rewriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.ModelQueryStatement;
import com.ouroboros.data.dsl.statement.ModelQueryStatementBuilder;
import com.ouroboros.data.dsl.statement.OmitClause;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.dsl.JoinType;

/**
 * JoinDeduplicator 单元测试
 *
 * @author Claude Code
 */
class JoinDeduplicatorTest {

  private JoinDeduplicator deduplicator;
  private OrchestrationContext context;

  @BeforeEach
  void setUp() {
    deduplicator = new JoinDeduplicator();
    context = new OrchestrationContext();
  }

  private SExpression<Boolean> createOnCondition(String leftField, String rightField) {
    return SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, leftField),
        SExpression.create(Operators.FIELD, rightField)
    );
  }

  @Test
  void testNoJoins() {
    // Given: 无 JOIN 的 QueryStatement
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .build();

    // When
    QueryStatement result = deduplicator.rewrite(statement, context);

    // Then: 返回原 statement
    assertSame(statement, result);
  }

  @Test
  void testSingleJoin() {
    // Given: 单个 JOIN
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .join(JoinType.INNERJOIN, "Department", "d", createOnCondition("Employee.deptId", "d.id"))
        .build();

    // When
    QueryStatement result = deduplicator.rewrite(statement, context);

    // Then: 返回原 statement
    assertSame(statement, result);
  }

  @Test
  void testDuplicateJoinsWithSameOnCondition_InnerJoin() {
    // Given: 同一关联路径导致的重复 INNER JOIN
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .join(JoinType.INNERJOIN, "Department", "d1", createOnCondition("Employee.deptId", "d1.id"))
        .join(JoinType.INNERJOIN, "Department", "d2", createOnCondition("Employee.deptId", "d2.id"))
        .build();

    // When
    QueryStatement result = deduplicator.rewrite(statement, context);

    // Then: 只保留一个 JOIN
    assertEquals(1, result.getJoins().size());
    assertEquals("d1", result.getJoins().get(0).getAlias());

    // 别名映射记录
    assertEquals(1, context.getAliasMapping().size());
    assertEquals("d1", context.getAliasMapping().get("d2"));
  }

  @Test
  void testDuplicateJoinsWithSameOnCondition_LeftJoinPriority() {
    // Given: 同一关联路径的 INNER JOIN + LEFT JOIN
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .join(JoinType.INNERJOIN, "Department", "d1", createOnCondition("Employee.deptId", "d1.id"))
        .join(JoinType.LEFTJOIN, "Department", "d2", createOnCondition("Employee.deptId", "d2.id"))
        .build();

    // When
    QueryStatement result = deduplicator.rewrite(statement, context);

    // Then: 保留 LEFT JOIN
    assertEquals(1, result.getJoins().size());
    assertEquals("d2", result.getJoins().get(0).getAlias());
    assertEquals(JoinType.LEFTJOIN, result.getJoins().get(0).getType());

    // 别名映射
    assertEquals("d2", context.getAliasMapping().get("d1"));
  }

  @Test
  void testReplaceAliasInWhereForEquivalentJoins() {
    // Given: WHERE 中引用了被去重的等价 JOIN 别名
    SExpression<Boolean> where = SExpression.create(
        Operators.AND,
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "d1.name"),
            SExpression.create(Operators.CONSTANT, "IT")
        ),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "d2.budget"),
            SExpression.create(Operators.CONSTANT, 100000)
        )
    );

    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .join(JoinType.INNERJOIN, "Department", "d1", createOnCondition("Employee.deptId", "d1.id"))
        .join(JoinType.INNERJOIN, "Department", "d2", createOnCondition("Employee.deptId", "d2.id"))
        .where(where)
        .build();

    // When
    QueryStatement result = deduplicator.rewrite(statement, context);

    // Then: WHERE 中的 d2 被替换为 d1
    SExpression<Boolean> newWhere = result.getWhere();

    // 检查第一个条件：d1.name = 'IT'
    SExpression<?> cond1 = newWhere.getParamAsSExpression(0);
    assertEquals(Operators.EQ, cond1.getOperator());
    SExpression<?> field1 = cond1.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, field1.getOperator());
    assertEquals("d1.name", field1.getParam(0));

    // 检查第二个条件：d2.budget 应该被替换为 d1.budget
    SExpression<?> cond2 = newWhere.getParamAsSExpression(1);
    assertEquals(Operators.EQ, cond2.getOperator());
    SExpression<?> field2 = cond2.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, field2.getOperator());
    assertEquals("d1.budget", field2.getParam(0));
  }

  @Test
  void testReplaceAliasInSelectForEquivalentJoins() {
    // Given: SELECT 中引用了被去重的等价 JOIN 别名
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .select(
            SExpression.create(Operators.FIELD, "Employee.name"),
            SExpression.create(Operators.FIELD, "d1.name"),
            SExpression.create(Operators.FIELD, "d2.budget")
        )
        .join(JoinType.INNERJOIN, "Department", "d1", createOnCondition("Employee.deptId", "d1.id"))
        .join(JoinType.INNERJOIN, "Department", "d2", createOnCondition("Employee.deptId", "d2.id"))
        .build();

    // When
    QueryStatement result = deduplicator.rewrite(statement, context);

    // Then: SELECT 中的 d2 被替换为 d1
    List<SExpression<?>> selectList = result.getSelect();
    assertEquals(3, selectList.size());
    assertEquals("Employee.name", selectList.get(0).getParam(0));
    assertEquals("d1.name", selectList.get(1).getParam(0));
    assertEquals("d1.budget", selectList.get(2).getParam(0));
  }

  @Test
  void testReplaceAliasInMultiParamSelectForEquivalentJoins() {
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .select(SExpression.alias(SExpression.field("d2", "budget"), "budget"))
        .join(JoinType.INNERJOIN, "Department", "d1", createOnCondition("Employee.deptId", "d1.id"))
        .join(JoinType.INNERJOIN, "Department", "d2", createOnCondition("Employee.deptId", "d2.id"))
        .build();

    QueryStatement result = deduplicator.rewrite(statement, context);

    assertEquals(1, result.getJoins().size());
    SExpression<?> aliased = result.getSelect().get(0);
    SExpression<?> innerField = aliased.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, innerField.getOperator());
    assertEquals("d1", innerField.getParam(0));
    assertEquals("budget", innerField.getParam(1));
  }

  @Test
  void testMixedJoins_SubQueryNotDeduplicated() {
    // Given: 等价表 JOIN + 子查询 JOIN
    QueryStatement subQuery = QueryStatement.builder()
        .from("Project")
        .select(SExpression.create(Operators.FIELD, "deptId"))
        .build();

    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .join(JoinType.INNERJOIN, "Department", "d1", createOnCondition("Employee.deptId", "d1.id"))
        .join(JoinType.INNERJOIN, "Department", "d2", createOnCondition("Employee.deptId", "d2.id"))
        .join(JoinType.LEFTJOIN, subQuery, "p", createOnCondition("Employee.id", "p.empId"))
        .build();

    // When
    QueryStatement result = deduplicator.rewrite(statement, context);

    // Then: 表 JOIN 去重，子查询 JOIN 保留
    assertEquals(2, result.getJoins().size());
    assertEquals("d1", result.getJoins().get(0).getAlias());
    assertEquals("p", result.getJoins().get(1).getAlias());
    assertTrue(result.getJoins().get(1).isSubQuery());
  }

  @Test
  void testDeduplicationPreservesModelQueryStatementType() {
    ModelQueryStatementBuilder builder = new ModelQueryStatementBuilder();
    builder.from("Employee");
    builder.join(JoinType.INNERJOIN, "Department", "d1", createOnCondition("Employee.deptId", "d1.id"));
    builder.join(JoinType.INNERJOIN, "Department", "d2", createOnCondition("Employee.deptId", "d2.id"));
    builder.populateClause(PopulateClause.fromRaw("department"));
    builder.omitClause(OmitClause.fromRaw("salary"));
    QueryStatement statement = builder.build();

    QueryStatement result = deduplicator.rewrite(statement, context);

    assertInstanceOf(ModelQueryStatement.class, result,
        "JOIN 去重后不应把 ModelQueryStatement 降级为普通 QueryStatement");
    ModelQueryStatement modelResult = (ModelQueryStatement) result;
    assertEquals(PopulateClause.fromRaw("department"), modelResult.getPopulateClause());
    assertEquals(OmitClause.fromRaw("salary"), modelResult.getOmitClause());
  }

  @Test
  void testDistinctJoinsToSameTableShouldBePreserved() {
    QueryStatement statement = QueryStatement.builder()
        .from("Employee")
        .select(
            SExpression.alias(SExpression.field("entryDisplay", "id"), "entryDisplay__id"),
            SExpression.alias(SExpression.field("driverDisplay", "id"), "driverDisplay__id"))
        .join(JoinType.LEFTJOIN, "Device", "entryDisplay", createOnCondition("Employee.entryDisplayId", "entryDisplay.id"))
        .join(JoinType.LEFTJOIN, "Device", "driverDisplay", createOnCondition("Employee.driverDisplayId", "driverDisplay.id"))
        .build();

    QueryStatement result = deduplicator.rewrite(statement, context);

    assertEquals(2, result.getJoins().size(), "同表但不同 ON 条件的 JOIN 不应被合并");
    assertTrue(context.getAliasMapping().isEmpty(), "未发生去重时不应记录别名映射");

    List<SExpression<?>> selectList = result.getSelect();
    assertEquals(2, selectList.size());
    SExpression<?> firstInner = selectList.get(0).getParamAsSExpression(0);
    SExpression<?> secondInner = selectList.get(1).getParamAsSExpression(0);
    assertEquals("entryDisplay", firstInner.getParam(0));
    assertEquals("id", firstInner.getParam(1));
    assertEquals("driverDisplay", secondInner.getParam(0));
    assertEquals("id", secondInner.getParam(1));
  }
}
