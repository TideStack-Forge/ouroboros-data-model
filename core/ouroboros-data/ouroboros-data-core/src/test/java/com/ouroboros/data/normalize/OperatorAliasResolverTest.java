package com.ouroboros.data.normalize;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.exception.NormalizeException;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;

/**
 * 操作符别名解析器测试
 *
 * <p>验证 {@link OperatorAliasResolver} 能够正确解析各种格式的操作符别名。
 * <p>注意：$ 前缀处理已收口到
 * {@code com.ouroboros.data.normalize.expressionnormalizers.MapExpressionNormalizer}，
 * OperatorAliasResolver 不再处理 $ 前缀。调用方需在调用前自行去除。
 *
 * @since 1.0.0-beta.2
 */
class OperatorAliasResolverTest {

  // ==================== 无前缀格式测试 ====================

  @Test
  void testResolve_Lowercase() {
    Operator result = OperatorAliasResolver.resolveOperator("gt");
    assertEquals(Ops.GT, result);
  }

  @Test
  void testResolve_Uppercase() {
    Operator result = OperatorAliasResolver.resolveOperator("GT");
    assertEquals(Ops.GT, result);
  }

  @Test
  void testResolve_MixedCase() {
    Operator result = OperatorAliasResolver.resolveOperator("Gt");
    assertEquals(Ops.GT, result);
  }

  // ==================== $ 前缀不再被解析（设计变更验证） ====================

  @Test
  void testResolve_DollarPrefixNoLongerResolved() {
    // $ 前缀现在只由 expressionnormalizers 包下的 MapExpressionNormalizer 负责去除
    Optional<Operator> result = OperatorAliasResolver.tryResolveOperator("$gt");
    assertFalse(result.isPresent(), "$ 前缀不应被 OperatorAliasResolver 解析");
  }

  // ==================== 配置文件别名测试 ====================

  @Test
  void testResolve_ConfigSymbolAliases() {
    assertEquals(Ops.GT, OperatorAliasResolver.resolveOperator(">"));
    assertEquals(Ops.GOE, OperatorAliasResolver.resolveOperator(">="));
    assertEquals(Ops.LT, OperatorAliasResolver.resolveOperator("<"));
    assertEquals(Ops.LIKE, OperatorAliasResolver.resolveOperator("~"));
  }

  @Test
  void testResolve_ConfigEnglishAliases() {
    assertEquals(Ops.GT, OperatorAliasResolver.resolveOperator("greaterThan"));
    assertEquals(Ops.GT, OperatorAliasResolver.resolveOperator("GREATER_THAN"));
    assertEquals(Ops.GOE, OperatorAliasResolver.resolveOperator("greaterThanOrEqual"));
    assertEquals(Ops.GOE, OperatorAliasResolver.resolveOperator("GREATER_THAN_OR_EQUAL"));
    assertEquals(Ops.STARTS_WITH, OperatorAliasResolver.resolveOperator("STARTS_WITH"));
    assertEquals(Ops.STARTS_WITH, OperatorAliasResolver.resolveOperator("startsWith"));
  }

  // ==================== 逻辑操作符测试 ====================

  @Test
  void testResolve_LogicOperators() {
    assertEquals(Ops.AND, OperatorAliasResolver.resolveOperator("AND"));
    assertEquals(Ops.AND, OperatorAliasResolver.resolveOperator("&&"));
    assertEquals(Ops.OR, OperatorAliasResolver.resolveOperator("OR"));
    assertEquals(Ops.OR, OperatorAliasResolver.resolveOperator("||"));
    assertEquals(Ops.NOT, OperatorAliasResolver.resolveOperator("NOT"));
    assertEquals(Ops.NOT, OperatorAliasResolver.resolveOperator("!"));
  }

  // ==================== 比较操作符测试 ====================

  @Test
  void testResolve_ComparisonOperators() {
    assertEquals(Ops.EQ, OperatorAliasResolver.resolveOperator("EQ"));
    assertEquals(Ops.EQ, OperatorAliasResolver.resolveOperator("="));
    assertEquals(Ops.NE, OperatorAliasResolver.resolveOperator("NE"));
    assertEquals(Ops.LT, OperatorAliasResolver.resolveOperator("LT"));
    assertEquals(Ops.LT, OperatorAliasResolver.resolveOperator("<"));
    assertEquals(Ops.LOE, OperatorAliasResolver.resolveOperator("LOE"));
    assertEquals(Ops.LOE, OperatorAliasResolver.resolveOperator("<="));
    assertEquals(Ops.GT, OperatorAliasResolver.resolveOperator("GT"));
    assertEquals(Ops.GT, OperatorAliasResolver.resolveOperator(">"));
    assertEquals(Ops.GOE, OperatorAliasResolver.resolveOperator("GOE"));
    assertEquals(Ops.GOE, OperatorAliasResolver.resolveOperator("GTE"));
    assertEquals(Ops.GOE, OperatorAliasResolver.resolveOperator(">="));
  }

