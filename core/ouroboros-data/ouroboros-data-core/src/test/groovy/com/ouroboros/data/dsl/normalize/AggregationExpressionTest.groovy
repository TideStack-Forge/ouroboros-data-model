package com.ouroboros.data.dsl.normalize

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import com.ouroboros.data.dsl.ExtOps
import com.ouroboros.data.dsl.SExpression
import com.ouroboros.data.normalize.QueryNormalizeContext
import com.querydsl.core.types.Operator
import com.querydsl.core.types.Ops
import io.vavr.control.Try
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregationExpressionTest {

  private QueryNormalizeContext ctx

  @BeforeAll
  void setupContext() {
    ctx = QueryNormalizeContext.builder()
      .withDefaultNormalizers()
      .build()
  }

  private Try<SExpression<?>> normalizeExpression(Object expr, String clauseType) {
    ctx.forClause(clauseType).normalizeExpression(expr, 'root')
  }

  private Try<SExpression<Boolean>> normalizeCondition(Object expr, String clauseType) {
    ctx.forClause(clauseType).normalizeCondition(expr, 'root')
  }

  private void assertSuccess(Try<?> result) {
    assertTrue(result.isSuccess(), result.isFailure() ? result.getCause().getMessage() : '')
  }

  private SExpression<Boolean> assertConditionSuccess(Try<SExpression<Boolean>> result) {
    assertTrue(result.isSuccess(), result.isFailure() ? result.getCause().getMessage() : '')
    result.get()
  }

  private void assertFailure(Try<?> result) {
    assertFalse(result.isSuccess())
  }

  private static SExpression<?> field(String... path) {
    SExpression.create(ExtOps.FIELD, (Object[]) path)
  }

  private static SExpression<?> constant(Object value) {
    SExpression.constant(value)
  }

  private static SExpression<?> expr(Operator operator, Object... params) {
    SExpression.create(operator, params)
  }

  // ── Section 7: 聚合函数 ──

  @Test
  @DisplayName('7-01 COUNT(*)')
  void test_7_01_countStar() {
    assertFailure(normalizeExpression(['count', ['*']], 'SELECT'))
  }

  @Test
  @DisplayName('7-02 COUNT(field)')
  void test_7_02_countField() {
    assertFailure(normalizeExpression(['count', ['id']], 'SELECT'))
  }

  @Test
  @DisplayName('7-03 SUM')
  void test_7_03_sumAggregation() {
    assertFailure(normalizeExpression(['sum', ['amount']], 'SELECT'))
  }

  @Test
  @DisplayName('7-04 AVG')
  void test_7_04_avgAggregation() {
    assertFailure(normalizeExpression(['avg', ['amount']], 'SELECT'))
  }

  @Test
  @DisplayName('7-05 MAX')
  void test_7_05_maxAggregation() {
    assertFailure(normalizeExpression(['max', ['age']], 'SELECT'))
  }

  @Test
  @DisplayName('7-06 MIN')
  void test_7_06_minAggregation() {
    assertFailure(normalizeExpression(['min', ['age']], 'SELECT'))
  }

  @Test
  @DisplayName('7-07 聚合加别名')
  void test_7_07_aggregationWithAlias() {
    assertFailure(normalizeExpression([totalAmount: ['sum', ['amount']]], 'SELECT'))
  }

  @Test
  @DisplayName('7-08 HAVING 聚合条件')
  void test_7_08_havingAggregationCondition() {
    def actual = assertConditionSuccess(normalizeCondition(['>', ['count', ['*']], 5], 'HAVING'))
    def expected = expr(Ops.GT, expr(Ops.AggOps.COUNT_AGG, field('*')), constant(5))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('7-09 WHERE 误用聚合')
  void test_7_09_whereAggregationMisuse() {
    assertFailure(normalizeCondition(['sum', ['amount']], 'WHERE'))
  }

  // ── Section 8: CASE/WHEN ──

  @Test
  @DisplayName('8-01 搜索式 CASE')
  void test_8_01_searchCaseWhen() {
    assertFailure(normalizeExpression([
      'case',
      ['when', ['>', ['age'], 60], '老年'],
      ['when', ['>', ['age'], 18], '成年'],
      ['else', '未成年']
    ], 'SELECT'))
  }

  @Test
  @DisplayName('8-02 简单式 CASE_EQ')
  void test_8_02_simpleCaseEq() {
    assertFailure(normalizeExpression([
      'caseEq',
      ['age'],
      ['when', 1, '一'],
      ['when', 2, '二'],
      ['else', '其他']
    ], 'SELECT'))
  }

  @Test
  @DisplayName('8-03 CASE 无 ELSE')
  void test_8_03_caseWithoutElse() {
    assertFailure(normalizeExpression([
      'case',
      ['when', ['>', ['age'], 18], '成年']
    ], 'SELECT'))
  }

  @Test
  @DisplayName('8-04 SELECT 中 CASE')
  void test_8_04_caseInSelect() {
    assertFailure(normalizeExpression([
      label: ['case', ['when', ['>', ['age'], 60], '老年'], ['else', '其他']]
    ], 'SELECT'))
  }
}
