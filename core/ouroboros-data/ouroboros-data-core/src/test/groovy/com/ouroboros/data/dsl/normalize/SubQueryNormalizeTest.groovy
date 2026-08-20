package com.ouroboros.data.dsl.normalize

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

import com.ouroboros.data.dsl.statement.QueryStatement
import com.ouroboros.data.normalize.QueryNormalizeContext
import com.querydsl.core.types.Ops
import io.vavr.control.Try
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubQueryNormalizeTest {

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

  // ── Section 23: 子查询（design-only） ──

  @Test
  @Disabled('23-01 design-only: $in 子查询 Map 右值当前未形成稳定 Map -> SUB_QUERY normalize 入口')
  @DisplayName('23-01 WHERE IN 子查询')
  void test_23_01_whereInSubQuery() {
    def result = normalizeQuery([
      'SELECT': ['id'],
      'FROM'  : 'Order',
      'WHERE' : ['userId': ['$in': ['SELECT': ['id'], 'FROM': 'User', 'WHERE': ['status': 'active']]]]
    ])

    assertFalse(result.isSuccess())
  }

  @Test
  @Disabled('23-02 design-only: exists 参数的 Map -> SUB_QUERY 自动转换链路未在设计中冻结')
  @DisplayName('23-02 EXISTS 子查询')
  void test_23_02_existsSubQuery() {
    def result = normalizeQuery([
      'SELECT': ['id'],
      'FROM'  : 'User',
      'WHERE' : ['exists', ['SELECT': ['1'], 'FROM': 'Order', 'WHERE': ['status': 'paid']]]
    ])

    assertFalse(result.isSuccess())
  }

  // ── Section 24: EXISTS 独立操作符（undocumented） ──

  @Test
  @Tag('undocumented')
  @DisplayName('24-01 S 表达式 EXISTS')
  void test_24_01_existsStandaloneOperator() {
    def result = normalizeQuery([
      'SELECT': ['id'],
      'FROM'  : 'User',
      'WHERE' : ['exists', ['SELECT': ['1'], 'FROM': 'Order', 'WHERE': ['status': 'paid']]]
    ])

    assertTrue(result.isSuccess(), result.isFailure() ? result.getCause().getMessage() : '')
    def stmt = result.get()
    assertNotNull(stmt.getWhere())
    assertEquals(Ops.EXISTS, stmt.getWhere().getOperator())
    assertTrue(stmt.getWhere().getParam(0) instanceof QueryStatement)
  }
}
