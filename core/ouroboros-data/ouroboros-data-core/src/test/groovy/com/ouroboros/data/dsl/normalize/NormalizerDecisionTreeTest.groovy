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
class NormalizerDecisionTreeTest {

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

  // ── Section 0: Normalizer 判定树路由 ──

  @Test
  @DisplayName('0-01 Map 顶层路由')
  void test_0_01_mapRoutesToMapNormalizer() {
    def actual = assertSuccess(normalizeExpression([name: '张三']))
    assertEquals(eq('name', '张三'), actual)
  }

  @Test
  @DisplayName('0-02 List 顶层路由')
  void test_0_02_listRoutesToListNormalizer() {
    def actual = assertSuccess(normalizeExpression(['=', ['field', 'name'], '张三']))
    assertEquals(expr(Ops.EQ, field('name'), constant('张三')), actual)
  }

  @Test
  @DisplayName('0-03 基础类型顶层输入')
  void test_0_03_scalarTopLevelInput() {
    assertFailure(normalizeExpression('raw string'))
  }

  @Test
  @DisplayName('0-04 null 顶层输入')
  void test_0_04_nullTopLevelInput() {
    def actual = assertSuccess(normalizeExpression(null))
    assertTrue(actual.isEmpty())
  }

  @Test
  @DisplayName('0-05 Map key 路由 $and')
  void test_0_05_mapKeyRoutesToAnd() {
    def actual = assertConditionSuccess(normalizeCondition(['$and': [[a: 1], [b: 2]]]))
    assertEquals(expr(Ops.AND, eq('a', 1), eq('b', 2)), actual)
  }

  @Test
  @DisplayName('0-06 Map key 路由 $or')
  void test_0_06_mapKeyRoutesToOr() {
    def actual = assertConditionSuccess(normalizeCondition(['$or': [[a: 1], [b: 2]]]))
    assertEquals(expr(Ops.OR, eq('a', 1), eq('b', 2)), actual)
  }

  @Test
  @DisplayName('0-07 Map key 路由 $not')
  void test_0_07_mapKeyRoutesToNot() {
    def actual = assertConditionSuccess(normalizeCondition(['$not': [a: 1]]))
    assertEquals(expr(Ops.NOT, eq('a', 1)), actual)
  }

  @Test
  @DisplayName('0-08 Map key 路由 and 兼容')
  void test_0_08_mapKeyRoutesToCompatAnd() {
    def actual = assertConditionSuccess(normalizeCondition([and: [[a: 1], [b: 2]]]))
    assertEquals(expr(Ops.AND, eq('a', 1), eq('b', 2)), actual)
  }

  @Test
  @DisplayName('0-09 Map key 路由 or 兼容')
  void test_0_09_mapKeyRoutesToCompatOr() {
    def actual = assertConditionSuccess(normalizeCondition([or: [[a: 1], [b: 2]]]))
    assertEquals(expr(Ops.OR, eq('a', 1), eq('b', 2)), actual)
  }

  @Test
  @DisplayName('0-10 顶层 $constant 特殊操作符')
  void test_0_10_topLevelConstantOperator() {
    def actual = assertSuccess(normalizeExpression(['$constant': true]))
    assertEquals(SExpression.constant(true), actual)
  }

  @Test
  @DisplayName('0-11 普通字段路由')
  void test_0_11_plainFieldRoutesToFieldExpression() {
    def actual = assertSuccess(normalizeExpression([name: '张三']))
    assertEquals(eq('name', '张三'), actual)
  }

  @Test
  @DisplayName('0-12 未知顶层 $key')
  void test_0_12_unknownDollarTopLevelKey() {
    assertFailure(normalizeExpression(['$unknown': 'x']))
  }

  @Test
  @DisplayName('0-13 字段值 null 分派')
  void test_0_13_fieldValueNullBranch() {
    def actual = assertSuccess(normalizeExpression([name: null]))
    assertEquals(expr(Ops.IS_NULL, field('name')), actual)
  }

  @Test
  @DisplayName('0-14 字段值 String 分派')
  void test_0_14_fieldValueStringBranch() {
    def actual = assertSuccess(normalizeExpression([name: '张三']))
    assertEquals(eq('name', '张三'), actual)
  }

