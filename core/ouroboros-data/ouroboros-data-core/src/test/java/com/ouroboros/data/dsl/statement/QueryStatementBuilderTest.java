package com.ouroboros.data.dsl.statement;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;

class QueryStatementBuilderTest {

  @Test
  void selectShouldReplaceExistingSelectList() {
    QueryStatement original = QueryStatement.builder()
        .from("user")
        .select(SExpression.create(Operators.FIELD, "id"))
        .build();

    QueryStatement rewritten = original.getBuilder()
        .select(SExpression.create(Operators.FIELD, "name"))
        .build();

    assertEquals(1, rewritten.getSelect().size());
    assertEquals(Operators.FIELD, rewritten.getSelect().get(0).getOperator());
    assertEquals("name", rewritten.getSelect().get(0).getParam(0));
  }
}
