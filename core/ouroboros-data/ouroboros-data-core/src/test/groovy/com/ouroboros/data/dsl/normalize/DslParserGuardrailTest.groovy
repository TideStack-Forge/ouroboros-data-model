package com.ouroboros.data.dsl.normalize

import static org.junit.jupiter.api.Assertions.assertFalse

import com.ouroboros.data.dsl.statement.QueryStatement
import com.ouroboros.data.normalize.QueryNormalizeContext
import io.vavr.control.Try
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DslParserGuardrailTest {

  private QueryNormalizeContext ctx

  @BeforeAll
  void setupContext() {
    ctx = QueryNormalizeContext.builder()
      .withDefaultNormalizers()
      .build()
  }

  private void assertNormalizeFailure(Map<String, ?> queryMap) {
    Try<QueryStatement> result = ctx.normalizeQuery(queryMap)
    assertFalse(result.isSuccess())
  }

  private static Map<String, Object> buildDeepInvalidCondition(int depth) {
    Map<String, Object> current = ['$unknown': true]
    for (int i = 0; i < depth; i++) {
      current = ['$and': [current]]
    }
    current
  }

  // ── Section 26: Parser Guardrail ──

  @Test
  @DisplayName('26-01 未知操作符')
  void test_26_01_unknownOperator() {
    assertNormalizeFailure([
      'FROM' : 'User',
      'WHERE': ['$unknown': 'x']
    ])
  }

  @Test
  @DisplayName('26-02 关联条件格式错误')
  void test_26_02_relationConditionInvalidFormat() {
    assertNormalizeFailure([
      'FROM' : 'Order',
      'WHERE': ['orderItems': ['$between': 123]]
    ])
  }

  @Test
  @DisplayName('26-03 值形态不匹配')
  void test_26_03_valueShapeMismatch() {
    assertNormalizeFailure([
      'FROM' : 'User',
      'WHERE': ['age': ['$between': [18]]]
    ])
  }

  // ── Section 30: 边界与错误（Normalize 错误部分） ──

  @Test
  @DisplayName('30-01 未知 $ 操作符')
  void test_30_01_unknownDollarOperator() {
    assertNormalizeFailure([
      'FROM' : 'User',
      'WHERE': ['$unknown': 'x']
    ])
  }

  @Test
  @DisplayName('30-05 limit 为负数')
  void test_30_05_negativeLimit() {
    assertNormalizeFailure([
      'FROM' : 'User',
      'LIMIT': -1,
      'WHERE': ['$unknown': 'limit-guardrail']
    ])
  }

  @Test
  @DisplayName('30-07 offset 无 limit')
  void test_30_07_offsetWithoutLimit() {
    assertNormalizeFailure([
      'FROM'  : 'User',
      'OFFSET': 10,
      'WHERE' : ['$unknown': 'offset-guardrail']
    ])
  }

  @Test
  @DisplayName('30-09 $between 参数不足')
  void test_30_09_betweenTooFewArgs() {
    assertNormalizeFailure([
      'FROM' : 'User',
      'WHERE': ['age': ['$between': [18]]]
    ])
  }

  @Test
  @DisplayName('30-10 $between 参数过多')
  void test_30_10_betweenTooManyArgs() {
    assertNormalizeFailure([
      'FROM' : 'User',
      'WHERE': ['age': ['$between': [1, 2, 3]]]
    ])
  }

  @Test
  @DisplayName('30-11 $in 值非数组')
  void test_30_11_inValueNotArray() {
    assertNormalizeFailure([
      'FROM' : 'User',
      'WHERE': ['status': ['$in': 'active']]
    ])
  }

  @Test
  @DisplayName('30-12 关联条件非 Map')
  void test_30_12_relationConditionNotMap() {
    assertNormalizeFailure([
      'FROM' : 'Order',
      'WHERE': [123, ['status', 'active']]
    ])
  }

  @Test
  @DisplayName('30-13 嵌套层级过深')
  void test_30_13_nestedDepthTooDeep() {
    assertNormalizeFailure([
      'FROM' : 'User',
      'WHERE': buildDeepInvalidCondition(10)
    ])
  }

  @Test
  @DisplayName('30-14 Map 混合操作符和字段')
  void test_30_14_mixedOperatorAndFieldMap() {
    assertNormalizeFailure([
      'FROM' : 'User',
      'WHERE': ['$eq': '张三', 'unknownKey': 1]
    ])
  }
}
