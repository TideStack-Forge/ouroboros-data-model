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
class MapExpressionNormalizeTest {

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

  private Try<SExpression<Boolean>> normalizeCondition(Object expr) {
    ctx.forClause('WHERE').normalizeCondition(expr, 'root')
  }

  private SExpression<?> assertSuccess(Try<SExpression<?>> result) {
    assertTrue(result.isSuccess(), result.isFailure() ? result.getCause().getMessage() : '')
    result.get()
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

  private static SExpression<?> eq(String fieldName, Object value) {
    expr(Ops.EQ, field(fieldName), constant(value))
  }

  private static SExpression<?> gte(String fieldName, Object value) {
    expr(Ops.GOE, field(fieldName), constant(value))
  }

  private static SExpression<?> gt(String fieldName, Object value) {
    expr(Ops.GT, field(fieldName), constant(value))
  }

  private static SExpression<?> lt(String fieldName, Object value) {
    expr(Ops.LT, field(fieldName), constant(value))
  }

  private static SExpression<?> lte(String fieldName, Object value) {
    expr(Ops.LOE, field(fieldName), constant(value))
  }

  private static SExpression<?> ne(String fieldName, Object value) {
    expr(Ops.NE, field(fieldName), constant(value))
  }

  // ── Section 1: 隐式等值与基础 Map ──

  @Test
  @DisplayName('1-01 字符串隐式等值')
  void test_1_01_stringImplicitEquals() {
    def actual = assertSuccess(normalizeExpression([name: '张三']))
    assertEquals(eq('name', '张三'), actual)
  }

  @Test
  @DisplayName('1-02 数值隐式等值')
  void test_1_02_numberImplicitEquals() {
    def actual = assertSuccess(normalizeExpression([age: 25]))
    assertEquals(eq('age', 25), actual)
  }

  @Test
  @DisplayName('1-03 布尔隐式等值')
  void test_1_03_booleanImplicitEquals() {
    def actual = assertSuccess(normalizeExpression([active: true]))
    assertEquals(eq('active', true), actual)
  }

  @Test
  @DisplayName('1-04 null 隐式 IS_NULL')
  void test_1_04_nullImplicitIsNull() {
    def actual = assertSuccess(normalizeExpression([name: null]))
    assertEquals(expr(Ops.IS_NULL, field('name')), actual)
  }

  @Test
  @DisplayName('1-05 List 隐式 IN')
  void test_1_05_listImplicitIn() {
    def actual = assertSuccess(normalizeExpression([status: ['active', 'pending']]))
    assertEquals(expr(Ops.IN, field('status'), constant(['active', 'pending'])), actual)
  }

  @Test
  @DisplayName('1-06 空 List 隐式 IN')
  void test_1_06_emptyListImplicitIn() {
    def actual = assertSuccess(normalizeExpression([status: []]))
    assertEquals(expr(Ops.IN, field('status'), constant([])), actual)
  }

  @Test
  @DisplayName('1-07 多字段隐式 AND')
  void test_1_07_multiFieldImplicitAnd() {
    def actual = assertConditionSuccess(normalizeCondition([name: '张三', age: 25]))
    def expected = expr(Ops.AND,
      eq('name', '张三'),
      eq('age', 25)
    )
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('1-08 单字段不包 AND')
  void test_1_08_singleFieldWithoutAnd() {
    def actual = assertSuccess(normalizeExpression([name: '张三']))
    assertEquals(eq('name', '张三'), actual)
  }

  // ── Section 2: 比较操作符 ──

  @Test
  @DisplayName('2-01 $eq 显式等值')
  void test_2_01_explicitEq() {
    def actual = assertSuccess(normalizeExpression([name: ['$eq': '张三']]))
    assertEquals(eq('name', '张三'), actual)
  }

  @Test
  @DisplayName('2-02 $ne 不等于')
  void test_2_02_notEquals() {
    def actual = assertSuccess(normalizeExpression([status: ['$ne': 'deleted']]))
    assertEquals(ne('status', 'deleted'), actual)
  }

  @Test
  @DisplayName('2-03 $gt 大于')
  void test_2_03_greaterThan() {
    def actual = assertSuccess(normalizeExpression([age: ['$gt': 18]]))
    assertEquals(gt('age', 18), actual)
  }

  @Test
  @DisplayName('2-04 $gte 大于等于')
  void test_2_04_greaterThanOrEqual() {
    def actual = assertSuccess(normalizeExpression([age: ['$gte': 18]]))
    assertEquals(gte('age', 18), actual)
  }

  @Test
  @DisplayName('2-05 $lt 小于')
  void test_2_05_lessThan() {
    def actual = assertSuccess(normalizeExpression([age: ['$lt': 60]]))
    assertEquals(lt('age', 60), actual)
  }

  @Test
  @DisplayName('2-06 $lte 小于等于')
  void test_2_06_lessThanOrEqual() {
    def actual = assertSuccess(normalizeExpression([age: ['$lte': 60]]))
    assertEquals(lte('age', 60), actual)
  }

  @Test
  @DisplayName('2-07 $between 区间')
  void test_2_07_betweenRange() {
    def actual = assertSuccess(normalizeExpression([age: ['$between': [18, 60]]]))
    def expected = expr(Ops.BETWEEN, field('age'), constant(18), constant(60))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('2-08 $in 包含')
  void test_2_08_inOperator() {
    def actual = assertSuccess(normalizeExpression([status: ['$in': ['active', 'pending']]]))
    assertEquals(expr(Ops.IN, field('status'), constant(['active', 'pending'])), actual)
  }

  @Test
  @DisplayName('2-09 $notIn 不包含')
  void test_2_09_notInOperator() {
    def actual = assertSuccess(normalizeExpression([status: ['$notIn': ['deleted']]]))
    assertEquals(expr(Ops.NOT_IN, field('status'), constant(['deleted'])), actual)
  }

  @Test
  @DisplayName('2-10 无前缀 gt 兼容')
  void test_2_10_compatGtWithoutPrefix() {
    def actual = assertSuccess(normalizeExpression([age: [gt: 18]]))
    assertEquals(gt('age', 18), actual)
  }

  @Test
  @DisplayName('2-11 无前缀 gte 兼容')
  void test_2_11_compatGteWithoutPrefix() {
    def actual = assertSuccess(normalizeExpression([age: [gte: 18]]))
    assertEquals(gte('age', 18), actual)
  }

  @Test
  @DisplayName('2-12 符号 >=')
  void test_2_12_symbolGreaterThanOrEqual() {
    def actual = assertSuccess(normalizeExpression([age: ['>=': 18]]))
    assertEquals(gte('age', 18), actual)
  }

  @Test
  @DisplayName('2-13 符号 >')
  void test_2_13_symbolGreaterThan() {
    def actual = assertSuccess(normalizeExpression([age: ['>': 18]]))
    assertEquals(gt('age', 18), actual)
  }

  @Test
  @DisplayName('2-14 符号 <')
  void test_2_14_symbolLessThan() {
    def actual = assertSuccess(normalizeExpression([age: ['<': 60]]))
    assertEquals(lt('age', 60), actual)
  }

  @Test
  @DisplayName('2-15 符号 <=')
  void test_2_15_symbolLessThanOrEqual() {
    def actual = assertSuccess(normalizeExpression([age: ['<=': 60]]))
    assertEquals(lte('age', 60), actual)
  }

  @Test
  @DisplayName('2-16 符号 =')
  void test_2_16_symbolEquals() {
    def actual = assertSuccess(normalizeExpression([age: ['=': 25]]))
    assertEquals(eq('age', 25), actual)
  }

  @Test
  @DisplayName('2-17 符号 !=')
  void test_2_17_symbolNotEquals() {
    def actual = assertSuccess(normalizeExpression([status: ['!=': 'deleted']]))
    assertEquals(ne('status', 'deleted'), actual)
  }

  @Test
  @DisplayName('2-18 多操作符组合')
  void test_2_18_multiOperatorsCombination() {
    def actual = assertConditionSuccess(normalizeCondition([age: ['$gte': 18, '$lt': 60]]))
    def expected = expr(Ops.AND, gte('age', 18), lt('age', 60))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('2-19 三操作符组合')
  void test_2_19_threeOperatorsCombination() {
    def actual = assertConditionSuccess(normalizeCondition([age: ['$gte': 18, '$lt': 60, '$ne': 30]]))
    def expected = expr(Ops.AND, gte('age', 18), lt('age', 60), ne('age', 30))
    assertEquals(expected, actual)
  }

  // ── Section 3: 字符串操作符 ──

  @Test
  @DisplayName('3-01 $contains 包含')
  void test_3_01_containsOperator() {
    def actual = assertSuccess(normalizeExpression([name: ['$contains': '张']]))
    assertEquals(expr(Ops.LIKE, field('name'), constant('张')), actual)
  }

  @Test
  @DisplayName('3-02 $startsWith 前缀')
  void test_3_02_startsWithOperator() {
    def actual = assertSuccess(normalizeExpression([name: ['$startsWith': '张']]))
    assertEquals(expr(Ops.STARTS_WITH, field('name'), constant('张')), actual)
  }

  @Test
  @DisplayName('3-03 $endsWith 后缀')
  void test_3_03_endsWithOperator() {
    def actual = assertSuccess(normalizeExpression([email: ['$endsWith': '@test.com']]))
    assertEquals(expr(Ops.ENDS_WITH, field('email'), constant('@test.com')), actual)
  }

  @Test
  @DisplayName('3-04 $like 模糊匹配')
  void test_3_04_likeOperator() {
    def actual = assertSuccess(normalizeExpression([name: ['$like': '%张%三']]))
    assertEquals(expr(Ops.LIKE, field('name'), constant('%张%三')), actual)
  }

  @Test
  @DisplayName('3-05 $notLike')
  void test_3_05_notLikeOperator() {
    def actual = assertSuccess(normalizeExpression([name: ['$notLike': '%test%']]))
    assertEquals(expr(ExtOps.NOT_LIKE, field('name'), constant('%test%')), actual)
  }

  @Test
  @DisplayName('3-06 $notContains 别名')
  void test_3_06_notContainsAlias() {
    def actual = assertSuccess(normalizeExpression([name: ['$notContains': 'test']]))
    assertEquals(expr(ExtOps.NOT_LIKE, field('name'), constant('test')), actual)
  }

  @Test
  @DisplayName('3-07 无前缀 contains 兼容')
  void test_3_07_compatContainsWithoutPrefix() {
    def actual = assertSuccess(normalizeExpression([name: [contains: '张']]))
    assertEquals(expr(Ops.LIKE, field('name'), constant('张')), actual)
  }

  @Test
  @DisplayName('3-08 符号 ~ LIKE')
  void test_3_08_symbolLike() {
    def actual = assertSuccess(normalizeExpression([name: ['~': '%张%']]))
    assertEquals(expr(Ops.LIKE, field('name'), constant('%张%')), actual)
  }

  @Test
  @DisplayName('3-09 $regexp / MATCHES')
  void test_3_09_regexpMatches() {
    def actual = assertSuccess(normalizeExpression([name: ['$regexp': '^张.*']]))
    assertEquals(expr(Ops.MATCHES, field('name'), constant('^张.*')), actual)
  }

  // ── Section 4: 空值操作符 ──

  @Test
  @DisplayName('4-01 隐式 null IS_NULL')
  void test_4_01_implicitNullIsNull() {
    def actual = assertSuccess(normalizeExpression([name: null]))
    assertEquals(expr(Ops.IS_NULL, field('name')), actual)
  }

  @Test
  @DisplayName('4-02 $isNull 显式')
  void test_4_02_explicitIsNull() {
    def actual = assertSuccess(normalizeExpression([name: ['$isNull': true]]))
    assertEquals(expr(Ops.IS_NULL, field('name')), actual)
  }

  @Test
  @DisplayName('4-03 $isNotNull')
  void test_4_03_isNotNull() {
    def actual = assertSuccess(normalizeExpression([name: ['$isNotNull': true]]))
    assertEquals(expr(Ops.IS_NOT_NULL, field('name')), actual)
  }

  // ── Section 5: 逻辑操作符 ──

  @Test
  @DisplayName('5-01 $and 显式')
  void test_5_01_explicitAnd() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$and': [
        [age: ['$gte': 18]],
        [status: 'active']
      ]
    ]))
    def expected = expr(Ops.AND, gte('age', 18), eq('status', 'active'))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-02 $or')
  void test_5_02_orOperator() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$or': [[name: '张三'], [name: '李四']]
    ]))
    def expected = expr(Ops.OR, eq('name', '张三'), eq('name', '李四'))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-03 $not')
  void test_5_03_notOperator() {
    def actual = assertConditionSuccess(normalizeCondition(['$not': [status: 'deleted']]))
    assertEquals(expr(Ops.NOT, eq('status', 'deleted')), actual)
  }

  @Test
  @DisplayName('5-04 嵌套 and + or')
  void test_5_04_nestedAndOr() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$and': [
        ['$or': [[a: 1], [b: 2]]],
        [c: 3]
      ]
    ]))
    def expected = expr(Ops.AND,
      expr(Ops.OR, eq('a', 1), eq('b', 2)),
      eq('c', 3)
    )
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-05 嵌套 or + and')
  void test_5_05_nestedOrAnd() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$or': [
        ['$and': [[a: 1], [b: 2]]],
        [c: 3]
      ]
    ]))
    def expected = expr(Ops.OR,
      expr(Ops.AND, eq('a', 1), eq('b', 2)),
      eq('c', 3)
    )
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-06 三层嵌套')
  void test_5_06_threeLevelNestedLogic() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$and': [
        ['$or': [
          ['$not': [a: 1]],
          [b: 2]
        ]],
        [c: 3]
      ]
    ]))
    def expected = expr(Ops.AND,
      expr(Ops.OR, expr(Ops.NOT, eq('a', 1)), eq('b', 2)),
      eq('c', 3)
    )
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-07 无前缀 and 兼容')
  void test_5_07_compatAndWithoutPrefix() {
    def actual = assertConditionSuccess(normalizeCondition([
      and: [[age: ['>=': 18]], [status: 'active']]
    ]))
    def expected = expr(Ops.AND, gte('age', 18), eq('status', 'active'))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-08 无前缀 or 兼容')
  void test_5_08_compatOrWithoutPrefix() {
    def actual = assertConditionSuccess(normalizeCondition([
      or: [[name: '张三'], [name: '李四']]
    ]))
    def expected = expr(Ops.OR, eq('name', '张三'), eq('name', '李四'))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-09 嵌套 AND')
  void test_5_09_nestedAnd() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$and': [
        ['$and': [[a: 1], [b: 2]]],
        [c: 3]
      ]
    ]))
    def expected = expr(Ops.AND, eq('a', 1), eq('b', 2), eq('c', 3))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-10 嵌套 OR')
  void test_5_10_nestedOr() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$or': [
        ['$or': [[a: 1], [b: 2]]],
        [c: 3]
      ]
    ]))
    def expected = expr(Ops.OR, eq('a', 1), eq('b', 2), eq('c', 3))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-11 单元素 AND')
  void test_5_11_singleElementAnd() {
    def actual = assertConditionSuccess(normalizeCondition(['$and': [[a: 1]]]))
    assertEquals(eq('a', 1), actual)
  }

  @Test
  @DisplayName('5-12 空 AND')
  void test_5_12_emptyAnd() {
    def actual = assertConditionSuccess(normalizeCondition(['$and': []]))
    assertEquals(SExpression.constant(true), actual)
  }

  @Test
  @DisplayName('5-13 XOR')
  void test_5_13_xorOperator() {
    def actual = assertConditionSuccess(normalizeCondition(['$xor': [[a: 1], [b: 2]]]))
    def expected = expr(Ops.XOR, eq('a', 1), eq('b', 2))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('5-14 XNOR')
  void test_5_14_xnorOperator() {
    def actual = assertConditionSuccess(normalizeCondition(['$xnor': [[a: 1], [b: 2]]]))
    def expected = expr(Ops.XNOR, eq('a', 1), eq('b', 2))
    assertEquals(expected, actual)
  }
}
