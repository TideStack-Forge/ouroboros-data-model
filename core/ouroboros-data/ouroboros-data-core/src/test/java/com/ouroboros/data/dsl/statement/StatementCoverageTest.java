package com.ouroboros.data.dsl.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.JoinType;
import com.ouroboros.data.dsl.Order;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;

class StatementCoverageTest {

  @Test
  void statementWrapsMapAndIsReadOnly() {
    var meta = new LinkedHashMap<String, Object>();
    meta.put("name", "user");
    meta.put("count", 1);

    var statement = new Statement(meta);
    var copied = new Statement(statement);

    assertEquals(2, statement.size());
    assertFalse(statement.isEmpty());
    assertTrue(statement.containsKey("name"));
    assertTrue(statement.containsValue(1));
    assertEquals("user", statement.get("name"));
    assertEquals(statement.keySet(), copied.keySet());
    assertEquals(statement.values(), copied.values());
    assertEquals(statement.entrySet(), copied.entrySet());

    assertThrows(UnsupportedOperationException.class, () -> statement.put("x", 1));
    assertThrows(UnsupportedOperationException.class, () -> statement.remove("name"));
    assertThrows(UnsupportedOperationException.class, () -> statement.putAll(Collections.singletonMap("x", 1)));
    assertThrows(UnsupportedOperationException.class, statement::clear);
  }

  @Test
  void dmlStatementsExposeEntityValuesAndWhere() {
    var values = new LinkedHashMap<String, Object>();
    values.put("name", "alice");
    SExpression<Boolean> where = SExpression.create(Operators.EQ, "id", 1);

    var insert = InsertStatement.of("user", values);
    assertEquals("user", insert.getEntityName());
    assertEquals(values, insert.getValues());
    assertTrue(new InsertStatement(Collections.<String, Object>emptyMap()).getValues().isEmpty());

    var update = UpdateStatement.of("user", values);
    assertEquals("user", update.getEntityName());
    assertEquals(values, update.getValues());
    assertTrue(update.getWhere().isEmpty());

    var updateWithWhere = UpdateStatement.of("user", values, where);
    assertSame(where, updateWithWhere.getWhere());
    assertTrue(new UpdateStatement(Collections.<String, Object>emptyMap()).getValues().isEmpty());

    var delete = DeleteStatement.of("user", where);
    assertEquals("user", delete.getEntityName());
    assertSame(where, delete.getWhere());
    assertNull(new DeleteStatement(Collections.<String, Object>emptyMap()).getEntityName());
    assertTrue(new DeleteStatement(Collections.<String, Object>emptyMap()).getWhere().isEmpty());

    var batchInsert = BatchInsertStatement.of("user", Collections.<Map<String, ?>>singletonList(values));
    assertEquals("user", batchInsert.getEntityName());
    assertEquals(1, batchInsert.getValuesList().size());
    assertTrue(new BatchInsertStatement(Collections.<String, Object>emptyMap()).getValuesList().isEmpty());

    var dmlStatement = new DMLStatement(Collections.<String, Object>singletonMap("x", 1));
    assertEquals(1, dmlStatement.size());
  }

  @Test
  void queryStatementValueObjectsExposeState() {
    var subQuery = QueryStatement.builder().from("orders", "o").build();
    SExpression<Boolean> on = SExpression.create(Operators.EQ, "u.id", "o.userId");

    var tableSource = new QueryStatement.TableSource("users", null);
    assertFalse(tableSource.isSubQuery());
    assertEquals("users", tableSource.getTableName());
    assertNull(tableSource.getSubQuery());
    assertEquals("users", tableSource.getName());

    var subQuerySource = new QueryStatement.TableSource(subQuery, "sq");
    assertTrue(subQuerySource.isSubQuery());
    assertSame(subQuery, subQuerySource.getSubQuery());
    assertNull(subQuerySource.getTableName());
    assertEquals("sq", subQuerySource.getName());

    var tableJoin = new QueryStatement.JoinEntry(JoinType.INNERJOIN, "users", "u", null);
    assertEquals(JoinType.INNERJOIN, tableJoin.getType());
    assertEquals("u", tableJoin.getAlias());
    assertEquals("users", tableJoin.getTableName());
    assertTrue(tableJoin.getOn().isEmpty());
    assertFalse(tableJoin.isSubQuery());

    var subQueryJoin = new QueryStatement.JoinEntry(JoinType.LEFTJOIN, subQuery, "sq", on);
    assertTrue(subQueryJoin.isSubQuery());
    assertSame(subQuery, subQueryJoin.getSubQuery());
    assertSame(on, subQueryJoin.getOn());
    assertNull(subQueryJoin.getTableName());

    var sourceJoin = new QueryStatement.JoinEntry(JoinType.DEFAULT, subQuerySource, null);
    assertEquals("sq", sourceJoin.getAlias());
    assertSame(subQuery, sourceJoin.getOrigin());

    var unionEntry = new QueryStatement.UnionEntry(subQuery, true);
    assertSame(subQuery, unionEntry.getQuery());
    assertTrue(unionEntry.isAll());

    var ascendingOrder = new QueryStatement.OrderEntry("name", Order.ASC);
    assertEquals("name", ascendingOrder.getColumn());
    assertEquals(Order.ASC, ascendingOrder.getOrder());

    var descendingOrder = new QueryStatement.OrderEntry("name", "desc");
    assertEquals(Order.DESC, descendingOrder.getOrder());

    var defaultAscendingOrder = new QueryStatement.OrderEntry("name", "anything");
    assertEquals(Order.ASC, defaultAscendingOrder.getOrder());

    var cteDefinition = new QueryStatement.CTEDefinition(subQuery, "cte", true);
    assertEquals("cte", cteDefinition.getAlias());
    assertSame(subQuery, cteDefinition.getQuery());
    assertTrue(cteDefinition.isRecursive());

    var builder = new ModelQueryStatementBuilder();
    builder.from("users", "u");
    builder.populateClause(PopulateClause.fromRaw("profile"));
    builder.omitClause(OmitClause.fromRaw("password"));
    var builtModelQuery = builder.build();
    assertEquals("u", builtModelQuery.getFrom().getAlias());
    assertNotNull(builtModelQuery.getPopulateClause());
    assertTrue(builtModelQuery.getOmitClause().getFields().contains("password"));

    var copiedBuilder = new ModelQueryStatementBuilder(builtModelQuery);
    var copiedModelQuery = copiedBuilder.build();
    assertEquals(builtModelQuery, copiedModelQuery);
    assertEquals(builtModelQuery.hashCode(), copiedModelQuery.hashCode());
  }

