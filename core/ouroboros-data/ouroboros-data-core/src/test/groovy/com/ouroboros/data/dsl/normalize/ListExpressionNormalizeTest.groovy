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
class ListExpressionNormalizeTest {

  private QueryNormalizeContext ctx

  @BeforeAll
  void setupContext() {
    ctx = QueryNormalizeContext.builder()
      .withDefaultNormalizers()
      .build()
  }

  private Try<SExpression<?>> normalizeExpression(Object expr) {
    ctx.forClause('WHERE').normalizeExpression(expr, 'root')
  }

  private SExpression<?> assertSuccess(Try<SExpression<?>> result) {
    assertTrue(result.isSuccess(), result.isFailure() ? result.getCause().getMessage() : '')
    result.get()
  }

  private void assertFailure(Try<SExpression<?>> result) {
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

  // ── Section 6: S 表达式语法 ──

  @Test
  @DisplayName('6-01 等值比较')
  void test_6_01_equalsComparison() {
    def actual = assertSuccess(normalizeExpression(['=', ['departmentId'], 1001]))
    assertEquals(expr(Ops.EQ, field('departmentId'), constant(1001)), actual)
  }

  @Test
  @DisplayName('6-02 大于等于')
  void test_6_02_greaterThanOrEqualComparison() {
    def actual = assertSuccess(normalizeExpression(['>=', ['age'], 18]))
    assertEquals(expr(Ops.GOE, field('age'), constant(18)), actual)
  }

  @Test
  @DisplayName('6-03 AND 组合')
  void test_6_03_andCombination() {
    def actual = assertSuccess(normalizeExpression(['and', ['>=', ['age'], 18], ['=', ['status'], 'active']]))
    def expected = expr(Ops.AND,
      expr(Ops.GOE, field('age'), constant(18)),
      expr(Ops.EQ, field('status'), constant('active'))
    )
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('6-04 OR 组合')
  void test_6_04_orCombination() {
    def actual = assertSuccess(normalizeExpression(['or', ['=', ['name'], '张三'], ['=', ['name'], '李四']]))
    def expected = expr(Ops.OR,
      expr(Ops.EQ, field('name'), constant('张三')),
      expr(Ops.EQ, field('name'), constant('李四'))
    )
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('6-05 NOT')
  void test_6_05_notOperator() {
    def actual = assertSuccess(normalizeExpression(['not', ['=', ['status'], 'deleted']]))
    assertEquals(expr(Ops.NOT, expr(Ops.EQ, field('status'), constant('deleted'))), actual)
  }

  @Test
  @DisplayName('6-06 BETWEEN')
  void test_6_06_betweenOperator() {
    def actual = assertSuccess(normalizeExpression(['between', ['age'], 18, 60]))
    assertEquals(expr(Ops.BETWEEN, field('age'), constant(18), constant(60)), actual)
  }

  @Test
  @DisplayName('6-07 IN')
  void test_6_07_inOperator() {
    assertFailure(normalizeExpression(['in', ['status'], ['active', 'pending']]))
  }

  @Test
  @DisplayName('6-08 LIKE')
  void test_6_08_likeOperator() {
    def actual = assertSuccess(normalizeExpression(['like', ['name'], '%张%']))
    assertEquals(expr(Ops.LIKE, field('name'), constant('%张%')), actual)
  }

  @Test
  @DisplayName('6-09 IS_NULL')
  void test_6_09_isNullOperator() {
    def actual = assertSuccess(normalizeExpression(['isNull', ['name']]))
    assertEquals(expr(Ops.IS_NULL, field('name')), actual)
  }

  @Test
  @DisplayName('6-10 嵌套布尔')
  void test_6_10_nestedBoolean() {
    def actual = assertSuccess(normalizeExpression(['and', ['or', ['=', ['a'], 1], ['=', ['b'], 2]], ['=', ['c'], 3]]))
    def expected = expr(Ops.AND,
      expr(Ops.OR,
        expr(Ops.EQ, field('a'), constant(1)),
        expr(Ops.EQ, field('b'), constant(2))
      ),
      expr(Ops.EQ, field('c'), constant(3))
    )
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('6-11 $field 显式字段')
  void test_6_11_explicitFieldOperator() {
    def actual = assertSuccess(normalizeExpression(['=', ['field', 'name'], '张三']))
    def expected = expr(Ops.EQ, field('name'), constant('张三'))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('6-12 $constant 常量')
  void test_6_12_constantOperator() {
    def actual = assertSuccess(normalizeExpression(['=', ['field', 'status'], ['constant', 'active']]))
    def expected = expr(Ops.EQ, field('status'), constant('active'))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('6-13 函数调用')
  void test_6_13_functionCallSyntax() {
    assertFailure(normalizeExpression(['=', ['year', ['createdAt']], ['year', ['now']]]))
  }
}
