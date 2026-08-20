package com.ouroboros.data.dsl.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.JoinType;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;

class QueryStatementsTraversalTest {

  @Test
  void rewriteExpressionsShouldCoverAllNormalizedStatementExpressionLocations() {
    QueryStatement statement = buildStatementWithExpressions();
    Set<Object> topLevelPaths = new LinkedHashSet<>();

    QueryStatement rewritten = QueryStatements.rewriteExpressions(statement, path -> {
      if (!path.segments().isEmpty()) {
        topLevelPaths.add(path.segments().get(0));
      }
      if (path.expression().getOperator() == Operators.FIELD
          && "old".equals(path.expression().getParam(0))) {
        return SExpression.field("new");
      }
      return path.expression();
    });

    assertFalse(containsField(rewritten, "old"));
    assertTrue(containsField(rewritten, "new"));
    assertTrue(topLevelPaths.contains(Keyword.SELECT.toString()));
    assertTrue(topLevelPaths.contains(Keyword.WHERE.toString()));
    assertTrue(topLevelPaths.contains(Keyword.GROUP.toString()));
    assertTrue(topLevelPaths.contains(Keyword.HAVING.toString()));
    assertTrue(topLevelPaths.contains(Keyword.JOIN.toString()));
    assertTrue(topLevelPaths.contains(Keyword.WITH.toString()));
    assertTrue(topLevelPaths.contains(Keyword.UNION.toString()));
    assertTrue(topLevelPaths.contains(Keyword.FROM.toString()));
    assertTrue(topLevelPaths.contains(Keyword.POPULATE.toString()));
    assertInstanceOf(ModelQueryStatement.class, rewritten);
    assertEquals(Set.of("secret"), ((ModelQueryStatement) rewritten).getOmitClause().getFields());
  }

  private QueryStatement buildStatementWithExpressions() {
    QueryStatement fromSubquery = QueryStatement.builder()
        .from("from_source", "fs")
        .where(eqOld("from"))
        .build();
    QueryStatement joinSubquery = QueryStatement.builder()
        .from("join_source", "js")
        .where(eqOld("join-subquery"))
        .build();
    QueryStatement withQuery = QueryStatement.builder()
        .from("with_source", "ws")
        .where(eqOld("with"))
        .build();
    QueryStatement unionQuery = QueryStatement.builder()
        .from("union_source", "us")
        .where(eqOld("union"))
        .build();

    ModelQueryStatementBuilder builder = new ModelQueryStatementBuilder();
    builder.select(SExpression.field("old"));
    builder.from(fromSubquery, "root");
    builder.where(eqOld("where"));
    builder.group(SExpression.field("old"));
    builder.having(eqOld("having"));
    builder.join(JoinType.LEFTJOIN, joinSubquery, "j", eqOld("join-on"));
    builder.with("cte", withQuery);
    builder.union(unionQuery);
    builder.populateClause(PopulateClause.fromRaw(List.of(Map.of("child", Map.of("where", eqOld("populate"))))));
    builder.omitClause(OmitClause.fromRaw(List.of("secret")));
    return builder.build();
  }

  private SExpression<Boolean> eqOld(String value) {
    return SExpression.create(Operators.EQ, SExpression.field("old"), SExpression.constant(value));
  }

  private boolean containsField(Object value, String fieldName) {
    if (value instanceof SExpression<?> expression) {
      if (expression.getOperator() == Operators.FIELD && fieldName.equals(expression.getParam(0))) {
        return true;
      }
      return expression.getParams().stream().anyMatch(param -> containsField(param, fieldName));
    }
    if (value instanceof QueryStatement statement) {
      return statement.values().stream().anyMatch(item -> containsField(item, fieldName))
          || containsModelPopulateField(statement, fieldName);
    }
    if (value instanceof QueryStatement.TableSource tableSource && tableSource.isSubQuery()) {
      return containsField(tableSource.getSubQuery(), fieldName);
    }
    if (value instanceof QueryStatement.JoinEntry join) {
      return containsField(join.getOn(), fieldName)
          || (join.isSubQuery() && containsField(join.getSubQuery(), fieldName));
    }
    if (value instanceof QueryStatement.CTEDefinition cte) {
      return containsField(cte.getQuery(), fieldName);
    }
    if (value instanceof QueryStatement.UnionEntry union) {
      return containsField(union.getQuery(), fieldName);
    }
    if (value instanceof Map<?, ?> map) {
      return map.values().stream().anyMatch(item -> containsField(item, fieldName));
    }
    if (value instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        if (containsField(item, fieldName)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean containsModelPopulateField(QueryStatement statement, String fieldName) {
    if (!(statement instanceof ModelQueryStatement modelStatement) || modelStatement.getPopulateClause() == null) {
      return false;
    }
    return modelStatement.getPopulateClause().getEntries().stream()
        .anyMatch(entry -> containsField(entry.options(), fieldName));
  }
}