  // ==================== 集合操作符测试 ====================

  @Test
  void testResolve_CollectionOperators() {
    assertEquals(Ops.IN, OperatorAliasResolver.resolveOperator("IN"));
    assertEquals(Ops.NOT_IN, OperatorAliasResolver.resolveOperator("NOT_IN"));
    assertEquals(Ops.NOT_IN, OperatorAliasResolver.resolveOperator("notIn"));
  }

  // ==================== 字符串操作符测试 ====================

  @Test
  void testResolve_StringOperators() {
    assertEquals(Ops.LIKE, OperatorAliasResolver.resolveOperator("LIKE"));
    assertEquals(Ops.LIKE, OperatorAliasResolver.resolveOperator("CONTAINS"));
    assertEquals(Ops.LIKE, OperatorAliasResolver.resolveOperator("~"));
    assertEquals(Ops.STARTS_WITH, OperatorAliasResolver.resolveOperator("STARTS_WITH"));
    assertEquals(Ops.ENDS_WITH, OperatorAliasResolver.resolveOperator("ENDS_WITH"));
  }

  // ==================== BETWEEN 操作符测试 ====================

  @Test
  void testResolve_BetweenOperator() {
    assertEquals(Ops.BETWEEN, OperatorAliasResolver.resolveOperator("BETWEEN"));
    assertEquals(Ops.BETWEEN, OperatorAliasResolver.resolveOperator("between"));
  }

  // ==================== NULL 检查操作符测试 ====================

  @Test
  void testResolve_NullOperators() {
    assertEquals(Ops.IS_NULL, OperatorAliasResolver.resolveOperator("IS_NULL"));
    assertEquals(Ops.IS_NULL, OperatorAliasResolver.resolveOperator("ISNULL"));
    assertEquals(Ops.IS_NOT_NULL, OperatorAliasResolver.resolveOperator("IS_NOT_NULL"));
    assertEquals(Ops.IS_NOT_NULL, OperatorAliasResolver.resolveOperator("ISNOTNULL"));
  }

  // ==================== 错误处理测试 ====================

  @Test
  void testResolve_UnknownOperator_Throws() {
    NormalizeException exception = assertThrows(
        NormalizeException.class,
        () -> OperatorAliasResolver.resolveOperator("UNKNOWN_OP")
    );
    assertTrue(exception.getMessage().contains("未知的操作符"));
  }

  @Test
  void testTryResolve_UnknownOperator_Empty() {
    Optional<Operator> result1 = OperatorAliasResolver.tryResolveOperator("UNKNOWN_OP");
    assertFalse(result1.isPresent());

    // $ prefix is now treated as unknown (by design)
    Optional<Operator> result2 = OperatorAliasResolver.tryResolveOperator("$unknown");
    assertFalse(result2.isPresent());
  }

  // ==================== 缓存性能测试 ====================

  @Test
  void testCache_Performance() {
    String[] aliases = {"GT", "gt", ">", "EQ", "eq", "=", "AND", "and", "&&"};

    // 预热
    for (String alias : aliases) {
      OperatorAliasResolver.resolveOperator(alias);
    }

    long startTime1 = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
      for (String alias : aliases) {
        OperatorAliasResolver.resolveOperator(alias);
      }
    }
    long duration1 = System.nanoTime() - startTime1;

    long startTime2 = System.nanoTime();
    for (int i = 0; i < 10000; i++) {
      for (String alias : aliases) {
        OperatorAliasResolver.resolveOperator(alias);
      }
    }
    long duration2 = System.nanoTime() - startTime2;

    assertTrue(duration2 <= duration1 * 3,
        String.format("缓存性能测试: 第一次=%dms, 第二次=%dms",
            duration1 / 1_000_000, duration2 / 1_000_000));
  }

  @Test
  void testCache_Consistency() {
    Operator op1 = OperatorAliasResolver.resolveOperator("gt");
    Operator op2 = OperatorAliasResolver.resolveOperator("gt");
    Operator op3 = OperatorAliasResolver.resolveOperator("GT");
    Operator op4 = OperatorAliasResolver.resolveOperator(">");

    assertSame(op1, op2, "gt 的两次查询应返回相同实例");
    assertSame(op1, op3, "gt 和 GT 应返回相同实例");
    assertSame(op1, op4, "gt 和 > 应返回相同实例");
  }
}
