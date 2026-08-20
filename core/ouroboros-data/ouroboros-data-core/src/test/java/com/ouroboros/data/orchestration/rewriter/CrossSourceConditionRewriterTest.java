package com.ouroboros.data.orchestration.rewriter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.OrchestrationException;
import com.ouroboros.data.record.RecordList;

/**
 * CrossSourceConditionRewriter 测试
 *
 * <p>Round 3 更新：验证改写结果的正确性
 */
class CrossSourceConditionRewriterTest {

  private OrchestrationContext context;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
  }

  @Test
  void testRewriteWithEmptyResult() {
    // Given: 预查询结果为空，原语句无 WHERE
    RecordList emptyResult = RecordList.empty();
    context.setResult("preQuery", emptyResult);

    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .build();

    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter("preQuery", "userId", "department", "id", 1000);

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: WHERE 应为 EQ(1, 0)
    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertFalse(where.isEmpty());
    assertEquals(Operators.EQ, where.getOperator());
  }

  @Test
  void testRewriteWithSingleValue() {
    // Given: 预查询结果为单个值
    List<Map<String, Object>> data = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 123);
    data.add(record);
    RecordList singleResult = RecordList.of(data);
    context.setResult("preQuery", singleResult);

    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .build();

    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter("preQuery", "userId", "department", "id", 1000);

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: WHERE 应为 EQ(FIELD(userId), constant(123))
    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertFalse(where.isEmpty());
    assertEquals(Operators.EQ, where.getOperator());
  }

  @Test
  void testRewriteWithMultipleValues() {
    // Given: 预查询结果为多个值
    List<Map<String, Object>> data = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      data.add(record);
    }
    RecordList multipleResult = RecordList.of(data);
    context.setResult("preQuery", multipleResult);

    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .build();

    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter("preQuery", "userId", "department", "id", 1000);

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: WHERE 应为 IN(FIELD(userId), ...)
    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertFalse(where.isEmpty());
    assertEquals(Operators.IN, where.getOperator());
  }

  @Test
  void testRewriteWithExistingWhere() {
    // Given: 原语句有 WHERE 条件 + 预查询结果为空
    RecordList emptyResult = RecordList.empty();
    context.setResult("preQuery", emptyResult);

    SExpression<Boolean> existingCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "status"),
        SExpression.constant("active")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(existingCondition)
        .build();

    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter("preQuery", "userId", "department", "id", 1000);

    // When: 执行改写
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: WHERE 应为 AND(原条件, EQ(1, 0))
    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertFalse(where.isEmpty());
    assertEquals(Operators.AND, where.getOperator());
  }

  @Test
  void testPreQueryResultNotFound() {
    // Given: 预查询结果不存在
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .build();

    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter("nonExistent", "userId", "department", "id", 1000);

    // When & Then: 应该抛出异常
    assertThrows(OrchestrationException.class, () -> {
      rewriter.rewrite(statement, context);
    });
  }

  // ========== Round 6: 分批 UNION ALL 测试 ==========

  @Test
  void testRewriteWithExactlyMaxInListSize() {
    // Given: 5 个 ID，maxInListSize=5，应使用 IN 而非 UNION ALL
    List<Map<String, Object>> data = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      data.add(record);
    }
    context.setResult("preQuery", RecordList.of(data));

    QueryStatement statement = QueryStatement.builder().from("user").build();
    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter("preQuery", "userId", "department", "id", 5);

    // When
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: WHERE 为 IN，无 UNION ALL
    assertNotNull(rewritten);
    assertEquals(Operators.IN, rewritten.getWhere().getOperator());
    assertTrue(rewritten.getUnions().isEmpty());
  }

  @Test
  void testRewriteToBatchedUnionWithTwoBatches() {
    // Given: 5 个 ID，maxInListSize=3，应生成 2 批 [1,2,3] + [4,5]
    List<Map<String, Object>> data = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      data.add(record);
    }
    context.setResult("preQuery", RecordList.of(data));

    QueryStatement statement = QueryStatement.builder().from("user").build();
    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter("preQuery", "userId", "department", "id", 3);

    // When
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: 基础查询 WHERE 为 IN，unions 列表大小为 1
    assertNotNull(rewritten);
    assertEquals(Operators.IN, rewritten.getWhere().getOperator());
    assertEquals(1, rewritten.getUnions().size());
    assertTrue(rewritten.getUnions().get(0).isAll());
  }

  @Test
  void testRewriteToBatchedUnionWithThreeBatches() {
    // Given: 7 个 ID，maxInListSize=3，应生成 3 批 [1,2,3] + [4,5,6] + [7]
    List<Map<String, Object>> data = new ArrayList<>();
    for (int i = 1; i <= 7; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      data.add(record);
    }
    context.setResult("preQuery", RecordList.of(data));

    QueryStatement statement = QueryStatement.builder().from("user").build();
    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter("preQuery", "userId", "department", "id", 3);

    // When
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: unions 列表大小为 2
    assertNotNull(rewritten);
    assertEquals(Operators.IN, rewritten.getWhere().getOperator());
    assertEquals(2, rewritten.getUnions().size());
    assertTrue(rewritten.getUnions().get(0).isAll());
    assertTrue(rewritten.getUnions().get(1).isAll());
  }

  @Test
  void testRewriteToBatchedUnionPreservesExistingWhere() {
    // Given: 有现有 WHERE 条件 + 5 个 ID，maxInListSize=3
    List<Map<String, Object>> data = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      Map<String, Object> record = new HashMap<>();
      record.put("id", i);
      data.add(record);
    }
    context.setResult("preQuery", RecordList.of(data));

    SExpression<Boolean> existingCondition = SExpression.create(
        Operators.EQ,
        SExpression.create(Operators.FIELD, "status"),
        SExpression.constant("active")
    );
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(existingCondition)
        .build();

    CrossSourceConditionRewriter rewriter = new CrossSourceConditionRewriter("preQuery", "userId", "department", "id", 3);

    // When
    QueryStatement rewritten = rewriter.rewrite(statement, context);

    // Then: 基础查询 WHERE 为 AND(原条件, IN(...))
    assertNotNull(rewritten);
    assertEquals(Operators.AND, rewritten.getWhere().getOperator());
    assertEquals(1, rewritten.getUnions().size());
  }

  @Test
  void testRewriteToEqualsKeepsStructuredFieldContract() {
    List<Map<String, Object>> data = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 123);
    data.add(record);
    context.setResult("preQuery", RecordList.of(data));

    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .build();

    CrossSourceConditionRewriter rewriter =
        new CrossSourceConditionRewriter("preQuery", "user.departmentId", "department", "id", 1000);

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    assertNotNull(rewritten);
    SExpression<Boolean> where = rewritten.getWhere();
    assertEquals(Operators.EQ, where.getOperator());

    SExpression<?> fieldExpr = where.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, fieldExpr.getOperator());
    assertEquals(2, fieldExpr.getParams().size());
    assertEquals("user", fieldExpr.getParam(0));
    assertEquals("departmentId", fieldExpr.getParam(1));
  }
}
