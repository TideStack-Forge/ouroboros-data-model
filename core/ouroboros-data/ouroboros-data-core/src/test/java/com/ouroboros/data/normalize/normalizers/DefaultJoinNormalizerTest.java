package com.ouroboros.data.normalize.normalizers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.JoinType;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.QueryNormalizeContext;

class DefaultJoinNormalizerTest {

  private final DefaultJoinNormalizer normalizer = new DefaultJoinNormalizer();

  @Test
  void testNormalizeJoinMapUsesProjectOperators() {
    Map<String, Object> joinOn = new LinkedHashMap<>();
    joinOn.put("employee.departmentId", "dept.id");

    Map<String, Object> joinClause = new LinkedHashMap<>();
    joinClause.put("dept", "department");
    joinClause.put("on", joinOn);

    Map<String, Object> clauseData = new LinkedHashMap<>();
    clauseData.put(Keyword.JOIN.toString(), joinClause);

    QueryStatement statement = normalize(clauseData);

    assertEquals(1, statement.getJoins().size());
    QueryStatement.JoinEntry joinEntry = statement.getJoins().get(0);
    assertFalse(joinEntry.getOn().isEmpty());
    assertEquals(Operators.EQ, joinEntry.getOn().getOperator());

    SExpression<?> leftField = joinEntry.getOn().getParamAsSExpression(0);
    SExpression<?> rightField = joinEntry.getOn().getParamAsSExpression(1);
    assertEquals(Operators.FIELD, leftField.getOperator());
    assertEquals(Operators.FIELD, rightField.getOperator());
  }

  @Test
  void testNormalizeJoinListUsesProjectAndOperator() {
    Map<String, Object> firstCondition = new LinkedHashMap<>();
    firstCondition.put("employee.departmentId", "dept.id");
    Map<String, Object> secondCondition = new LinkedHashMap<>();
    secondCondition.put("employee.tenantId", "dept.tenantId");

    Map<String, Object> joinClause = new LinkedHashMap<>();
    joinClause.put("dept", "department");
    joinClause.put("on", Arrays.asList(firstCondition, secondCondition));

    Map<String, Object> clauseData = new LinkedHashMap<>();
    clauseData.put(Keyword.JOIN.toString(), joinClause);

    QueryStatement statement = normalize(clauseData);

    assertEquals(1, statement.getJoins().size());
    SExpression<Boolean> joinCondition = statement.getJoins().get(0).getOn();
    assertEquals(Operators.AND, joinCondition.getOperator());

    SExpression<?> firstExpr = joinCondition.getParamAsSExpression(0);
    SExpression<?> secondExpr = joinCondition.getParamAsSExpression(1);
    assertEquals(Operators.EQ, firstExpr.getOperator());
    assertEquals(Operators.EQ, secondExpr.getOperator());
    assertEquals(Operators.FIELD, firstExpr.getParamAsSExpression(0).getOperator());
    assertEquals(Operators.FIELD, secondExpr.getParamAsSExpression(0).getOperator());
  }

  @Test
  void testNormalizeTopLevelJoinEntryPreservesCanonicalEntry() {
    SExpression<Boolean> joinOn = SExpression.create(
        Operators.EQ,
        SExpression.field("employee.departmentId"),
        SExpression.field("dept.id"));
    QueryStatement.JoinEntry canonicalEntry =
        new QueryStatement.JoinEntry(JoinType.INNERJOIN, "department", "dept", joinOn);
    Map<String, Object> clauseData = new LinkedHashMap<>();
    clauseData.put(Keyword.JOIN.toString(), canonicalEntry);

    QueryStatement statement = normalize(clauseData);

    assertEquals(1, statement.getJoins().size());
    assertSame(canonicalEntry, statement.getJoins().get(0));
  }

  private QueryStatement normalize(Map<String, Object> clauseData) {
    QueryNormalizeContext queryContext = QueryNormalizeContext.builder().build();
    ClauseNormalizeContext clauseContext = new ClauseNormalizeContext(queryContext, "QUERY");
    return normalizer.normalize(
        clauseData,
        QueryStatement.builder().from("employee"),
        clauseContext
    ).build();
  }
}
