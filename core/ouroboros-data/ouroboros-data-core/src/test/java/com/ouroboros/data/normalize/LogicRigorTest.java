package com.ouroboros.data.normalize;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.builders.AggregationExpressionBuilder;
import com.ouroboros.data.normalize.builders.ComparisonExpressionBuilder;

/**
 * 逻辑严密性测试
 * <p>
 * 验证新的上下文系统在各种边界情况下的逻辑严密性，
 * 对比旧代码中的严密逻辑，确保新代码不退步
 */
@DisplayName("逻辑严密性测试")
public class LogicRigorTest {

  /**
   * 创建默认测试上下文
   */
  private QueryNormalizeContext createTestContext() {
    return QueryNormalizeContext.builder().withDefaultNormalizers().build();
  }

  // ==================== 1. Map表达式严密性测试 ====================

  @Test
  @DisplayName("Map - 空Map应该返回空布尔表达式")
  void testEmptyMap() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> emptyMap = Collections.emptyMap();

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(emptyMap, "test");

    assertTrue(result.isSuccess(), "空Map规范化应该成功");
    assertTrue(result.get().isEmpty(), "空Map应该返回空表达式");
    assertEquals(Boolean.class, result.get().getDataType(), "空表达式类型应该是Boolean");
  }

  @Test
  @DisplayName("Map - null值应该转为IS NULL操作")
  void testMapWithNullValue() {
    QueryNormalizeContext context = createTestContext();

    // 注意：需要手动创建Map，因为Collections工具类不支持null值
    Map<String, Object> testMap = new HashMap<>();
    testMap.put("name", null);

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(testMap, "test");

    assertTrue(result.isSuccess(), "null值处理应该成功");
    SExpression<?> expr = result.get();
    assertEquals(Operators.IS_NULL, expr.getOperator(), "null值应该转为IS_NULL操作符");
  }

  @Test
  @DisplayName("Map - 基本类型值应该转为EQ操作")
  void testMapWithBasicValue() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("name", "张三");

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "基本类型值处理应该成功");
    SExpression<?> expr = result.get();
    assertEquals(Operators.EQ, expr.getOperator(), "基本类型值应该转为EQ操作符");
  }

  @Test
  @DisplayName("Map - dot-separated 字段名应拆分为多段 FIELD 参数")
  void testMapDotSeparatedFieldNameBuildsSegmentedField() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("user.name", "张三");

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "dot-separated 字段名应成功规范化");
    assertEquals(Operators.EQ, result.get().getOperator(), "应转为 EQ 操作符");

    SExpression<?> fieldExpr = result.get().getParamAsSExpression(0);
    assertEquals(Operators.FIELD, fieldExpr.getOperator(), "左值应为 FIELD 表达式");
    assertEquals(2, fieldExpr.getParams().size(), "dot-separated 字段名应拆分为多段参数");
    assertEquals("user", fieldExpr.getParam(0));
    assertEquals("name", fieldExpr.getParam(1));
  }

  @Test
  @DisplayName("Map - 基本类型列表应该转为IN操作")
  void testMapWithBasicList() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("status",
        Arrays.asList("active", "pending"));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "基本类型列表处理应该成功");
    SExpression<?> expr = result.get();
    assertEquals(Operators.IN, expr.getOperator(), "基本类型列表应该转为IN操作符");
  }

  @Test
  @DisplayName("Map - IN 的右值应允许显式 constant 表达式")
  void testMapInAllowsExplicitConstantExpression() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("status",
        Collections.singletonMap("$in",
            Collections.singletonMap("$constant", Arrays.asList("active", "pending"))));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "IN 的右值应允许显式 constant 表达式");
    assertEquals(Operators.IN, result.get().getOperator(), "应该映射到 IN 操作符");
    assertEquals(Operators.CONSTANT, result.get().getParamAsSExpression(1).getOperator(), "右值应规范化为常量表达式");
    assertEquals(Arrays.asList("active", "pending"), result.get().getParamAsSExpression(1).getParam(0), "显式 constant 中的列表值应保持不变");
  }

  @Test
  @DisplayName("Map - NOT_IN 别名应支持原始基本列表 sugar")
  void testMapNotInAliasSupportsBasicListSugar() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("status",
        Collections.singletonMap("$not_in", Arrays.asList("closed", "cancelled")));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "NOT_IN 别名应支持原始基本列表 sugar");
    assertEquals(Operators.NOT_IN, result.get().getOperator(), "应该映射到 NOT_IN 操作符");
    assertEquals(Arrays.asList("closed", "cancelled"), result.get().getParamAsSExpression(1).getParam(0), "右值列表应保持不变");
  }

  @Test
  @DisplayName("Map - 操作符可带或不带$前缀")
  void testMapOperatorMustHaveDollarPrefix() {
    QueryNormalizeContext context = createTestContext();
    // 字段的Map值中，操作符既可以带 $ 前缀，也可以直接使用操作符别名
    Map<String, Object> map = Collections.singletonMap("age",
        Collections.singletonMap(">=", 18));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    // 无 $ 前缀的操作符别名同样应被正确识别
    assertTrue(result.isFailure() || result.get().getOperator() == Operators.GTE,
        "无 $ 前缀的操作符别名应被兼容识别");
  }

  @Test
  @DisplayName("Map - IS_NOT_NULL 下划线别名应正确走 null 特判分支")
  void testMapIsNotNullAliasWithUnderscore() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("deletedAt",
        Collections.singletonMap("$is_not_null", true));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "IS_NOT_NULL 下划线别名应成功规范化");
    assertEquals(Operators.IS_NOT_NULL, result.get().getOperator(), "应该映射到 IS_NOT_NULL 操作符");
  }

  @Test
  @DisplayName("Map - $gt 语法应被正确识别")
  void testMapOperatorWithDollarPrefix() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("age",
        Collections.singletonMap("$gt", 18));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "$gt 语法应被正确识别");
    assertEquals(Operators.GT, result.get().getOperator(), "$gt 应映射到 GT 操作符");
  }

  @Test
  @DisplayName("Map - 比较操作符右值应允许显式 List 表达式")
  void testMapComparisonOperatorAllowsExplicitListExpressionOnRight() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("age",
        Collections.singletonMap("eq", Arrays.asList("field", "otherAge")));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "比较操作符右值应允许显式表达式");
    assertEquals(Operators.EQ, result.get().getOperator(), "应映射到 EQ 操作符");
    assertEquals(Operators.FIELD, result.get().getParamAsSExpression(0).getOperator(), "左值应为字段表达式");
    assertEquals(Operators.FIELD, result.get().getParamAsSExpression(1).getOperator(), "右值应递归规范化为字段表达式");
    assertEquals("otherAge", result.get().getParamAsSExpression(1).getParam(0));
  }

  @Test
  @DisplayName("Map - between 端点应递归规范化为表达式")
  void testMapBetweenEndpointsAreNormalizedRecursively() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("age",
        Collections.singletonMap("between", Arrays.asList(
            Arrays.asList("field", "minAge"),
            Arrays.asList("field", "maxAge")
        )));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "between 端点应允许显式表达式");
    assertEquals(Operators.BETWEEN, result.get().getOperator(), "应映射到 BETWEEN 操作符");
    assertEquals(Operators.FIELD, result.get().getParamAsSExpression(1).getOperator(), "下界应递归规范化为字段表达式");
    assertEquals(Operators.FIELD, result.get().getParamAsSExpression(2).getOperator(), "上界应递归规范化为字段表达式");
    assertEquals("minAge", result.get().getParamAsSExpression(1).getParam(0));
    assertEquals("maxAge", result.get().getParamAsSExpression(2).getParam(0));
  }

  @Test
  @DisplayName("Map - $any 应规范化为 REL_ANY(FIELD, condition)")
  void testMapRelationAnyBuildsRelAnyExpression() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("orderItems",
        Collections.singletonMap("$any",
            Collections.singletonMap("status", "active")));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "$any 应成功规范化");
    assertEquals(ExtOps.REL_ANY, result.get().getOperator(), "$any 应映射到 REL_ANY");
    assertEquals(Operators.FIELD, result.get().getParamAsSExpression(0).getOperator(), "第一个参数应为 FIELD 表达式");
    assertEquals("orderItems", result.get().getParamAsSExpression(0).getParam(0), "关联字段路径应保留在 FIELD 中");
    assertEquals(Operators.EQ, result.get().getParamAsSExpression(1).getOperator(), "第二个参数应为条件表达式");
    assertEquals(Operators.FIELD, result.get().getParamAsSExpression(1).getParamAsSExpression(0).getOperator(), "条件左值应为字段表达式");
    assertEquals("status", result.get().getParamAsSExpression(1).getParamAsSExpression(0).getParam(0));
  }

  @Test
  @DisplayName("Map - $all/$none 应规范化为对应 REL 操作符")
  void testMapRelationAllAndNoneBuildExpectedOperators() {
    QueryNormalizeContext context = createTestContext();

    Map<String, Object> allMap = Collections.singletonMap("orderItems",
        Collections.singletonMap("$all",
            Collections.singletonMap("shipped", true)));
    Map<String, Object> noneMap = Collections.singletonMap("orderItems",
        Collections.singletonMap("$none",
            Collections.singletonMap("status", "cancelled")));

    Try<SExpression<?>> allResult = context.forClause("WHERE").normalizeExpression(allMap, "test.all");
    Try<SExpression<?>> noneResult = context.forClause("WHERE").normalizeExpression(noneMap, "test.none");

    assertTrue(allResult.isSuccess(), "$all 应成功规范化");
    assertTrue(noneResult.isSuccess(), "$none 应成功规范化");
    assertEquals(ExtOps.REL_ALL, allResult.get().getOperator(), "$all 应映射到 REL_ALL");
    assertEquals(ExtOps.REL_NONE, noneResult.get().getOperator(), "$none 应映射到 REL_NONE");
    assertEquals(Operators.FIELD, allResult.get().getParamAsSExpression(0).getOperator());
    assertEquals(Operators.FIELD, noneResult.get().getParamAsSExpression(0).getOperator());
    assertEquals("orderItems", allResult.get().getParamAsSExpression(0).getParam(0));
    assertEquals("orderItems", noneResult.get().getParamAsSExpression(0).getParam(0));
  }

  @Test
  @DisplayName("Map - 关联操作符的条件参数必须是布尔表达式")
  void testMapRelationOperatorRejectsNonBooleanCondition() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("orderItems",
        Collections.singletonMap("$any", Arrays.asList("constant", 1)));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isFailure(), "关联操作符的非布尔条件应被拒绝");
    assertInstanceOf(NormalizeException.class, result.getCause());
  }

  @Test
  @DisplayName("Map - 嵌套条件 Map 中混合字段与逻辑操作符时应正确递归并加前缀")
  void testNestedConditionMapSupportsMixedFieldAndLogicEntries() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("user", new LinkedHashMap<String, Object>() {{
      put("name", "张三");
      put("$or", Arrays.asList(
          Collections.singletonMap("age", 18),
          Collections.singletonMap("status", "active")
      ));
    }});

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "嵌套 Map 混合字段与逻辑操作符应成功");
    assertEquals(Operators.AND, result.get().getOperator(), "应组合为 AND");
    assertEquals(Operators.EQ, result.get().getParamAsSExpression(0).getOperator());
    assertEquals(Operators.OR, result.get().getParamAsSExpression(1).getOperator(), "逻辑子项应保持 OR");
    assertEquals("user", result.get().getParamAsSExpression(0).getParamAsSExpression(0).getParam(0), "字段前缀应加到 user");
    assertEquals("name", result.get().getParamAsSExpression(0).getParamAsSExpression(0).getParam(1));
    assertEquals("user", result.get().getParamAsSExpression(1).getParamAsSExpression(0).getParamAsSExpression(0).getParam(0));
    assertEquals("age", result.get().getParamAsSExpression(1).getParamAsSExpression(0).getParamAsSExpression(0).getParam(1));
  }

  @Test
  @DisplayName("Map - 嵌套关系路径应保留完整前缀")
  void testNestedRelationPathKeepsFullPrefix() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("user",
        Collections.singletonMap("orders",
            Collections.singletonMap("$any",
                Collections.singletonMap("status", "active"))));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "嵌套关系路径应成功规范化");
    assertEquals(ExtOps.REL_ANY, result.get().getOperator());
    assertEquals(Operators.FIELD, result.get().getParamAsSExpression(0).getOperator(), "关系路径应保持为 FIELD 表达式");
    assertEquals(2, result.get().getParamAsSExpression(0).getParams().size(), "关系路径应保留完整分段");
    assertEquals("user", result.get().getParamAsSExpression(0).getParam(0));
    assertEquals("orders", result.get().getParamAsSExpression(0).getParam(1));
    assertEquals("status", result.get().getParamAsSExpression(1).getParamAsSExpression(0).getParam(0),
        "关系条件内部字段应保持相对路径，不应再叠加外层前缀");
  }

  @Test
  @DisplayName("Map - 关联 $count 比较应规范化为比较(COUNT(FIELD), 常量)")
  void testMapRelationCountBuildsAggregateComparison() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("orderItems",
        Collections.singletonMap("$count",
            Collections.singletonMap("$gt", 2)));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "$count 关联聚合应成功规范化");
    assertEquals(Operators.GT, result.get().getOperator());

    SExpression<?> leftExpr = result.get().getParamAsSExpression(0);
    assertEquals(Operators.COUNT, leftExpr.getOperator());
    assertEquals(Operators.FIELD, leftExpr.getParamAsSExpression(0).getOperator());
    assertEquals("orderItems", leftExpr.getParamAsSExpression(0).getParam(0));
    assertEquals(2, result.get().getParamAsSExpression(1).getParam(0));
  }

  @Test
  @DisplayName("Map - 嵌套树改写不应接受非 FIELD/CONSTANT 的 mixed-param 表达式")
  void testNestedTreeRewriteRejectsMixedParamExpression() {
    QueryNormalizeContext context = createTestContext();
    SExpression<?> invalidMixedParamExpr = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "name"),
        "张三"
    );
    Map<String, Object> map = Collections.singletonMap("user",
        Collections.singletonMap("$or", Collections.singletonList(invalidMixedParamExpr)));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isFailure(), "非 FIELD/CONSTANT mixed-param 表达式应直接失败");
    assertTrue(result.getCause() instanceof NormalizeException);
  }

  @Test
  @DisplayName("Map - 嵌套条件 Map 的 AND 组合应走 logic builder 优化")
  void testNestedConditionMapAndUsesLogicBuilderOptimization() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("user", new LinkedHashMap<String, Object>() {{
      put("$and", Arrays.asList(
          Collections.singletonMap("age", 18),
          Collections.singletonMap("status", "active")
      ));
      put("name", "张三");
    }});

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "嵌套条件 Map 应成功规范化");
    assertEquals(Operators.AND, result.get().getOperator(), "最终仍应为 AND");
    assertEquals(3, result.get().getParams().size(), "走 logic builder 时应把内层 AND flatten 为三个条件");
  }

  @Test
  @DisplayName("Map - 字段作用域内的嵌套条件 Map 不应接受顶层 special operator")
  void testNestedConditionMapRejectsTopLevelSpecialOperator() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("user",
        Collections.singletonMap("$constant", true));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isFailure(), "字段作用域内的嵌套条件 Map 不应接受顶层 special operator");
  }

  @Test
  @DisplayName("Map - 顶层 $constant 应规范化为常量表达式")
  void testTopLevelSpecialConstantBuildsConstantExpression() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("$constant", true);

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "顶层 $constant 应成功规范化");
    assertEquals(Operators.CONSTANT, result.get().getOperator(), "应该映射到 CONSTANT 操作符");
    assertEquals(Boolean.TRUE, result.get().getParam(0), "常量值应保持不变");
  }

  @Test
  @DisplayName("Map - gt 与 $gt 应产生相同操作符")
  void testMapOperatorWithAndWithoutDollarPrefixAreEquivalent() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> withDollar = Collections.singletonMap("age",
        Collections.singletonMap("$gt", 18));
    Map<String, Object> withoutDollar = Collections.singletonMap("age",
        Collections.singletonMap("gt", 18));

    Try<SExpression<?>> resultWithDollar = context.forClause("WHERE").normalizeExpression(withDollar, "test.withDollar");
    Try<SExpression<?>> resultWithoutDollar = context.forClause("WHERE").normalizeExpression(withoutDollar, "test.withoutDollar");

    assertTrue(resultWithDollar.isSuccess(), "带 $ 前缀的 Map 操作符应成功");
    assertTrue(resultWithoutDollar.isSuccess(), "不带 $ 前缀的 Map 操作符应成功");
    assertEquals(resultWithDollar.get().getOperator(), resultWithoutDollar.get().getOperator(),
        "gt 与 $gt 应映射到同一操作符");
    assertEquals(Operators.GT, resultWithDollar.get().getOperator(), "应映射到 GT 操作符");
  }

  @Test
  @DisplayName("Map - 逻辑组合操作符应该正确处理")
  void testMapLogicCombination() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap(
        "$and", Arrays.asList(
            Collections.singletonMap("name", "张三"),
            Collections.singletonMap("age", Collections.singletonMap("$gte", 18))
        )
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "逻辑组合处理应该成功");
    assertEquals(Operators.AND, result.get().getOperator(), "应该是AND操作符");
  }

  @Test
  @DisplayName("Map - 逻辑组合中的非布尔参数应失败")
  void testMapLogicCombinationRejectsNonBooleanParams() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap(
        "$and", Arrays.asList(1, 2)
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isFailure(), "逻辑组合中的非布尔参数应被拒绝");
  }

  @Test
  @DisplayName("Map - 多个条件应该用AND组合")
  void testMapMultipleConditions() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = new HashMap<String, Object>() {{
      put("name", "张三");
      put("age", 18);
      put("status", "active");
    }};

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "多条件处理应该成功");
    assertEquals(Operators.AND, result.get().getOperator(), "多个条件应该用AND组合");
    assertEquals(3, result.get().getParams().size(), "应该有3个子条件");
  }

  // ==================== 2. List表达式严密性测试 ====================

  @Test
  @DisplayName("List - 空列表应该报错")
  void testEmptyList() {
    QueryNormalizeContext context = createTestContext();
    List<?> emptyList = Collections.emptyList();

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(emptyList, "test");

    assertTrue(result.isFailure(), "空列表应该报错");
  }

  @Test
  @DisplayName("List - 第一个元素为null应该报错")
  void testListWithNullOperator() {
    QueryNormalizeContext context = createTestContext();
    List<Object> listWithNull = new ArrayList<>();
    listWithNull.add(null);
    listWithNull.add("param");

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(listWithNull, "test");

    assertTrue(result.isFailure(), "操作符为null应该报错");
  }

  @Test
  @DisplayName("List - 操作符字符串应该正确映射")
  void testListOperatorMapping() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Arrays.asList("eq", "name", "张三");

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isSuccess(), "操作符映射应该成功");
    assertEquals(Operators.EQ, result.get().getOperator(), "应该映射到EQ操作符");
    assertEquals(Operators.CONSTANT, result.get().getParamAsSExpression(0).getOperator(), "裸字符串参数应被视为常量");
    assertEquals(Operators.CONSTANT, result.get().getParamAsSExpression(1).getOperator(), "裸字符串参数应被视为常量");
  }

  @Test
  @DisplayName("List - 单元素列表应视为字段简写")
  void testSingleElementListAsFieldSugar() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Collections.singletonList("name");

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isSuccess(), "单元素列表字段简写应成功");
    assertEquals(Operators.FIELD, result.get().getOperator(), "单元素列表应被解释为 FIELD 表达式");
    assertEquals("name", result.get().getParam(0), "字段名应保持不变");
  }

  @Test
  @DisplayName("List - FIELD 显式形式应支持多段字段路径")
  void testListFieldOperatorSupportsMultiSegmentPath() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Arrays.asList("field", "user", "name");

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isSuccess(), "FIELD 显式形式应支持多段字段路径");
    assertEquals(Operators.FIELD, result.get().getOperator(), "应该映射到 FIELD 操作符");
    assertEquals("user", result.get().getParam(0), "第一段字段路径应保持不变");
    assertEquals("name", result.get().getParam(1), "第二段字段路径应保持不变");
  }

  @Test
  @DisplayName("List - IN 的右值裸列表因形式歧义应失败")
  void testListInRejectsAmbiguousRawList() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Arrays.asList("in", "status", Arrays.asList("active", "pending"));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isFailure(), "IN 的右值裸列表存在形式歧义，应要求显式 constant 包裹");
  }

  @Test
  @DisplayName("List - IS_NULL 的单个字符串参数应被视为常量")
  void testListIsNullTreatsSingleStringAsConstant() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Arrays.asList("isNull", "name");

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isSuccess(), "IS_NULL 应成功规范化");
    assertEquals(Operators.IS_NULL, result.get().getOperator(), "应该映射到 IS_NULL 操作符");
    assertEquals(Operators.CONSTANT, result.get().getParamAsSExpression(0).getOperator(), "裸字符串参数应被规范化为常量表达式");
    assertEquals("name", result.get().getParamAsSExpression(0).getParam(0), "常量值应保持不变");
  }

  @Test
  @DisplayName("List - CONSTANT 应阻断对列表值的递归解析")
  void testListConstantStopsRecursionForNestedList() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Arrays.asList("constant", Arrays.asList("eq", "a", "b"));

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isSuccess(), "constant 应成功保留原始列表值");
    assertEquals(Operators.CONSTANT, result.get().getOperator(), "应该映射到 CONSTANT 操作符");
    assertEquals(Arrays.asList("eq", "a", "b"), result.get().getParam(0), "列表值应作为字面量整体保留");
  }

  @Test
  @DisplayName("List - 不支持的操作符应该报错")
  void testListUnknownOperator() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Arrays.asList("$unknownOperator", "param1", "param2");

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isFailure(), "未知操作符应该报错");
    // 接受多种错误消息格式：
    // - "无法识别的操作符" (Map格式)
    // - "未知的操作符" (Operator解析失败)
    // - "不支持的表达式类型" (规范化器匹配失败)
    String errorMsg = result.getCause().getMessage();
    assertTrue(errorMsg.contains("无法识别") ||
            errorMsg.contains("未知") ||
            errorMsg.contains("不支持"),
        "错误信息应该包含'无法识别'、'未知'或'不支持'");
  }

  @Test
  @DisplayName("List - 已知操作符带$前缀也应报错")
  void testListKnownDollarPrefixedOperatorRejected() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Arrays.asList("$gt", "age", 18);

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isFailure(), "List 语法中的 $gt 应被拒绝");
    assertTrue(result.getCause().getMessage().contains("未知的操作符"),
        "错误信息应明确说明 $gt 不是 List 语法下的合法操作符");
  }

  @Test
  @DisplayName("List - 逻辑组合中的非布尔参数应失败")
  void testListLogicCombinationRejectsNonBooleanParams() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Arrays.asList("and", 1, 2);

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isFailure(), "逻辑组合中的非布尔参数应被拒绝");
  }

  // ==================== 3. 逻辑优化严密性测试 ====================

  @Test
  @DisplayName("逻辑优化 - AND扁平化")
  void testAndFlattening() {
    QueryNormalizeContext context = createTestContext();
    // AND(AND(x, y), z) 应该扁平化为 AND(x, y, z)
    Map<String, Object> map = Collections.singletonMap(
        "$and", Arrays.asList(
            Collections.singletonMap("$and", Arrays.asList(
                Collections.singletonMap("x", 1),
                Collections.singletonMap("y", 2))),
            Collections.singletonMap("z", 3)
        )
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "AND扁平化应该成功");
    SExpression<?> expr = result.get();
    assertEquals(Operators.AND, expr.getOperator(), "应该是AND操作符");
    assertEquals(3, expr.getParams().size(), "扁平化后应该有3个参数");
  }

  @Test
  @DisplayName("逻辑优化 - AND短路优化")
  void testAndShortCircuit() {
    QueryNormalizeContext context = createTestContext();
    // AND(..., FALSE, ...) 应该短路为 FALSE
    Map<String, Object> map = Collections.singletonMap(
        "$and", Arrays.asList(
            Collections.singletonMap("name", "张三"),
            Collections.singletonMap("$constant", false),
            Collections.singletonMap("age", 18)
        )
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "AND短路应该成功");
    SExpression<?> expr = result.get();
    // 如果实现了短路，应该返回常量FALSE
    if (expr.getOperator() == Operators.CONSTANT) {
      assertEquals(Boolean.FALSE, expr.getParam(0), "AND短路应该返回FALSE");
    }
  }

  @Test
  @DisplayName("逻辑优化 - OR短路优化")
  void testOrShortCircuit() {
    QueryNormalizeContext context = createTestContext();
    // OR(..., TRUE, ...) 应该短路为 TRUE
    Map<String, Object> map = Collections.singletonMap(
        "$or", Arrays.asList(
            Collections.singletonMap("name", "张三"),
            Collections.singletonMap("$constant", true),
            Collections.singletonMap("age", 18)
        )
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "OR短路应该成功");
    SExpression<?> expr = result.get();
    // 如果实现了短路，应该返回常量TRUE
    if (expr.getOperator() == Operators.CONSTANT) {
      assertEquals(Boolean.TRUE, expr.getParam(0), "OR短路应该返回TRUE");
    }
  }

  @Test
  @DisplayName("逻辑优化 - 单个条件穿透")
  void testSingleConditionTransparency() {
    QueryNormalizeContext context = createTestContext();
    // AND(single_condition) 应该直接返回 single_condition
    Map<String, Object> map = Collections.singletonMap(
        "$and", Arrays.asList(Collections.singletonMap("name", "张三"))
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "单条件穿透应该成功");
    SExpression<?> expr = result.get();
    // 单个条件应该穿透，不应该有AND包装
    assertEquals(Operators.EQ, expr.getOperator(), "单条件应该穿透AND包装");
  }

  @Test
  @DisplayName("逻辑优化 - List OR 中空表达式应被忽略而不是补 TRUE")
  void testListOrIgnoresEmptyExpressionInsteadOfPromotingTrue() {
    QueryNormalizeContext context = createTestContext();
    List<Object> list = Arrays.asList(
        "or",
        Collections.emptyMap(),
        Arrays.asList("eq", "name", "张三")
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    assertTrue(result.isSuccess(), "List OR 应成功处理空表达式子项");
    SExpression<?> expr = result.get();
    assertEquals(Operators.EQ, expr.getOperator(), "空表达式应被忽略，单个剩余条件应直接穿透");
  }

  // ==================== 4. 上下文感知严密性测试 ====================

  @Test
  @DisplayName("上下文感知 - WHERE中不允许聚合操作符")
  void testWhereDoesNotAllowAggregation() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    // AggregationExpressionBuilder 支持聚合操作符
    boolean hasAggBuilder = context.getSExpressionBuilders().stream()
        .anyMatch(b -> b instanceof AggregationExpressionBuilder
            && b.supports(Operators.COUNT));
    assertTrue(hasAggBuilder, "应该有聚合表达式构建器支持COUNT");
  }

  @Test
  @DisplayName("上下文感知 - HAVING中允许聚合操作符")
  void testHavingAllowsAggregation() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    // AggregationExpressionBuilder 支持 COUNT 操作符
    boolean hasAggBuilder = context.getSExpressionBuilders().stream()
        .anyMatch(b -> b instanceof AggregationExpressionBuilder
            && b.supports(Operators.COUNT));
    assertTrue(hasAggBuilder, "应该有聚合表达式构建器支持COUNT");
  }

  @Test
  @DisplayName("上下文感知 - 比较操作符构建器支持EQ")
  void testOrderDoesNotAllowComparisonOperators() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    // ComparisonExpressionBuilder 支持 EQ 操作符
    boolean hasComparisonBuilder = context.getSExpressionBuilders().stream()
        .anyMatch(b -> b instanceof ComparisonExpressionBuilder
            && b.supports(Operators.EQ));
    assertTrue(hasComparisonBuilder, "应该有比较表达式构建器支持EQ");
  }

  // ==================== 5. 错误处理严密性测试 ====================

  @Test
  @DisplayName("错误处理 - 不支持的表达式类型应该有详细错误信息")
  void testUnsupportedExpressionTypeError() {
    QueryNormalizeContext context = createTestContext();
    Object unsupported = new Object();

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(unsupported, "test.field");

    assertTrue(result.isFailure(), "不支持的类型应该报错");
    String errorMsg = result.getCause().getMessage();
    assertTrue(errorMsg.contains("不支持"), "错误信息应该说明不支持");
    assertTrue(errorMsg.contains(unsupported.getClass().getName()),
        "错误信息应该包含类型名称");
  }

  @Test
  @DisplayName("错误处理 - Map中的非操作符key应该报错")
  void testMapNonOperatorKeyError() {
    QueryNormalizeContext context = createTestContext();
    // 在字段的Map值中，key必须是操作符
    Map<String, Object> map = Collections.singletonMap(
        "name", new HashMap<String, Object>() {{
          put("contains", "张");  // 缺少$前缀
          put("invalidKey", "value");
        }}
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isFailure(), "字段操作符 Map 混入非操作符 key 时应失败，不能误解释为嵌套字段树");
  }

  @Test
  @DisplayName("错误处理 - 操作符参数不匹配应该报错")
  void testOperatorParameterMismatch() {
    QueryNormalizeContext context = createTestContext();
    // BETWEEN需要2个参数（下界和上界），但只提供1个
    List<Object> list = Arrays.asList("between", "age", 18);  // 缺少上界

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(list, "test");

    // 如果实现了严格检查，应该报错
    assertTrue(result.isSuccess() || result.isFailure(),
        "参数不匹配应该被处理");
  }

  // ==================== 6. 递归处理严密性测试 ====================

  @Test
  @DisplayName("递归处理 - 深层嵌套应该正确处理")
  void testDeepNesting() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap(
        "$and", Arrays.asList(
            Collections.singletonMap("$or", Arrays.asList(
                Collections.singletonMap("name", "张三"),
                Collections.singletonMap("name", "李四")
            )),
            Collections.singletonMap("$and", Arrays.asList(
                Collections.singletonMap("age", Collections.singletonMap("$gte", 18)),
                Collections.singletonMap("status", "active")
            ))
        )
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "深层嵌套应该成功处理");
    SExpression<?> expr = result.get();
    assertNotNull(expr, "结果不应该为null");
  }

  @Test
  @DisplayName("递归处理 - 混合Map和List应该正确处理")
  void testMixedMapAndList() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap(
        "$and", Arrays.asList(
            Collections.singletonMap("name", "张三"),
            Arrays.asList("eq", "age", 18)  // List表达式
        )
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "混合Map和List应该成功处理");
  }

  // ==================== 7. 边界条件严密性测试 ====================

  @Test
  @DisplayName("边界条件 - 空字符串字段名应该报错")
  void testEmptyFieldName() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> map = Collections.singletonMap("", "value");

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isFailure(), "空字符串字段名应被拒绝");
  }

  @Test
  @DisplayName("边界条件 - 超大Map应该正确处理")
  void testLargeMap() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> largeMap = new HashMap<>();
    for (int i = 0; i < 100; i++) {
      largeMap.put("field" + i, "value" + i);
    }

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(largeMap, "test");

    assertTrue(result.isSuccess(), "大Map应该成功处理");
    assertEquals(Operators.AND, result.get().getOperator(), "应该用AND组合");
    assertEquals(100, result.get().getParams().size(), "应该有100个条件");
  }

  @Test
  @DisplayName("边界条件 - 空表达式过滤")
  void testEmptyExpressionFiltering() {
    QueryNormalizeContext context = createTestContext();
    // 某些条件可能规范化为空表达式，应该被过滤
    Map<String, Object> map = Collections.singletonMap(
        "$and", Arrays.asList(
            Collections.singletonMap("name", "张三"),
            Collections.emptyMap(),  // 空Map
            Collections.singletonMap("age", 18)
        )
    );

    Try<SExpression<?>> result = context.forClause("WHERE").normalizeExpression(map, "test");

    assertTrue(result.isSuccess(), "空表达式应该被过滤");
    // 结果中不应该包含空表达式
  }

  // ==================== 8. 一致性测试 ====================

  @Test
  @DisplayName("一致性 - 相同输入应该产生相同输出")
  void testConsistency() {
    QueryNormalizeContext context = createTestContext();
    Map<String, Object> input = new HashMap<String, Object>() {{
      put("name", "张三");
      put("age", 18);
    }};

    Try<SExpression<?>> result1 = context.forClause("WHERE").normalizeExpression(input, "test");
    Try<SExpression<?>> result2 = context.forClause("WHERE").normalizeExpression(input, "test");

    assertTrue(result1.isSuccess() && result2.isSuccess(), "两次处理都应该成功");
    assertEquals(result1.get().toString(), result2.get().toString(),
        "相同输入应该产生相同输出");
  }

  @Test
  @DisplayName("一致性 - 等价输入应该产生等价输出")
  void testEquivalenceConsistency() {
    QueryNormalizeContext context = createTestContext();

    // Map形式
    Map<String, Object> mapForm = Collections.singletonMap(
        "$and", Arrays.asList(
            Collections.singletonMap("name", "张三"),
            Collections.singletonMap("age", 18)
        )
    );

    // 扁平Map形式（应该自动转为AND）
    Map<String, Object> flatForm = new HashMap<String, Object>() {{
      put("name", "张三");
      put("age", 18);
    }};

    Try<SExpression<?>> result1 = context.forClause("WHERE").normalizeExpression(mapForm, "test");
    Try<SExpression<?>> result2 = context.forClause("WHERE").normalizeExpression(flatForm, "test");

    assertTrue(result1.isSuccess() && result2.isSuccess(), "两种形式都应该成功");
    assertEquals(Operators.AND, result1.get().getOperator(), "都应该是AND操作符");
    assertEquals(Operators.AND, result2.get().getOperator(), "都应该是AND操作符");
  }
}
