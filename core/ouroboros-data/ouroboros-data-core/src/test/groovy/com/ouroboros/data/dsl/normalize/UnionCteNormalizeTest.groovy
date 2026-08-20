package com.ouroboros.data.dsl.normalize

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import com.ouroboros.data.dsl.statement.QueryStatement
import com.ouroboros.data.normalize.QueryNormalizeContext
import com.ouroboros.data.dsl.Order
import io.vavr.control.Try
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnionCteNormalizeTest {

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

  // ── Section 16: UNION / UNION ALL ──

  @Test
  @DisplayName('16-01 UNION')
  void test_16_01_union() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': ['name'],
      'FROM'  : 'User',
      'UNION' : [
        ['SELECT': ['name'], 'FROM': 'User'],
        ['SELECT': ['name'], 'FROM': 'Department']
      ]
    ]))

    assertEquals(2, stmt.getUnions().size())
    assertFalse(stmt.getUnions().get(0).isAll())
    assertFalse(stmt.getUnions().get(1).isAll())
    assertEquals('User', stmt.getUnions().get(0).getQuery().getFrom().getTableName())
    assertEquals('Department', stmt.getUnions().get(1).getQuery().getFrom().getTableName())
  }

  @Test
  @DisplayName('16-02 UNION ALL')
  void test_16_02_unionAll() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT'  : ['name'],
      'FROM'    : 'User',
      'UNIONALL': [
        ['SELECT': ['name'], 'FROM': 'User'],
        ['SELECT': ['name'], 'FROM': 'Department']
      ]
    ]))

    assertEquals(2, stmt.getUnions().size())
    assertTrue(stmt.getUnions().get(0).isAll())
    assertTrue(stmt.getUnions().get(1).isAll())
  }

  @Test
  @DisplayName('16-03 UNION + ORDER')
  void test_16_03_unionWithOrder() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': ['name'],
      'FROM'  : 'User',
      'UNION' : [
        ['SELECT': ['name'], 'FROM': 'User'],
        ['SELECT': ['name'], 'FROM': 'Department']
      ],
      'ORDER' : [['name': 'asc']]
    ]))

    assertEquals(2, stmt.getUnions().size())
    assertEquals(1, stmt.getOrders().size())
    assertEquals('name', stmt.getOrders().get(0).getColumn())
    assertEquals(Order.ASC, stmt.getOrders().get(0).getOrder())
  }

  // ── Section 17: WITH / CTE ──

  @Test
  @DisplayName('17-01 基础 CTE')
  void test_17_01_basicCte() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'WITH'  : [
        'activeUsers': ['SELECT': ['id', 'name'], 'FROM': 'User', 'WHERE': ['status': 'active']]
      ],
      'SELECT': ['name'],
      'FROM'  : 'activeUsers'
    ]))

    assertEquals(1, stmt.getWith().size())
    assertEquals('activeUsers', stmt.getWith().get(0).getAlias())
    assertFalse(stmt.getWith().get(0).isRecursive())
    assertEquals('User', stmt.getWith().get(0).getQuery().getFrom().getTableName())
  }

  @Test
  @DisplayName('17-02 多个 CTE')
  void test_17_02_multipleCte() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'WITH'  : [
        'activeUsers' : ['SELECT': ['id'], 'FROM': 'User'],
        'activeOrders': ['SELECT': ['id'], 'FROM': 'Order']
      ],
      'SELECT': ['id'],
      'FROM'  : 'activeUsers'
    ]))

    assertEquals(2, stmt.getWith().size())
    assertEquals('activeUsers', stmt.getWith().get(0).getAlias())
    assertEquals('activeOrders', stmt.getWith().get(1).getAlias())
  }

  @Test
  @DisplayName('17-03 WITH RECURSIVE')
  void test_17_03_withRecursive() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'WITHRECURSIVE': [
        'nodeTree': ['SELECT': ['id', 'parentId'], 'FROM': 'Department']
      ],
      'SELECT'       : ['id'],
      'FROM'         : 'nodeTree'
    ]))

    assertEquals(1, stmt.getWith().size())
    assertEquals('nodeTree', stmt.getWith().get(0).getAlias())
    assertTrue(stmt.getWith().get(0).isRecursive())
    assertEquals('Department', stmt.getWith().get(0).getQuery().getFrom().getTableName())
  }
}
