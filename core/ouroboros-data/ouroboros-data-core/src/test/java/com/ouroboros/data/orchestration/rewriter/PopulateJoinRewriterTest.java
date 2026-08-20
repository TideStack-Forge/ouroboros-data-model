package com.ouroboros.data.orchestration.rewriter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;

class PopulateJoinRewriterTest {

  @Test
  void rewriteShouldAppendPopulateSelectFieldsInsteadOfReplacingExistingSelects() {
    QueryStatement statement = QueryStatement.builder()
        .from("user", "u")
        .select(SExpression.create(Operators.FIELD, "id"))
        .build();

    PopulateJoinRewriter rewriter = new PopulateJoinRewriter(
        "department",
        "Department",
        Arrays.asList("id", "name"),
        "departmentId",
        "id"
    );

    QueryStatement rewritten = rewriter.rewrite(statement, new OrchestrationContext());

    assertEquals(1, rewritten.getJoins().size());
    assertEquals("Department", rewritten.getJoins().get(0).getTableName());
    assertEquals("department", rewritten.getJoins().get(0).getAlias());
    assertEquals(3, rewritten.getSelect().size());
    assertEquals(Operators.FIELD, rewritten.getSelect().get(0).getOperator());
    assertEquals("id", rewritten.getSelect().get(0).getParam(0));

    assertEquals(Operators.ALIAS, rewritten.getSelect().get(1).getOperator());
    assertEquals(Operators.FIELD, rewritten.getSelect().get(1).getParamAsSExpression(0).getOperator());
    assertEquals("department__id", rewritten.getSelect().get(1).getParam(1));

    assertEquals(Operators.ALIAS, rewritten.getSelect().get(2).getOperator());
    assertEquals(Operators.FIELD, rewritten.getSelect().get(2).getParamAsSExpression(0).getOperator());
    assertEquals("department__name", rewritten.getSelect().get(2).getParam(1));

    SExpression<?> on = rewritten.getJoins().get(0).getOn();
    assertEquals(Operators.EQ, on.getOperator());
    assertEquals(SExpression.field("u", "departmentId"), on.getParam(0),
        "populate JOIN 的本表外键应显式绑定主表 alias，避免自关联时被解析到 join 别名");
    assertEquals(SExpression.field("department", "id"), on.getParam(1));
  }
}