  @Test
  void queryStatementBuilderCoversJoinWithPopulateOmitAndMergeBranches() {
    QueryStatement sub = QueryStatement.builder().from("orders", "o").build();
    SExpression<Boolean> on = SExpression.create(Operators.EQ, "u.id", "o.userId");

    var leftBuilder = QueryStatement.builder()
        .from("users as u")
        .distinct(true)
        .where(null)
        .group(SExpression.create(Operators.FIELD, "u.id"))
        .having(SExpression.create(Operators.EQ, 1, 1))
        .offset(2)
        .limit(10)
        .with("cte_users", sub)
        .withRecursive("cte_orders", sub)
        .join(new QueryStatement.JoinEntry(JoinType.INNERJOIN, "orders", "o1", on))
        .join(Collections.singletonList(new QueryStatement.JoinEntry(JoinType.LEFTJOIN, sub, "o2", on)))
        .join(JoinType.RIGHTJOIN, "orders", "o3", on)
        .join(JoinType.FULLJOIN, sub, "o4", on)
        .innerJoin("orders", "o5", on)
        .leftJoin(sub, "o6", on)
        .rightJoin("orders", "o7", on)
        .fullJoin(sub, "o8", on)
        .crossJoin("orders", "o9")
        .crossJoin(sub, "o10")
        .order("name", Order.DESC)
        .union(sub)
        .unionAll(sub)
        .populate("profile", "department")
        .putRawPopulate(Arrays.asList("x", "y"))
        .putRawOmit(Arrays.asList("password"))
        .replaceSelect(Arrays.asList(
            SExpression.create(Operators.FIELD, "u.id"),
            SExpression.create(Operators.FIELD, "u.name")));

    var rightBuilder = QueryStatement.builder()
        .select(SExpression.create(Operators.FIELD, "x"))
        .addSelect(SExpression.create(Operators.FIELD, "y"));

    var statement = leftBuilder
        .merge(rightBuilder)
        .build();

    assertEquals("u", statement.getFrom().getAlias());
    assertTrue(statement.getDistinct());
    assertTrue(statement.getWhere().isEmpty());
    assertEquals(10L, statement.getLimit());
    assertEquals(2L, statement.getOffset());
    assertEquals(2, statement.getWith().size());
    assertEquals(10, statement.getJoins().size());
    assertEquals(1, statement.getOrders().size());
    assertEquals(2, statement.getUnions().size());
    assertTrue(statement.getUnions().get(1).isAll());
    assertEquals(Arrays.asList("x", "y"), statement.getPopulate());
    assertEquals(Collections.singletonList("password"), statement.getOmit());
    assertEquals(2, statement.getSelect().size());

    var replaced = statement.getBuilder().replaceJoins(Collections.emptyList()).build();
    assertTrue(replaced.getJoins().isEmpty());

    var replacedWithOneJoin = statement.getBuilder()
        .replaceJoins(Collections.singletonList(new QueryStatement.JoinEntry(JoinType.INNERJOIN, "t", "t", on)))
        .build();
    assertEquals(1, replacedWithOneJoin.getJoins().size());

    var copied = statement.getBuilder().build();
    assertEquals(statement, copied);
    assertEquals(statement.hashCode(), copied.hashCode());
    assertFalse(statement.equals("not-statement"));
  }

  @Test
  void queryStatementPopulateAndOmitValidateRawTypes() {
    var badPopulate = QueryStatement.builder()
        .from("users", "u")
        .putRawPopulate(Collections.singletonList(Integer.valueOf(1)))
        .build();
    assertThrows(IllegalStateException.class, badPopulate::getPopulate);

    var badPopulateType = QueryStatement.builder()
        .from("users", "u")
        .putRawPopulate("profile")
        .build();
    assertThrows(IllegalStateException.class, badPopulateType::getPopulate);

    var badOmit = QueryStatement.builder()
        .from("users", "u")
        .putRawOmit(Collections.singletonList(Integer.valueOf(1)))
        .build();
    assertThrows(IllegalStateException.class, badOmit::getOmit);

    var badOmitType = QueryStatement.builder()
        .from("users", "u")
        .putRawOmit(Boolean.TRUE)
        .build();
    assertThrows(IllegalStateException.class, badOmitType::getOmit);
  }
}
