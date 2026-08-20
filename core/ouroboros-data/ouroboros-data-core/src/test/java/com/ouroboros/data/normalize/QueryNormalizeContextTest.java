package com.ouroboros.data.normalize;

import static com.ouroboros.data.dsl.query.Query.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.query.Query;
import com.ouroboros.data.dsl.query.QuerySource;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.normalize.expressionnormalizers.ListExpressionNormalizer;
import com.ouroboros.data.normalize.expressionnormalizers.MapExpressionNormalizer;
import com.ouroboros.data.normalize.expressionnormalizers.QueryFacadeExpressionNormalizer;
import com.querydsl.core.types.Operator;

/**
 * QueryNormalizeContext 测试类
 */
public class QueryNormalizeContextTest {

  @Test
  void testBasicContextBuilding() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .build();

    // model/relatedModels 已从 QueryNormalizeContext 移除
    assertNotNull(context);
  }

  @Test
  void testClauseNormalizerOrdering() {
    ClauseNormalizer normalizer1 = createMockClauseNormalizer("Normalizer1");
    ClauseNormalizer normalizer2 = createMockClauseNormalizer("Normalizer2");
    ClauseNormalizer normalizer3 = createMockClauseNormalizer("Normalizer3");

    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .addClauseNormalizer(normalizer1)
        .addClauseNormalizerAfter(normalizer2, "Normalizer1")
        .addClauseNormalizerBeforeAll(normalizer3)
        .build();

    List<ClauseNormalizer> normalizers = context.getClauseNormalizers();
    assertEquals(3, normalizers.size());
    assertEquals(normalizer3, normalizers.get(0)); // beforeAll
    assertEquals(normalizer1, normalizers.get(1)); // original
    assertEquals(normalizer2, normalizers.get(2)); // after normalizer1
  }

  @Test
  void testExpressionNormalizerOrdering() {
    RawExpressionNormalizer normalizer1 = createMockExpressionNormalizer("ExprNormalizer1");
    RawExpressionNormalizer normalizer2 = createMockExpressionNormalizer("ExprNormalizer2");

    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .addExpressionNormalizer(normalizer1)
        .addExpressionNormalizerBefore(normalizer2, ExpressionNormalizer1.class)
        .build();

    List<RawExpressionNormalizer> normalizers = context.getExpressionNormalizers();
    assertEquals(2, normalizers.size());
    assertEquals(normalizer2, normalizers.get(0)); // before ExpressionNormalizer1
    assertEquals(normalizer1, normalizers.get(1)); // original
  }

  @Test
  void testSExpressionNormalizerOrdering() {
    SExpressionBuilder builder1 = createMockSExpressionBuilder("Builder1");
    SExpressionBuilder builder2 = createMockSExpressionBuilder("Builder2");
    SExpressionBuilder builder3 = createMockSExpressionBuilder("Builder3");

    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .addSExpressionNormalizer(builder1)
        .addSExpressionNormalizerAfterAll(builder2)
        .addSExpressionNormalizerBefore(builder3, "Builder1")
        .build();

    List<SExpressionNormalizer> normalizers = context.getSExpressionNormalizers();
    assertEquals(3, normalizers.size());
    assertEquals(builder3, normalizers.get(0)); // before Builder1
    assertEquals(builder1, normalizers.get(1)); // original
    assertEquals(builder2, normalizers.get(2)); // afterAll

    List<SExpressionBuilder> builders = context.getSExpressionBuilders();
    assertEquals(3, builders.size());
    assertEquals(builder3, builders.get(0));
    assertEquals(builder1, builders.get(1));
    assertEquals(builder2, builders.get(2));
  }

  @Test
  void testClauseContextCreation() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .build();

    ClauseNormalizeContext clauseContext = context.forClause("WHERE");
    assertEquals("WHERE", clauseContext.getClauseType());
    assertEquals(context, clauseContext.getQueryContext());
  }

  @Test
  void testExpressionContextCreation() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .build();

    ClauseNormalizeContext clauseContext = context.forClause("WHERE");
    ExpressionNormalizeContext expressionContext = clauseContext.forExpression("user.name");

    assertEquals("user.name", expressionContext.getExpressionPath());
    assertEquals("WHERE", expressionContext.getClauseType());
    assertEquals(clauseContext, expressionContext.getClauseContext());
    assertEquals(context, expressionContext.getQueryContext());
  }

  @Test
  void testTopLevelNormalizeExpressionUsesUnifiedExpressionSemantics() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    var result = context.normalizeExpression(Arrays.asList("field", "name"), "expr");

    assertTrue(result.isSuccess(), () -> "顶层 normalizeExpression 应可直接处理原始表达式: "
        + (result.isFailure() ? result.getCause() : ""));
    assertEquals(Operators.FIELD, result.get().getOperator());
    assertFalse(result.get().isEmpty());
  }

  @Test
  void testTopLevelNormalizeConditionActsAsBooleanHelper() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    var result = context.normalizeCondition(Collections.singletonMap("name", "张三"), "cond");

    assertTrue(result.isSuccess(), () -> "顶层 normalizeCondition 应可作为布尔表达式 helper: "
        + (result.isFailure() ? result.getCause() : ""));
    assertEquals(Operators.EQ, result.get().getOperator());
    assertEquals(Boolean.class, result.get().getDataType());
  }

  @Test
  void testRawFromNormalizesQuerySource() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    QuerySource source = () -> Map.of("u", "User");

    var result = context.normalizeQuery(Map.of("FROM", source));

    assertTrue(result.isSuccess(), () -> "FROM 子句应可直接接受 QuerySource: "
        + (result.isFailure() ? result.getCause() : ""));
    assertEquals("User", result.get().getFrom().getTableName());
    assertEquals("u", result.get().getFrom().getAlias());
  }

  @Test
  void testRawQueryExpressionFacadeNormalizesAsExpression() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    var result = context.forClause("WHERE").normalizeExpression(field("user", "name"), "expr");

    assertTrue(result.isSuccess(), () -> "QueryExpression 应在 raw expression 入口归一化: "
        + (result.isFailure() ? result.getCause() : ""));
    assertEquals(Operators.FIELD, result.get().getOperator());
    assertEquals("user", result.get().getParam(0));
    assertEquals("name", result.get().getParam(1));
  }

  @Test
  void testRawSelectAliasMapNormalizesQueryExpressionValues() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    Map<String, Object> query = new HashMap<>();
    query.put("FROM", "user");
    query.put("SELECT", Map.of("userName", field("name")));
    var result = context.normalizeQuery(query);

    assertTrue(result.isSuccess(), () -> "SELECT alias map 中的 QueryExpression 应可归一化: "
        + (result.isFailure() ? result.getCause() : ""));

    SExpression<?> columns = result.get().getSelect().get(0);
    assertEquals(Operators.COLUMNS, columns.getOperator());
    SExpression<?> alias = columns.getParamAsSExpression(0);
    assertEquals(Operators.ALIAS, alias.getOperator());
    assertEquals("userName", alias.getParam(1));
    SExpression<?> aliasedField = alias.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, aliasedField.getOperator());
    assertEquals("name", aliasedField.getParam(0));
  }

  @Test
  void testRawSelectNormalizesQueryExpressionAliases() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    Map<String, Object> query = new HashMap<>();
    query.put("FROM", "user");
    query.put("SELECT", List.of(field("id"), field("name").as("username")));
    var result = context.normalizeQuery(query);

    assertTrue(result.isSuccess(), () -> "SELECT 子句应支持 field.as(alias): "
        + (result.isFailure() ? result.getCause() : ""));

    SExpression<?> columns = result.get().getSelect().get(0);
    assertEquals(Operators.COLUMNS, columns.getOperator());
    assertEquals(2, columns.getParams().size());
    assertEquals(Operators.FIELD, columns.getParamAsSExpression(0).getOperator());
    assertEquals("id", columns.getParamAsSExpression(0).getParam(0));

    SExpression<?> alias = columns.getParamAsSExpression(1);
    assertEquals(Operators.ALIAS, alias.getOperator());
    assertEquals("username", alias.getParam(1));
    assertEquals(Operators.FIELD, alias.getParamAsSExpression(0).getOperator());
    assertEquals("name", alias.getParamAsSExpression(0).getParam(0));
  }

  @Test
  void testRawWhereNormalizesQueryCondition() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    Map<String, Object> query = new HashMap<>();
    query.put("FROM", "user");
    query.put("WHERE", field("age").gt(18));
    var result = context.normalizeQuery(query);

    assertTrue(result.isSuccess(), () -> "WHERE 中的 QueryCondition 应可归一化: "
        + (result.isFailure() ? result.getCause() : ""));
    SExpression<Boolean> where = result.get().getWhere();
    assertEquals(Operators.GT, where.getOperator());
    assertEquals(Operators.FIELD, where.getParamAsSExpression(0).getOperator());
    assertEquals("age", where.getParamAsSExpression(0).getParam(0));
    assertEquals(18, where.getParamAsSExpression(1).getParam(0));
  }

  @Test
  void testRawWhereOperatorRightValueNormalizesQueryExpression() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    Map<String, Object> query = new HashMap<>();
    query.put("FROM", "user");
    query.put("WHERE", Map.of("age", Map.of("$gt", field("minAge"))));
    var result = context.normalizeQuery(query);

    assertTrue(result.isSuccess(), () -> "字段比较右值中的 QueryExpression 应可归一化: "
        + (result.isFailure() ? result.getCause() : ""));
    SExpression<Boolean> where = result.get().getWhere();
    assertEquals(Operators.GT, where.getOperator());
    assertEquals(Operators.FIELD, where.getParamAsSExpression(0).getOperator());
    assertEquals("age", where.getParamAsSExpression(0).getParam(0));
    assertEquals(Operators.FIELD, where.getParamAsSExpression(1).getOperator());
    assertEquals("minAge", where.getParamAsSExpression(1).getParam(0));
  }

  @Test
  void testBuiltWhereOperatorRightValueNormalizesAsFieldExpression() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    var result = context.normalizeQuery(Query.from("user")
        .where(field("age").gt(field("minAge")))
        .build());

    assertTrue(result.isSuccess(), () -> "builder 输出的字段比较右值应可归一化为 FIELD: "
        + (result.isFailure() ? result.getCause() : ""));
    SExpression<Boolean> where = result.get().getWhere();
    assertEquals(Operators.GT, where.getOperator());
    assertEquals(Operators.FIELD, where.getParamAsSExpression(0).getOperator());
    assertEquals("age", where.getParamAsSExpression(0).getParam(0));
    assertEquals(Operators.FIELD, where.getParamAsSExpression(1).getOperator());
    assertEquals("minAge", where.getParamAsSExpression(1).getParam(0));
  }

  @Test
  void testBuiltRawWhereMapOperatorRightValueNormalizesAsFieldExpression() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    var result = context.normalizeQuery(Query.from("user")
        .where(Map.of("age", Map.of("$gt", field("minAge"))))
        .build());

    assertTrue(result.isSuccess(), () -> "builder raw map 中的字段比较右值应可归一化为 FIELD: "
        + (result.isFailure() ? result.getCause() : ""));
    SExpression<Boolean> where = result.get().getWhere();
    assertEquals(Operators.GT, where.getOperator());
    assertEquals(Operators.FIELD, where.getParamAsSExpression(0).getOperator());
    assertEquals("age", where.getParamAsSExpression(0).getParam(0));
    assertEquals(Operators.FIELD, where.getParamAsSExpression(1).getOperator());
    assertEquals("minAge", where.getParamAsSExpression(1).getParam(0));
  }

  @Test
  void testDefaultExpressionNormalizersUseDedicatedExpressionPackage() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    List<RawExpressionNormalizer> normalizers = context.getExpressionNormalizers();
    assertFalse(normalizers.isEmpty());
    assertTrue(normalizers.get(0) instanceof QueryFacadeExpressionNormalizer);
    assertTrue(normalizers.get(1) instanceof ListExpressionNormalizer);
    assertTrue(normalizers.get(2) instanceof MapExpressionNormalizer);
    assertEquals("com.ouroboros.data.normalize.expressionnormalizers",
        normalizers.get(0).getClass().getPackage().getName());
    assertEquals("com.ouroboros.data.normalize.expressionnormalizers",
        normalizers.get(1).getClass().getPackage().getName());
  }

  @Test
  void testSelectClauseNormalizesFieldListIntoTranspilableExpressions() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    Map<String, Object> query = new HashMap<>();
    query.put("FROM", "user");
    query.put("SELECT", Arrays.asList("id", Collections.singletonMap("userName", "name")));
    var result = context.normalizeQuery(query);

    assertTrue(result.isSuccess(), () -> "SELECT 子句规范化失败: "
        + (result.isFailure() ? result.getCause() : ""));

    QueryStatement statement = result.get();
    assertEquals(1, statement.getSelect().size());

    SExpression<?> columns = statement.getSelect().get(0);
    assertEquals(Operators.COLUMNS, columns.getOperator());
    assertEquals(2, columns.getParams().size());

    SExpression<?> idField = columns.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, idField.getOperator());
    assertEquals("id", idField.getParam(0));

    SExpression<?> aliasExpr = columns.getParamAsSExpression(1);
    assertEquals(Operators.ALIAS, aliasExpr.getOperator());
    assertEquals("userName", aliasExpr.getParam(1));

    SExpression<?> aliasedField = aliasExpr.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, aliasedField.getOperator());
    assertEquals("name", aliasedField.getParam(0));

    assertSame(columns, statement.getSelect().get(0));
  }

  @Test
  void testGroupClauseNormalizesFieldListIntoTranspilableExpressions() {
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    Map<String, Object> query = new HashMap<>();
    query.put("FROM", "user");
    query.put("GROUP_BY", Arrays.asList("departmentId", "status"));
    var result = context.normalizeQuery(query);

    assertTrue(result.isSuccess(), () -> "GROUP BY 子句规范化失败: "
        + (result.isFailure() ? result.getCause() : ""));

    QueryStatement statement = result.get();
    SExpression<?> group = statement.getGroup();
    assertEquals(Operators.COLUMNS, group.getOperator());
    assertEquals(2, group.getParams().size());
    assertEquals(Operators.FIELD, group.getParamAsSExpression(0).getOperator());
    assertEquals("departmentId", group.getParamAsSExpression(0).getParam(0));
    assertEquals(Operators.FIELD, group.getParamAsSExpression(1).getOperator());
    assertEquals("status", group.getParamAsSExpression(1).getParam(0));
  }

  // Helper methods to create mock objects
  private ClauseNormalizer createMockClauseNormalizer(String name) {
    switch (name) {
      case "Normalizer1":
        return new Normalizer1();
      case "Normalizer2":
        return new Normalizer2();
      case "Normalizer3":
        return new Normalizer3();
      default:
        throw new IllegalArgumentException("Unknown normalizer: " + name);
    }
  }

  private RawExpressionNormalizer createMockExpressionNormalizer(String name) {
    if ("ExprNormalizer1".equals(name)) {
      return new ExpressionNormalizer1();
    }
    if ("ExprNormalizer2".equals(name)) {
      return new ExpressionNormalizer2();
    }
    throw new IllegalArgumentException("Unknown expression normalizer: " + name);
  }

  private SExpressionBuilder createMockSExpressionBuilder(String name) {
    switch (name) {
      case "Builder1":
        return new Builder1();
      case "Builder2":
        return new Builder2();
      case "Builder3":
        return new Builder3();
      default:
        throw new IllegalArgumentException("Unknown builder: " + name);
    }
  }

  // Mock implementations for testing class-based ordering
  private static class Normalizer1 implements ClauseNormalizer {
    @Override
    public boolean supports(String clauseType) {
      return true;
    }

    @Override
    public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                          QueryStatement.QueryStatementBuilder builder,
                                                          ClauseNormalizeContext context) {
      return builder;
    }
  }

  private static class Normalizer2 implements ClauseNormalizer {
    @Override
    public boolean supports(String clauseType) {
      return true;
    }

    @Override
    public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                          QueryStatement.QueryStatementBuilder builder,
                                                          ClauseNormalizeContext context) {
      return builder;
    }
  }

  private static class Normalizer3 implements ClauseNormalizer {
    @Override
    public boolean supports(String clauseType) {
      return true;
    }

    @Override
    public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                          QueryStatement.QueryStatementBuilder builder,
                                                          ClauseNormalizeContext context) {
      return builder;
    }
  }

  private static class ExpressionNormalizer1 implements RawExpressionNormalizer {
    @Override
    public boolean supports(String clauseType, Class<?> rawExpressionType) {
      return true;
    }

    @Override
    public SExpression<?> normalize(Object rawExpression, ExpressionNormalizeContext context) {
      return null;
    }
  }

  private static class ExpressionNormalizer2 implements RawExpressionNormalizer {
    @Override
    public boolean supports(String clauseType, Class<?> rawExpressionType) {
      return true;
    }

    @Override
    public SExpression<?> normalize(Object rawExpression, ExpressionNormalizeContext context) {
      return null;
    }
  }

  private static class Builder1 implements SExpressionBuilder {
    @Override
    public boolean supports(Operator operator) {
      return true;
    }

    @Override
    public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
      return null;
    }
  }

  private static class Builder2 implements SExpressionBuilder {
    @Override
    public boolean supports(Operator operator) {
      return true;
    }

    @Override
    public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
      return null;
    }
  }

  private static class Builder3 implements SExpressionBuilder {
    @Override
    public boolean supports(Operator operator) {
      return true;
    }

    @Override
    public SExpression<?> build(Operator operator, List<SExpression<?>> params, ExpressionNormalizeContext context) {
      return null;
    }
  }
}
