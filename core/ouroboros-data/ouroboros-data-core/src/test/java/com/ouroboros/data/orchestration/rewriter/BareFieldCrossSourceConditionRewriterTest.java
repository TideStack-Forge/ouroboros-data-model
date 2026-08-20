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
import com.ouroboros.data.record.RecordList;

class BareFieldCrossSourceConditionRewriterTest {

  private OrchestrationContext context;

  @BeforeEach
  void setUp() {
    context = new OrchestrationContext();
  }

  @Test
  void testRewritePreservesOrStructure() {
    List<Map<String, Object>> data = new ArrayList<>();
    Map<String, Object> record = new HashMap<>();
    record.put("id", 7);
    data.add(record);
    context.setResult("preQuery", RecordList.of(data));

    SExpression<Boolean> where = SExpression.create(
        Operators.OR,
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "project", "name"),
            SExpression.constant("Alpha")
        ),
        SExpression.create(
            Operators.EQ,
            SExpression.create(Operators.FIELD, "status"),
            SExpression.constant("active")
        )
    );
    QueryStatement statement = QueryStatement.builder()
        .from("employee")
        .where(where)
        .build();

    BareFieldCrossSourceConditionRewriter rewriter = new BareFieldCrossSourceConditionRewriter(
        "preQuery",
        "projectId",
        "project",
        "id",
        1000
    );

    QueryStatement rewritten = rewriter.rewrite(statement, context);

    assertEquals(Operators.OR, rewritten.getWhere().getOperator());
    SExpression<?> leftExpr = rewritten.getWhere().getParamAsSExpression(0);
    assertEquals(Operators.EQ, leftExpr.getOperator());
    SExpression<?> rewrittenLeftField = leftExpr.getParamAsSExpression(0);
    assertEquals(Operators.FIELD, rewrittenLeftField.getOperator(),
        "左支应整体替换为本地字段条件，不能把 FIELD 子节点错误替换成布尔表达式");
    assertNotEquals(
        SExpression.create(Operators.FIELD, "project", "name"),
        rewrittenLeftField,
        "左支应被替换为本地字段条件，而不是保留原始 relation 字段");
    SExpression<?> rightExpr = rewritten.getWhere().getParamAsSExpression(1);
    assertEquals(Operators.EQ, rightExpr.getOperator());
  }
}
