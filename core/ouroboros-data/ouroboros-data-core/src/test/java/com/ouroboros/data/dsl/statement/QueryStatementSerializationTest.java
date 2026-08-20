package com.ouroboros.data.dsl.statement;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ouroboros.data.dsl.JoinType;
import com.ouroboros.data.dsl.Order;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.normalize.QueryNormalizeContext;
import com.ouroboros.data.normalize.normalizers.PopulateOmitNormalizer;
import org.junit.jupiter.api.Test;

class QueryStatementSerializationTest {

  @SuppressWarnings("unchecked")
  private static <T extends Serializable> T roundTrip(T obj) throws Exception {
    var bos = new ByteArrayOutputStream();
    try (var oos = new ObjectOutputStream(bos)) {
      oos.writeObject(obj);
    }
    try (var ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
      return (T) ois.readObject();
    }
  }

  // --- Test Group 1: QueryStatement basic round-trip ---

  @Test
  void queryStatementBasicRoundTrip() throws Exception {
    var original = QueryStatement.builder()
        .from("users", "u")
        .select(SExpression.create(Operators.FIELD, "name"))
        .where(SExpression.create(Operators.EQ, "age", 18))
        .order("name", Order.ASC)
        .limit(10L)
        .offset(5L)
        .build();

    var d = roundTrip(original);

    assertInstanceOf(QueryStatement.class, d);
    assertEquals(original.size(), d.size());
    assertEquals(original.keySet(), d.keySet());
    // FROM
    assertEquals(original.getFrom().getTableName(), d.getFrom().getTableName());
    assertEquals(original.getFrom().getAlias(), d.getFrom().getAlias());
    // SELECT
    assertEquals(original.getSelect().size(), d.getSelect().size());
    assertEquals(original.getSelect().get(0).getOperator(), d.getSelect().get(0).getOperator());
    // WHERE
    assertEquals(original.getWhere().getOperator(), d.getWhere().getOperator());
    // ORDER
    assertEquals(original.getOrders().size(), d.getOrders().size());
    assertEquals(original.getOrders().get(0).getColumn(), d.getOrders().get(0).getColumn());
    assertEquals(original.getOrders().get(0).getOrder(), d.getOrders().get(0).getOrder());
    // LIMIT / OFFSET
    assertEquals(original.getLimit(), d.getLimit());
    assertEquals(original.getOffset(), d.getOffset());
  }

  // --- Test Group 2: ModelQueryStatement with PopulateClause/OmitClause ---

  @Test
  void modelQueryStatementRoundTrip() throws Exception {
    var builder = new ModelQueryStatementBuilder();
    builder.from("orders", "o");
    builder.populateClause(PopulateClause.fromRaw("field1,field2"));
    builder.omitClause(OmitClause.fromRaw("secret"));
    var original = builder.build();

    var d = roundTrip(original);

    assertInstanceOf(ModelQueryStatement.class, d);
    var dm = (ModelQueryStatement) d;
    // FROM
    assertEquals(original.getFrom().getTableName(), dm.getFrom().getTableName());
    assertEquals(original.getFrom().getAlias(), dm.getFrom().getAlias());
    // PopulateClause
    assertEquals(
        original.getPopulateClause().getEntries().size(),
        dm.getPopulateClause().getEntries().size());
    assertEquals(
        original.getPopulateClause().getEntries().get(0).fieldName(),
        dm.getPopulateClause().getEntries().get(0).fieldName());
    assertEquals(
        original.getPopulateClause().getEntries().get(1).fieldName(),
        dm.getPopulateClause().getEntries().get(1).fieldName());
    // OmitClause
    assertEquals(original.getOmitClause().getFields(), dm.getOmitClause().getFields());
  }

  // --- Test Group 3: Edge cases ---

  @Test
  void minimalQueryStatementRoundTrip() throws Exception {
    var original = QueryStatement.builder()
        .from("t", "t")
        .build();

    var d = roundTrip(original);

    assertInstanceOf(QueryStatement.class, d);
    assertEquals("t", d.getFrom().getTableName());
    assertEquals("t", d.getFrom().getAlias());
  }

  @Test
  void queryStatementWithJoinRoundTrip() throws Exception {
    var on = SExpression.<Boolean>create(Operators.EQ, "u.id", "o.user_id");
    var original = QueryStatement.builder()
        .from("users", "u")
        .innerJoin("orders", "o", on)
        .build();

    var d = roundTrip(original);

    assertEquals(1, d.getJoins().size());
    assertEquals("o", d.getJoins().get(0).getAlias());
    assertEquals("orders", d.getJoins().get(0).getTableName());
    assertEquals(Operators.EQ, d.getJoins().get(0).getOn().getOperator());
  }