  @Test
  @DisplayName('0-15 字段值 Number 分派')
  void test_0_15_fieldValueNumberBranch() {
    def actual = assertSuccess(normalizeExpression([age: 25]))
    assertEquals(eq('age', 25), actual)
  }

  @Test
  @DisplayName('0-16 字段值 Boolean 分派')
  void test_0_16_fieldValueBooleanBranch() {
    def actual = assertSuccess(normalizeExpression([active: true]))
    assertEquals(eq('active', true), actual)
  }

  @Test
  @DisplayName('0-17 字段值 List 分派')
  void test_0_17_fieldValueListBranch() {
    def actual = assertSuccess(normalizeExpression([status: ['a', 'b']]))
    assertEquals(expr(Ops.IN, field('status'), constant(['a', 'b'])), actual)
  }

  @Test
  @DisplayName('0-18 字段值空 List 分派')
  void test_0_18_fieldValueEmptyListBranch() {
    def actual = assertSuccess(normalizeExpression([status: []]))
    assertEquals(expr(Ops.IN, field('status'), constant([])), actual)
  }

  @Test
  @DisplayName('0-19 字段值 operatorMap 分派')
  void test_0_19_fieldValueOperatorMapBranch() {
    def actual = assertSuccess(normalizeExpression([age: ['$gt': 18]]))
    assertEquals(expr(Ops.GT, field('age'), constant(18)), actual)
  }

  @Test
  @DisplayName('0-20 字段值 nestedMap 分派')
  void test_0_20_fieldValueNestedMapBranch() {
    def actual = assertSuccess(normalizeExpression([user: [name: '张三']]))
    assertEquals(expr(Ops.EQ, field('user', 'name'), constant('张三')), actual)
  }

  @Test
  @DisplayName('0-21 字段值不可识别类型')
  void test_0_21_fieldValueUnsupportedType() {
    assertFailure(normalizeExpression([name: new Object()]))
  }

  @Test
  @DisplayName('0-22 isOperatorMap 单个 $key')
  void test_0_22_isOperatorMapSingleDollarKey() {
    def actual = assertSuccess(normalizeExpression([age: ['$gt': 18]]))
    assertEquals(expr(Ops.GT, field('age'), constant(18)), actual)
  }

  @Test
  @DisplayName('0-23 isOperatorMap 多个 $key')
  void test_0_23_isOperatorMapMultiDollarKeys() {
    def actual = assertConditionSuccess(normalizeCondition([age: ['$gte': 18, '$lt': 60]]))
    assertEquals(expr(Ops.AND,
      expr(Ops.GOE, field('age'), constant(18)),
      expr(Ops.LT, field('age'), constant(60))
    ), actual)
  }

  @Test
  @DisplayName('0-24 isOperatorMap 无前缀操作符')
  void test_0_24_isOperatorMapPrefixlessOperator() {
    def actual = assertSuccess(normalizeExpression([age: [gt: 18]]))
    assertEquals(expr(Ops.GT, field('age'), constant(18)), actual)
  }

  @Test
  @DisplayName('0-25 isOperatorMap 符号操作符')
  void test_0_25_isOperatorMapSymbolOperator() {
    def actual = assertSuccess(normalizeExpression([age: ['>=': 18]]))
    assertEquals(expr(Ops.GOE, field('age'), constant(18)), actual)
  }

  @Test
  @DisplayName('0-26 isOperatorMap 纯字段 Map')
  void test_0_26_isOperatorMapPlainFieldMap() {
    def actual = assertSuccess(normalizeExpression([user: [name: '张三']]))
    assertEquals(expr(Ops.EQ, field('user', 'name'), constant('张三')), actual)
  }

  @Test
  @DisplayName('0-27 isOperatorMap 混合 key')
  void test_0_27_isOperatorMapMixedKeys() {
    assertFailure(normalizeCondition([user: ['$eq': '张三', badKey: 1]]))
  }

  @Test
  @DisplayName('0-28 isOperatorMap 空 Map')
  void test_0_28_isOperatorMapEmptyMap() {
    def actual = assertConditionSuccess(normalizeCondition([user: [:]]))
    assertTrue(actual.isEmpty())
  }

  @Test
  @DisplayName('0-29 normalizeOperatorKey 标准 $gt')
  void test_0_29_normalizeOperatorKeyDollarGt() {
    def actual = assertSuccess(normalizeExpression([age: ['$gt': 18]]))
    assertEquals(Ops.GT, actual.getOperator())
  }

