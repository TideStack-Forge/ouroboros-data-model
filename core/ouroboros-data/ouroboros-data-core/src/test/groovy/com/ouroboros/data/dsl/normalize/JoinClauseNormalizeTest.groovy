package com.ouroboros.data.dsl.normalize

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import com.ouroboros.data.dsl.ExtOps
import com.ouroboros.data.dsl.SExpression
import com.ouroboros.data.dsl.statement.QueryStatement
import com.ouroboros.data.normalize.QueryNormalizeContext
import com.ouroboros.data.dsl.JoinType
import com.querydsl.core.types.Ops
import io.vavr.control.Try
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinClauseNormalizeTest {

  private QueryNormalizeContext ctx

  @BeforeAll
  void setupContext() {
    ctx = QueryNormalizeContext.builder()
      .withDefaultNormalizers()
      .build()
  }

  private Try<QueryStatement> normalizeQuery(Map<String, ?> queryMap) {
    ctx.normalizeQuery(queryMap)
  }

  private QueryStatement assertQuerySuccess(Try<QueryStatement> result) {
    assertTrue(result.isSuccess(), result.isFailure() ? result.getCause().getMessage() : '')
    result.get()
  }

  private QueryStatement.JoinEntry assertSingleJoin(QueryStatement statement, JoinType joinType) {
    assertEquals(1, statement.getJoins().size())
    def join = statement.getJoins().get(0)
    assertEquals(joinType, join.getType())
    join
  }

  private void assertEqOnCondition(SExpression<Boolean> onExpr, String leftField, String rightField) {
    assertEquals(Ops.EQ, onExpr.getOperator())
    def leftExpr = onExpr.getParamAsSExpression(0)
    def rightExpr = onExpr.getParamAsSExpression(1)
    assertEquals(ExtOps.FIELD, leftExpr.getOperator())
    assertEquals(ExtOps.FIELD, rightExpr.getOperator())
    assertEquals(leftField, leftExpr.getParam(0))
    assertEquals(rightField, rightExpr.getParam(0))
  }

  // ── Section 15: JOIN 子句 ──

  @Test
  @DisplayName('15-01 LEFT JOIN')
  void test_15_01_leftJoin() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT'  : ['id'],
      'FROM'    : 'User',
      'LEFTJOIN': ['d': 'Department', 'on': ['departmentId': 'id']]
    ]))

    def join = assertSingleJoin(stmt, JoinType.LEFTJOIN)
    assertEquals('Department', join.getTableName())
    assertEqOnCondition(join.getOn(), 'departmentId', 'id')
  }

  @Test
  @DisplayName('15-02 INNER JOIN')
  void test_15_02_innerJoin() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'     : 'User',
      'INNERJOIN': ['d': 'Department', 'on': ['departmentId': 'id']]
    ]))

    def join = assertSingleJoin(stmt, JoinType.INNERJOIN)
    assertEquals('Department', join.getTableName())
  }

  @Test
  @DisplayName('15-03 RIGHT JOIN')
  void test_15_03_rightJoin() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'     : 'User',
      'RIGHTJOIN': ['d': 'Department', 'on': ['departmentId': 'id']]
    ]))

    def join = assertSingleJoin(stmt, JoinType.RIGHTJOIN)
    assertEquals('Department', join.getTableName())
  }

  @Test
  @DisplayName('15-04 FULL JOIN')
  void test_15_04_fullJoin() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'    : 'User',
      'FULLJOIN': ['d': 'Department', 'on': ['departmentId': 'id']]
    ]))

    def join = assertSingleJoin(stmt, JoinType.FULLJOIN)
    assertEquals('Department', join.getTableName())
  }

  @Test
  @DisplayName('15-05 CROSS JOIN')
  void test_15_05_crossJoin() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'     : 'User',
      'CROSSJOIN': ['d': 'Department']
    ]))

    def join = assertSingleJoin(stmt, JoinType.DEFAULT)
    assertEquals('Department', join.getTableName())
    assertTrue(join.getOn().isEmpty())
  }

  @Test
  @DisplayName('15-06 多表 JOIN')
  void test_15_06_multiJoin() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'    : 'Order',
      'LEFTJOIN': [
        ['u': 'User', 'on': ['userId': 'id']],
        ['d': 'Department', 'on': ['departmentId': 'id']]
      ]
    ]))

    assertEquals(2, stmt.getJoins().size())
    assertEquals('User', stmt.getJoins().get(0).getTableName())
    assertEquals('Department', stmt.getJoins().get(1).getTableName())
  }

  @Test
  @DisplayName('15-07 JOIN + WHERE')
  void test_15_07_joinWithWhere() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'    : 'Order',
      'LEFTJOIN': ['u': 'User', 'on': ['userId': 'id']],
      'WHERE'   : ['status': 'active']
    ]))

    assertEquals(1, stmt.getJoins().size())
    assertFalse(stmt.getWhere().isEmpty())
  }

  @Test
  @DisplayName('15-08 ON 条件 Map 形式')
  void test_15_08_onMapCondition() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'Order',
      'JOIN' : ['u': 'User', 'on': ['userId': 'id']]
    ]))

    def join = assertSingleJoin(stmt, JoinType.INNERJOIN)
    assertEqOnCondition(join.getOn(), 'userId', 'id')
  }

  @Test
  @DisplayName('15-09 ON 条件 List 形式')
  void test_15_09_onListCondition() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'Order',
      'JOIN' : ['u': 'User', 'on': [['userId': 'id'], ['status': 'active']]]
    ]))

    def join = assertSingleJoin(stmt, JoinType.INNERJOIN)
    assertEquals(Ops.AND, join.getOn().getOperator())
    assertEquals(2, join.getOn().getParams().size())
  }
}