  @Test
  void queryStatementWithCTERoundTrip() throws Exception {
    var subQuery = QueryStatement.builder()
        .from("raw_data", "r")
        .build();
    var original = QueryStatement.builder()
        .with("cte", subQuery)
        .from("cte", "cte")
        .build();

    var d = roundTrip(original);

    assertEquals(1, d.getWith().size());
    assertEquals("cte", d.getWith().get(0).getAlias());
    assertEquals("r", d.getWith().get(0).getQuery().getFrom().getAlias());
  }

  @Test
  void queryStatementWithUnionRoundTrip() throws Exception {
    var another = QueryStatement.builder()
        .from("archived_users", "a")
        .build();
    var original = QueryStatement.builder()
        .from("users", "u")
        .union(another)
        .build();

    var d = roundTrip(original);

    assertEquals(1, d.getUnions().size());
    assertEquals("a", d.getUnions().get(0).getQuery().getFrom().getAlias());
    assertFalse(d.getUnions().get(0).isAll());
  }

  @Test
  void rawMapDowngradeCanBeNormalizedWithoutLosingTypedClauses() {
    var recursiveCte = QueryStatement.builder()
        .from("raw_users", "r")
        .build();
    var regularCte = QueryStatement.builder()
        .from("recursive_users", "recent")
        .build();
    var unionAll = QueryStatement.builder()
        .from("archived_users", "a")
        .build();
    var union = QueryStatement.builder()
        .from("deleted_users", "d")
        .build();
    var source = QueryStatement.builder()
        .from("users", "source_users")
        .build();
    var where = SExpression.<Boolean>create(
        Operators.EQ, SExpression.field("u.active"), SExpression.constant(Boolean.TRUE));
    var joinOn = SExpression.<Boolean>create(
        Operators.EQ, SExpression.field("u.id"), SExpression.field("o.user_id"));
    var dependentJoinOn = SExpression.<Boolean>create(
        Operators.EQ, SExpression.field("o.id"), SExpression.field("i.order_id"));
    var unaliasedJoinOn = SExpression.<Boolean>create(
        Operators.EQ, SExpression.field("u.id"), SExpression.field("audit_log.user_id"));
    var having = SExpression.<Boolean>create(
        Operators.GT, SExpression.field("order_count"), SExpression.constant(0));
    var builder = new ModelQueryStatementBuilder();
    builder.withRecursive("recursive_users", recursiveCte);
    builder.with("recent_users", regularCte);
    builder.from(source, "u");
    builder.select(SExpression.field("u.name"));
    builder.where(where);
    builder.leftJoin("orders", "o", joinOn);
    builder.innerJoin("order_items", "i", dependentJoinOn);
    builder.join(new QueryStatement.JoinEntry(
        JoinType.INNERJOIN, "audit_log", null, unaliasedJoinOn));
    builder.crossJoin("events", "e");
    builder.order("u.name", Order.DESC);
    builder.group(SExpression.columns(SExpression.field("u.name")));
    builder.having(having);
    builder.unionAll(unionAll);
    builder.union(union);
    builder.offset(5L);
    builder.limit(10L);
    builder.populateClause(PopulateClause.fromRaw(
        Collections.singletonMap("orders", Collections.singletonMap("limit", 5))));
    builder.omitClause(OmitClause.fromRaw(Collections.singletonList("secret")));
    ModelQueryStatement statement = builder.build();

    Map<String, Object> raw = statement.toRawMap();
    QueryStatement normalized = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .addClauseNormalizer(new PopulateOmitNormalizer())
        .build()
        .normalizeQuery(raw, new ModelQueryStatementBuilder())
        .get();

    assertInstanceOf(Map.class, raw.get("WHERE"));
    assertTrue(normalized.getFrom().isSubQuery());
    assertEquals("u", normalized.getFrom().getAlias());
    assertEquals("users", normalized.getFrom().getSubQuery().getFrom().getTableName());
    assertEquals(where, normalized.getWhere());
    assertEquals(statement.getSelect(), normalized.getSelect());
    assertEquals(4, normalized.getJoins().size());
    assertEquals(JoinType.LEFTJOIN, normalized.getJoins().get(0).getType());
    assertEquals("o", normalized.getJoins().get(0).getAlias());
    assertEquals(joinOn, normalized.getJoins().get(0).getOn());
    assertEquals(JoinType.INNERJOIN, normalized.getJoins().get(1).getType());
    assertEquals("i", normalized.getJoins().get(1).getAlias());
    assertEquals(dependentJoinOn, normalized.getJoins().get(1).getOn());
    assertEquals("audit_log", normalized.getJoins().get(2).getTableName());
    assertNull(normalized.getJoins().get(2).getAlias());
    assertEquals(JoinType.DEFAULT, normalized.getJoins().get(3).getType());
    assertEquals("events", normalized.getJoins().get(3).getTableName());
    assertEquals("e", normalized.getJoins().get(3).getAlias());
    assertTrue(normalized.getJoins().get(3).getOn().isEmpty());
    assertEquals(Order.DESC, normalized.getOrders().get(0).getOrder());
    assertEquals(statement.getGroup(), normalized.getGroup());
    assertEquals(having, normalized.getHaving());
    assertEquals("recursive_users", normalized.getWith().get(0).getAlias());
    assertTrue(normalized.getWith().get(0).isRecursive());
    assertEquals("r", normalized.getWith().get(0).getQuery().getFrom().getAlias());
    assertEquals("recent_users", normalized.getWith().get(1).getAlias());
    assertFalse(normalized.getWith().get(1).isRecursive());
    assertEquals("recent", normalized.getWith().get(1).getQuery().getFrom().getAlias());
    assertTrue(normalized.getUnions().get(0).isAll());
    assertEquals("a", normalized.getUnions().get(0).getQuery().getFrom().getAlias());
    assertFalse(normalized.getUnions().get(1).isAll());
    assertEquals("d", normalized.getUnions().get(1).getQuery().getFrom().getAlias());
    assertEquals(5L, normalized.getOffset());
    assertEquals(10L, normalized.getLimit());
    assertInstanceOf(ModelQueryStatement.class, normalized);
    ModelQueryStatement modelStatement = (ModelQueryStatement) normalized;
    assertEquals(statement.getPopulateClause(), modelStatement.getPopulateClause());
    assertEquals(statement.getOmitClause(), modelStatement.getOmitClause());
  }