  @Test
  @DisplayName('0-30 normalizeOperatorKey 无前缀 gt')
  void test_0_30_normalizeOperatorKeyPlainGt() {
    def actual = assertSuccess(normalizeExpression([age: [gt: 18]]))
    assertEquals(Ops.GT, actual.getOperator())
  }

  @Test
  @DisplayName('0-31 normalizeOperatorKey 符号 >=')
  void test_0_31_normalizeOperatorKeySymbolGte() {
    def actual = assertSuccess(normalizeExpression([age: ['>=': 18]]))
    assertEquals(Ops.GOE, actual.getOperator())
  }

  @Test
  @DisplayName('0-32 normalizeOperatorKey 符号 !=')
  void test_0_32_normalizeOperatorKeySymbolNe() {
    def actual = assertSuccess(normalizeExpression([status: ['!=': 'deleted']]))
    assertEquals(Ops.NE, actual.getOperator())
  }

  @Test
  @DisplayName('0-33 normalizeOperatorKey camelCase 长名')
  void test_0_33_normalizeOperatorKeyCamelCaseAlias() {
    def actual = assertSuccess(normalizeExpression([age: [greaterThan: 18]]))
    assertEquals(Ops.GT, actual.getOperator())
  }

  @Test
  @DisplayName('0-34 normalizeOperatorKey UPPER_CASE 长名')
  void test_0_34_normalizeOperatorKeyUpperCaseAlias() {
    def actual = assertSuccess(normalizeExpression([age: [GREATER_THAN: 18]]))
    assertEquals(Ops.GT, actual.getOperator())
  }

  @Test
  @DisplayName('0-35 normalizeOperatorKey GOE 缩写')
  void test_0_35_normalizeOperatorKeyAbbrGoe() {
    def actual = assertSuccess(normalizeExpression([age: [GOE: 18]]))
    assertEquals(Ops.GOE, actual.getOperator())
  }

  @Test
  @DisplayName('0-36 normalizeOperatorKey 符号 ~')
  void test_0_36_normalizeOperatorKeySymbolLike() {
    def actual = assertSuccess(normalizeExpression([name: ['~': '%张%']]))
    assertEquals(Ops.LIKE, actual.getOperator())
  }

  @Test
  @DisplayName('0-37 normalizeOperatorKey 不存在别名')
  void test_0_37_normalizeOperatorKeyUnknownAlias() {
    def actual = assertSuccess(normalizeExpression([name: ['$nonExist': 'x']]))
    def expected = expr(Ops.EQ, field('name', '$nonExist'), constant('x'))
    assertEquals(expected, actual)
  }

  @Test
  @DisplayName('0-38 List 首元素符号操作符')
  void test_0_38_listFirstSymbolOperator() {
    def actual = assertSuccess(normalizeExpression(['=', ['field', 'name'], '张三']))
    assertEquals(expr(Ops.EQ, field('name'), constant('张三')), actual)
  }

  @Test
  @DisplayName('0-39 List 首元素字母操作符')
  void test_0_39_listFirstWordOperator() {
    def actual = assertConditionSuccess(normalizeCondition(['and', [a: 1], [b: 2]]))
    assertEquals(expr(Ops.AND, eq('a', 1), eq('b', 2)), actual)
  }

  @Test
  @DisplayName('0-40 List 首元素为 List')
  void test_0_40_listFirstElementIsList() {
    def actual = assertSuccess(normalizeExpression([
      ['=', ['field', 'a'], 1],
      ['=', ['field', 'b'], 2]
    ]))
    assertEquals(expr(Ops.AND,
      expr(Ops.EQ, field('a'), constant(1)),
      expr(Ops.EQ, field('b'), constant(2))
    ), actual)
  }

  @Test
  @DisplayName('0-41 List 首元素为 Map')
  void test_0_41_listFirstElementIsMap() {
    def actual = assertSuccess(normalizeExpression([[a: 1], [b: 2]]))
    assertEquals(expr(Ops.AND, eq('a', 1), eq('b', 2)), actual)
  }

