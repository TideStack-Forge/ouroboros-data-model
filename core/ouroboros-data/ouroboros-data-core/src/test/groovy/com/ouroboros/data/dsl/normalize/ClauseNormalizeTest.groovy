package com.ouroboros.data.dsl.normalize

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue

import java.math.BigDecimal

import com.ouroboros.data.dsl.ExtOps
import com.ouroboros.data.dsl.SExpression
import com.ouroboros.data.dsl.statement.QueryStatement
import com.ouroboros.data.normalize.QueryNormalizeContext
import com.ouroboros.data.dsl.Order
import com.querydsl.core.types.Operator
import com.querydsl.core.types.Ops
import io.vavr.control.Try
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClauseNormalizeTest {

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

  private void assertQueryFailure(Try<QueryStatement> result) {
    assertFalse(result.isSuccess())
  }

  private SExpression<?> assertColumnsSelect(QueryStatement statement) {
    assertFalse(statement.getSelect().isEmpty())
    def selectExpr = statement.getSelect().get(0)
    assertEquals(ExtOps.COLUMNS, selectExpr.getOperator())
    selectExpr
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

  private static SExpression<?> alias(SExpression<?> expression, String alias) {
    SExpression.alias(expression, alias)
  }

  // ── Section 9: SELECT 子句 ──

  @Test
  @DisplayName('9-01 字段列表 SELECT')
  void test_9_01_selectFieldList() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': ['id', 'name', 'age'],
      'FROM'  : 'User'
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals(3, columns.getParams().size())
    assertEquals(field('id'), columns.getParamAsSExpression(0))
    assertEquals(field('name'), columns.getParamAsSExpression(1))
    assertEquals(field('age'), columns.getParamAsSExpression(2))
    assertEquals('User', stmt.getFrom().getTableName())
  }

  @Test
  @DisplayName('9-02 别名映射 SELECT')
  void test_9_02_selectAliasMapping() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': ['id', ['userName': 'name']],
      'FROM'  : 'User'
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals(2, columns.getParams().size())
    assertEquals(field('id'), columns.getParamAsSExpression(0))
    assertEquals(alias(field('name'), 'userName'), columns.getParamAsSExpression(1))
  }

  @Test
  @DisplayName('9-03 省略 SELECT 默认 *')
  void test_9_03_selectDefaultAll() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'WHERE': ['id': 1]
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals('*', columns.getParam(0))
  }

  @Test
  @DisplayName('9-04 SELECT 中聚合表达式')
  void test_9_04_selectAggregation() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': [['total': ['count', ['*']]]],
      'FROM'  : 'User'
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals(1, columns.getParams().size())
    assertEquals(alias(expr(Ops.AggOps.COUNT_AGG, field('*')), 'total'),
      columns.getParamAsSExpression(0))
  }

  @Test
  @DisplayName('9-05 混合字段与聚合 SELECT')
  void test_9_05_selectFieldAndAggregation() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': ['name', ['cnt': ['count', ['*']]]],
      'FROM'  : 'User'
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals(2, columns.getParams().size())
    assertEquals(field('name'), columns.getParamAsSExpression(0))
    assertEquals(alias(expr(Ops.AggOps.COUNT_AGG, field('*')), 'cnt'),
      columns.getParamAsSExpression(1))
  }

  @Test
  @DisplayName('9-06 SELECT 关联字段点号路径')
  void test_9_06_selectRelationDotPath() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': ['id', ['userName': 'user.name']],
      'FROM'  : 'Order'
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals(2, columns.getParams().size())
    assertEquals(field('id'), columns.getParamAsSExpression(0))
    assertEquals(alias(field('user', 'name'), 'userName'), columns.getParamAsSExpression(1))
  }

  @Test
  @DisplayName('9-07 字符串 SELECT undocumented')
  void test_9_07_selectStringUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': 'id, name, age',
      'FROM'  : 'User'
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals(3, columns.getParams().size())
    assertEquals(field('id'), columns.getParamAsSExpression(0))
    assertEquals(field('name'), columns.getParamAsSExpression(1))
    assertEquals(field('age'), columns.getParamAsSExpression(2))
  }

  @Test
  @DisplayName('9-08 字符串 SELECT + AS undocumented')
  void test_9_08_selectStringAsUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': 'id, name as userName, age',
      'FROM'  : 'User'
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals(3, columns.getParams().size())
    assertEquals(field('id'), columns.getParamAsSExpression(0))
    assertEquals(alias(field('name'), 'userName'), columns.getParamAsSExpression(1))
    assertEquals(field('age'), columns.getParamAsSExpression(2))
  }

  @Test
  @DisplayName('9-09 Map 形式 SELECT')
  void test_9_09_selectMapForm() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': ['userName': 'name', 'userAge': 'age'],
      'FROM'  : 'User'
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals(2, columns.getParams().size())
    assertEquals(alias(field('name'), 'userName'), columns.getParamAsSExpression(0))
    assertEquals(alias(field('age'), 'userAge'), columns.getParamAsSExpression(1))
  }

  @Test
  @DisplayName('9-10 SELECT 数字常量')
  void test_9_10_selectNumericConstant() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': ['1', '2.5'],
      'FROM'  : 'User'
    ]))

    def columns = assertColumnsSelect(stmt)
    assertEquals(2, columns.getParams().size())
    assertEquals(constant(1), columns.getParamAsSExpression(0))
    assertEquals(constant(new BigDecimal('2.5')), columns.getParamAsSExpression(1))
  }

  // ── Section 10: FROM 子句 ──

  @Test
  @DisplayName('10-01 FROM 单表字符串')
  void test_10_01_fromSingleTable() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM': 'User'
    ]))

    assertNotNull(stmt.getFrom())
    assertEquals('User', stmt.getFrom().getTableName())
  }

  @Test
  @DisplayName('10-02 DataModel 自动注入 FROM（normalize 不支持）')
  void test_10_02_fromAutoInjectByDataModel() {
    def result = normalizeQuery([
      'SELECT': ['id', 'name'],
      'WHERE' : ['id': 1]
    ])

    assertQueryFailure(result)
  }

  // ── Section 11: ORDER 子句 ──

  @Test
  @DisplayName('11-01 ORDER 单字段 Map')
  void test_11_01_orderSingleFieldMap() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'ORDER': [['name': 'asc']]
    ]))

    assertEquals(1, stmt.getOrders().size())
    assertEquals('name', stmt.getOrders().get(0).getColumn())
    assertEquals(Order.ASC, stmt.getOrders().get(0).getOrder())
  }

  @Test
  @DisplayName('11-02 ORDER 多字段 Map')
  void test_11_02_orderMultiFieldMap() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'ORDER': [['createdAt': 'desc'], ['name': 'asc']]
    ]))

    assertEquals(2, stmt.getOrders().size())
    assertEquals('createdAt', stmt.getOrders().get(0).getColumn())
    assertEquals(Order.DESC, stmt.getOrders().get(0).getOrder())
    assertEquals('name', stmt.getOrders().get(1).getColumn())
    assertEquals(Order.ASC, stmt.getOrders().get(1).getOrder())
  }

  @Test
  @DisplayName('11-03 ORDER 字符串 undocumented')
  void test_11_03_orderStringUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'ORDER': 'name asc, createdAt desc'
    ]))

    assertEquals(2, stmt.getOrders().size())
    assertEquals('name', stmt.getOrders().get(0).getColumn())
    assertEquals(Order.ASC, stmt.getOrders().get(0).getOrder())
    assertEquals('createdAt', stmt.getOrders().get(1).getColumn())
    assertEquals(Order.DESC, stmt.getOrders().get(1).getOrder())
  }

  @Test
  @DisplayName('11-04 ORDER List 混合 undocumented')
  void test_11_04_orderMixedListUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'ORDER': [['name': 'asc'], 'createdAt desc']
    ]))

    assertEquals(2, stmt.getOrders().size())
    assertEquals('name', stmt.getOrders().get(0).getColumn())
    assertEquals('createdAt', stmt.getOrders().get(1).getColumn())
  }

  @Test
  @DisplayName('11-05 ORDER Map 整体形式 undocumented')
  void test_11_05_orderWholeMapUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'ORDER': ['name': 'asc', 'createdAt': 'desc']
    ]))

    assertEquals(2, stmt.getOrders().size())
    assertEquals('name', stmt.getOrders().get(0).getColumn())
    assertEquals('createdAt', stmt.getOrders().get(1).getColumn())
  }

  @Test
  @DisplayName('11-06 ORDER 默认 ASC')
  void test_11_06_orderDefaultAsc() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'ORDER': ['name']
    ]))

    assertEquals(1, stmt.getOrders().size())
    assertEquals('name', stmt.getOrders().get(0).getColumn())
    assertEquals(Order.ASC, stmt.getOrders().get(0).getOrder())
  }

  // ── Section 12: 分页 ──

  @Test
  @DisplayName('12-01 limit + offset')
  void test_12_01_limitAndOffset() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'  : 'User',
      'LIMIT' : 10,
      'OFFSET': 20
    ]))

    assertEquals(10L, stmt.getLimit())
    assertEquals(20L, stmt.getOffset())
  }

  @Test
  @DisplayName('12-02 仅 limit')
  void test_12_02_limitOnly() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'LIMIT': 10
    ]))

    assertEquals(10L, stmt.getLimit())
    assertNull(stmt.getOffset())
  }

  @Test
  @DisplayName('12-03 skip + limit undocumented')
  void test_12_03_skipAndLimitUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'skip' : 20,
      'LIMIT': 10
    ]))

    assertEquals(10L, stmt.getLimit())
    assertEquals(20L, stmt.getOffset())
  }

  @Test
  @DisplayName('12-04 page + pageSize undocumented')
  void test_12_04_pageAndPageSizeUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'    : 'User',
      'PAGE'    : 2,
      'PAGESIZE': 20
    ]))

    assertEquals(20L, stmt.getLimit())
    assertEquals(20L, stmt.getOffset())
  }

  @Test
  @DisplayName('12-05 pageNum + perPage undocumented')
  void test_12_05_pageNumAndPerPageUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'   : 'User',
      'PAGENUM': 3,
      'PERPAGE': 10
    ]))

    assertEquals(10L, stmt.getLimit())
    assertEquals(20L, stmt.getOffset())
  }

  @Test
  @DisplayName('12-06 无分页')
  void test_12_06_withoutPagination() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM': 'User'
    ]))

    assertNull(stmt.getLimit())
    assertNull(stmt.getOffset())
  }

  // ── Section 13: GROUP BY + HAVING ──

  @Test
  @DisplayName('13-01 单字段 GROUP BY')
  void test_13_01_groupBySingleField() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT': ['status', ['cnt': ['count', ['*']]]],
      'FROM'  : 'User',
      'GROUP' : ['status']
    ]))

    assertEquals(ExtOps.COLUMNS, stmt.getGroup().getOperator())
    assertEquals(1, stmt.getGroup().getParams().size())
    assertEquals(field('status'), stmt.getGroup().getParamAsSExpression(0))
  }

  @Test
  @DisplayName('13-02 多字段 GROUP BY')
  void test_13_02_groupByMultiFields() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'GROUP': ['status', 'departmentId']
    ]))

    assertEquals(2, stmt.getGroup().getParams().size())
    assertEquals(field('status'), stmt.getGroup().getParamAsSExpression(0))
    assertEquals(field('departmentId'), stmt.getGroup().getParamAsSExpression(1))
  }

  @Test
  @DisplayName('13-03 字符串 GROUP BY undocumented')
  void test_13_03_groupByStringUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM' : 'User',
      'GROUP': 'status, departmentId'
    ]))

    assertEquals(2, stmt.getGroup().getParams().size())
    assertEquals(field('status'), stmt.getGroup().getParamAsSExpression(0))
    assertEquals(field('departmentId'), stmt.getGroup().getParamAsSExpression(1))
  }

  @Test
  @DisplayName('13-04 groupBy 别名 undocumented')
  void test_13_04_groupByAliasUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'   : 'User',
      'GROUPBY': ['status']
    ]))

    assertEquals(1, stmt.getGroup().getParams().size())
    assertEquals(field('status'), stmt.getGroup().getParamAsSExpression(0))
  }

  @Test
  @DisplayName('13-05 group_by 别名 undocumented')
  void test_13_05_groupBySnakeAliasUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'    : 'User',
      'GROUP_BY': ['status']
    ]))

    assertEquals(1, stmt.getGroup().getParams().size())
    assertEquals(field('status'), stmt.getGroup().getParamAsSExpression(0))
  }

  @Test
  @DisplayName('13-06 HAVING 聚合条件')
  void test_13_06_havingAggregationCondition() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'  : 'User',
      'GROUP' : ['status'],
      'HAVING': ['>', ['count', ['*']], 5]
    ]))

    def expected = expr(Ops.GT, expr(Ops.AggOps.COUNT_AGG, field('*')), constant(5))
    assertEquals(expected, stmt.getHaving())
  }

  @Test
  @DisplayName('13-07 HAVING 多条件')
  void test_13_07_havingMultiCondition() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'  : 'User',
      'GROUP' : ['status'],
      'HAVING': ['and', ['>', ['count', ['*']], 5], ['>', ['sum', ['amount']], 1000]]
    ]))

    def expected = expr(Ops.AND,
      expr(Ops.GT, expr(Ops.AggOps.COUNT_AGG, field('*')), constant(5)),
      expr(Ops.GT, expr(Ops.AggOps.SUM_AGG, field('amount')), constant(1000))
    )
    assertEquals(expected, stmt.getHaving())
  }

  @Test
  @DisplayName('13-08 HAVING 无 GROUP')
  void test_13_08_havingWithoutGroup() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'  : 'User',
      'HAVING': ['>', ['count', ['*']], 5]
    ]))

    def expected = expr(Ops.GT, expr(Ops.AggOps.COUNT_AGG, field('*')), constant(5))
    assertEquals(expected, stmt.getHaving())
  }

  // ── Section 14: DISTINCT ──

  @Test
  @DisplayName('14-01 DISTINCT true')
  void test_14_01_distinctBooleanTrue() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'SELECT'  : ['name'],
      'FROM'    : 'User',
      'DISTINCT': true
    ]))

    assertTrue(stmt.getDistinct())
  }

  @Test
  @DisplayName('14-02 DISTINCT false')
  void test_14_02_distinctBooleanFalse() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'    : 'User',
      'DISTINCT': false
    ]))

    assertFalse(stmt.getDistinct())
  }

  @Test
  @DisplayName('14-03 DISTINCT Number 非零 undocumented')
  void test_14_03_distinctNonZeroNumberUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'    : 'User',
      'DISTINCT': 1
    ]))

    assertTrue(stmt.getDistinct())
  }

  @Test
  @DisplayName('14-04 DISTINCT 字符串 true undocumented')
  void test_14_04_distinctStringTrueUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'    : 'User',
      'DISTINCT': 'true'
    ]))

    assertTrue(stmt.getDistinct())
  }

  @Test
  @DisplayName('14-05 DISTINCT 字符串 1 undocumented')
  void test_14_05_distinctStringOneUndocumented() {
    def stmt = assertQuerySuccess(normalizeQuery([
      'FROM'    : 'User',
      'DISTINCT': '1'
    ]))

    assertTrue(stmt.getDistinct())
  }
}