  @Test
  void rawMapDowngradeSupportsMixedCanonicalAndRawOrderedClauses() {
    QueryStatement canonicalCte = QueryStatement.builder()
        .from("canonical_source", "canonical_source")
        .build();
    QueryStatement canonicalUnion = QueryStatement.builder()
        .from("canonical_union", "canonical_union")
        .build();
    SExpression<?> canonicalSelect = SExpression.field("u", "name");
    QueryStatement statement = QueryStatement.builder()
        .with("canonical_cte", canonicalCte)
        .from("users", "u")
        .select(canonicalSelect)
        .union(canonicalUnion)
        .build();
    Map<String, Object> rawCteQuery = QueryStatement.builder()
        .from("raw_source", "raw_source")
        .build()
        .toRawMap();
    Map<String, Object> rawUnionQuery = QueryStatement.builder()
        .from("raw_union", "raw_union")
        .build()
        .toRawMap();
    Map<String, Object> raw = statement.toRawMap();
    List<Object> mixedWith = new ArrayList<>((List<?>) raw.get("WITH"));
    mixedWith.add(Collections.singletonMap("raw_cte", rawCteQuery));
    raw.put("WITH", mixedWith);
    List<Object> mixedSelect = new ArrayList<>((List<?>) raw.get("SELECT"));
    mixedSelect.add("u.id");
    raw.put("SELECT", mixedSelect);
    List<Object> mixedUnion = new ArrayList<>((List<?>) raw.get("UNION"));
    mixedUnion.add(rawUnionQuery);
    raw.put("UNION", mixedUnion);

    QueryStatement normalized = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build()
        .normalizeQuery(raw)
        .get();

    assertEquals(2, normalized.getWith().size());
    assertEquals("canonical_cte", normalized.getWith().get(0).getAlias());
    assertEquals("raw_cte", normalized.getWith().get(1).getAlias());
    assertEquals(2, normalized.getSelect().size());
    assertSame(canonicalSelect, normalized.getSelect().get(0));
    assertEquals(SExpression.field("u", "id"), normalized.getSelect().get(1));
    assertEquals(2, normalized.getUnions().size());
    assertEquals("canonical_union", normalized.getUnions().get(0).getQuery().getFrom().getAlias());
    assertEquals("raw_union", normalized.getUnions().get(1).getQuery().getFrom().getAlias());
  }
}