  @Test
  @DisplayName('0-42 List 首元素为 Operator 对象')
  void test_0_42_listFirstElementIsOperatorObject() {
    def actual = assertSuccess(normalizeExpression([Ops.EQ, ['field', 'name'], '张三']))
    assertEquals(expr(Ops.EQ, field('name'), constant('张三')), actual)
  }

  @Test
  @DisplayName('0-43 List 首元素不可识别类型')
  void test_0_43_listFirstElementUnsupportedType() {
    assertFailure(normalizeExpression([123, [a: 1]]))
  }

  @Test
  @DisplayName('0-44 逻辑优化同层 AND 嵌套')
  void test_0_44_logicOptimizationAndFlatten() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$and': [
        ['$and': [[a: 1], [b: 2]]],
        [c: 3]
      ]
    ]))
    assertEquals(expr(Ops.AND, eq('a', 1), eq('b', 2), eq('c', 3)), actual)
  }

  @Test
  @DisplayName('0-45 逻辑优化同层 OR 嵌套')
  void test_0_45_logicOptimizationOrFlatten() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$or': [
        ['$or': [[a: 1], [b: 2]]],
        [c: 3]
      ]
    ]))
    assertEquals(expr(Ops.OR, eq('a', 1), eq('b', 2), eq('c', 3)), actual)
  }

  @Test
  @DisplayName('0-46 逻辑优化空条件列表')
  void test_0_46_logicOptimizationEmptyAnd() {
    def actual = assertConditionSuccess(normalizeCondition(['$and': []]))
    assertEquals(SExpression.constant(true), actual)
  }

  @Test
  @DisplayName('0-47 逻辑优化单元素退化')
  void test_0_47_logicOptimizationSingleElement() {
    def actual = assertConditionSuccess(normalizeCondition(['$and': [[a: 1]]]))
    assertEquals(eq('a', 1), actual)
  }

  @Test
  @DisplayName('0-48 逻辑优化 OR 含 TRUE 短路')
  void test_0_48_logicOptimizationOrShortCircuit() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$or': [
        ['$constant': true],
        [a: 1]
      ]
    ]))
    assertEquals(SExpression.constant(true), actual)
  }

  @Test
  @DisplayName('0-49 逻辑优化 AND 含 FALSE 短路')
  void test_0_49_logicOptimizationAndShortCircuit() {
    def actual = assertConditionSuccess(normalizeCondition([
      '$and': [
        ['$constant': false],
        [a: 1]
      ]
    ]))
    assertEquals(SExpression.constant(false), actual)
  }

  @Test
  @DisplayName('0-50 逻辑优化不同操作符不展平')
  void test_0_50_logicOptimizationKeepsDifferentOperators() {
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
  @DisplayName('0-51 DollarPrefix 单 $key')
  void test_0_51_dollarPrefixSingleKey() {
    def actual = assertSuccess(normalizeExpression([age: ['$gt': 18]]))
    assertEquals(Ops.GT, actual.getOperator())
  }

  @Test
  @DisplayName('0-52 DollarPrefix 多 $key')
  void test_0_52_dollarPrefixMultiKeys() {
    def actual = assertConditionSuccess(normalizeCondition([age: ['$gte': 18, '$lt': 60]]))
    assertEquals(expr(Ops.AND,
      expr(Ops.GOE, field('age'), constant(18)),
      expr(Ops.LT, field('age'), constant(60))
    ), actual)
  }

  @Test
  @DisplayName('0-53 DollarPrefix 回退到别名路径')
  void test_0_53_dollarPrefixFallbackToAlias() {
    def actual = assertSuccess(normalizeExpression([name: ['$notLike': '%x%']]))
    assertEquals(ExtOps.NOT_LIKE, actual.getOperator())
  }

  @Test
  @DisplayName('0-54 DollarPrefix $sum 标量值会生成聚合比较')
  void test_0_54_dollarPrefixSumOperator() {
    def actual = assertSuccess(normalizeExpression([amount: ['$sum': 'amount']]))
    assertEquals(expr(Ops.EQ,
      expr(Ops.AggOps.SUM_AGG, field('amount')),
      constant('amount')
    ), actual)
  }

  @Test
  @DisplayName('0-55 DollarPrefix 混合 $key 与普通 key')
  void test_0_55_dollarPrefixMixedDollarAndPlainKeys() {
    assertFailure(normalizeCondition([age: ['$gt': 18, name: '张三']]))
  }
}
