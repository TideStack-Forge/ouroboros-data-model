package com.ouroboros.data.dsl.query;

import static com.ouroboros.data.dsl.query.Query.field;
import static com.ouroboros.data.dsl.query.Query.populate;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DataModelQueryBuilderTest {

  @Test
  void buildShouldRenderSelectAndWhereConditionsAsCanonicalRawMap() {
    Map<String, Object> rawMap = Query.from("User")
        .select(field("id"), field("username"))
        .where(
            field("status").eq("ENABLED"),
            field("age").gte(18)
        )
        .build();

    assertEquals("User", rawMap.get("FROM"));
    assertEquals(List.of("id", "username"), rawMap.get("SELECT"));
    assertEquals(Map.of("$and", List.of(
        Map.of("status", "ENABLED"),
        Map.of("age", Map.of("$gte", 18))
    )), rawMap.get("WHERE"));
  }

  @Test
  void whereCollectionShouldUseAndForNonEmptyConditionsAndOmitWhereForEmptyCollection() {
    Map<String, Object> rawMap = Query.from("User")
        .where(Arrays.asList(
            field("status").eq("ENABLED"),
            field("age").gte(18)
        ))
        .build();

    assertEquals(Map.of("$and", List.of(
        Map.of("status", "ENABLED"),
        Map.of("age", Map.of("$gte", 18))
    )), rawMap.get("WHERE"));

    Map<String, Object> emptyWhere = Query.from("User")
        .where(Collections.emptyList())
        .build();

    assertFalse(emptyWhere.containsKey("WHERE"));
  }

  @Test
  void repeatedWhereShouldReplaceAndExplicitAndOrShouldAppend() {
    Map<String, Object> replaced = Query.from("User")
        .where(field("status").eq("DISABLED"))
        .where(field("status").eq("ENABLED"))
        .build();

    assertEquals(Map.of("status", "ENABLED"), replaced.get("WHERE"));

    Map<String, Object> andWhere = Query.from("User")
        .where(field("status").eq("ENABLED"))
        .andWhere(field("age").gte(18))
        .build();

    assertEquals(Map.of("$and", List.of(
        Map.of("status", "ENABLED"),
        Map.of("age", Map.of("$gte", 18))
    )), andWhere.get("WHERE"));

    Map<String, Object> orWhere = Query.from("User")
        .where(field("status").eq("ENABLED"))
        .orWhere(field("status").eq("LOCKED"))
        .build();

    assertEquals(Map.of("$or", List.of(
        Map.of("status", "ENABLED"),
        Map.of("status", "LOCKED")
    )), orWhere.get("WHERE"));
  }

  @Test
  void whereShouldFailFastForNullConditions() {
    DataModelQuery query = Query.from("User");

    assertThrows(IllegalArgumentException.class, () -> query.where((QueryCondition[]) null));
    assertThrows(IllegalArgumentException.class, () -> query.where(field("status").eq("ENABLED"), null));
    assertThrows(IllegalArgumentException.class, () -> query.where((List<QueryCondition>) null));
    assertThrows(IllegalArgumentException.class, () -> query.where(Arrays.asList(
        field("status").eq("ENABLED"),
        null
    )));
  }

  @Test
  void selectShouldPreserveStringListAndMapForms() {
    Map<String, Object> stringSelect = Query.from("User")
        .select("id, name as userName, age")
        .build();

    assertEquals("id, name as userName, age", stringSelect.get("SELECT"));

    Map<String, Object> listSelect = Query.from("User")
        .select(List.of("id", Map.of("userName", "name")))
        .build();

    assertEquals(List.of("id", Map.of("userName", "name")), listSelect.get("SELECT"));

    Map<String, Object> arraySelect = Query.from("User")
        .select(new String[] {"id", "name"})
        .build();

    assertEquals(List.of("id", "name"), arraySelect.get("SELECT"));

    Map<String, Object> mixedArraySelect = Query.from("User")
        .select(new Object[] {field("id"), Map.of("userName", field("name"))})
        .build();

    assertEquals(List.of("id", Map.of("userName", "name")), mixedArraySelect.get("SELECT"));

    Map<String, Object> mapSelect = Query.from("User")
        .select(Map.of("userName", "name", "userAge", "age"))
        .build();

    assertEquals(Map.of("userName", "name", "userAge", "age"), mapSelect.get("SELECT"));

    Map<String, Object> mapSelectWithExpression = Query.from("User")
        .select(Map.of("userName", field("name")))
        .build();

    assertEquals(Map.of("userName", "name"), mapSelectWithExpression.get("SELECT"));
  }

  @Test
  void whereShouldPreserveMapListAndAppendForms() {
    Map<String, Object> rawMapWhere = Query.from("User")
        .where(Map.of("status", List.of("ENABLED", "LOCKED")))
        .build();

    assertEquals(Map.of("status", List.of("ENABLED", "LOCKED")), rawMapWhere.get("WHERE"));

    Map<String, Object> rawListWhere = Query.from("User")
        .where(List.of(
            Map.of("status", "ENABLED"),
            Map.of("age", Map.of("$gte", 18))
        ))
        .build();

    assertEquals(List.of(
        Map.of("status", "ENABLED"),
        Map.of("age", Map.of("$gte", 18))
    ), rawListWhere.get("WHERE"));

    Map<String, Object> appendedWhere = Query.from("User")
        .where(field("status").eq("ENABLED"))
        .andWhere(Map.of("age", Map.of("$gte", 18)))
        .orWhere(Map.of("status", "LOCKED"))
        .build();

    assertEquals(Map.of("$or", List.of(
        Map.of("$and", List.of(
            Map.of("status", "ENABLED"),
            Map.of("age", Map.of("$gte", 18))
        )),
        Map.of("status", "LOCKED")
    )), appendedWhere.get("WHERE"));
  }

  @Test
  void whereShouldRenderExpressionRightValuesAsRawExpressions() {
    Map<String, Object> rawMap = Query.from("User")
        .where(field("age").gte(field("minAge")))
        .build();

    assertEquals(Map.of("age", Map.of("$gte", List.of("FIELD", "minAge"))),
        rawMap.get("WHERE"));
  }

  @Test
  void rawWhereMapsShouldRenderExpressionValuesAsRawExpressions() {
    Map<String, Object> operatorWhere = Query.from("User")
        .where(Map.of("age", Map.of("$gte", field("minAge"))))
        .build();

    assertEquals(Map.of("age", Map.of("$gte", List.of("FIELD", "minAge"))),
        operatorWhere.get("WHERE"));

    Map<String, Object> aliasOperatorWhere = Query.from("User")
        .where(Map.of("age", Map.of("GT", field("minAge"))))
        .build();

    assertEquals(Map.of("age", Map.of("GT", List.of("FIELD", "minAge"))),
        aliasOperatorWhere.get("WHERE"));

    Map<String, Object> equalityWhere = Query.from("User")
        .where(Map.of("age", field("minAge")))
        .build();

    assertEquals(Map.of("age", Map.of("$eq", List.of("FIELD", "minAge"))),
        equalityWhere.get("WHERE"));

    Map<String, Object> clauseWhere = Query.from("User")
        .clause("where", Map.of("age", Map.of("$lte", field("maxAge"))))
        .build();

    assertEquals(Map.of("age", Map.of("$lte", List.of("FIELD", "maxAge"))),
        clauseWhere.get("WHERE"));
  }

  @Test
  void clauseShouldPreserveOtherQueryClausesAndNormalizeAliases() {
    Map<String, Object> rawMap = Query.from("User")
        .clause("distinct", "1")
        .clause("order", "name asc, createdAt desc")
        .clause("limit", 10)
        .clause("skip", 20)
        .clause("groupBy", List.of("status"))
        .clause("having", List.of(">", List.of("count", List.of("*")), 5))
        .clause("omit", "password, secret")
        .clause("leftJoin", Map.of("Department", Map.of("on", Map.of("departmentId", "id"))))
        .clause("unionAll", List.of(Map.of("select", List.of("name"), "from", "Department")))
        .clause("withRecursive", Map.of("tree", Map.of("select", List.of("id"), "from", "Department")))
        .build();

    assertEquals("1", rawMap.get("DISTINCT"));
    assertEquals("name asc, createdAt desc", rawMap.get("ORDER"));
    assertEquals(10, rawMap.get("LIMIT"));
    assertEquals(20, rawMap.get("OFFSET"));
    assertEquals(List.of("status"), rawMap.get("GROUP"));
    assertEquals(List.of(">", List.of("count", List.of("*")), 5), rawMap.get("HAVING"));
    assertEquals("password, secret", rawMap.get("OMIT"));
    assertEquals(Map.of("Department", Map.of("on", Map.of("departmentId", "id"))), rawMap.get("LEFTJOIN"));
    assertEquals(List.of(Map.of("select", List.of("name"), "from", "Department")), rawMap.get("UNIONALL"));
    assertEquals(Map.of("tree", Map.of("select", List.of("id"), "from", "Department")), rawMap.get("WITHRECURSIVE"));
  }

  @Test
  void clausesShouldApplyKnownQueryClauseAliasesInBulk() {
    Map<String, Object> rawMap = Query.from("User")
        .clauses(Map.of(
            "pageNum", 3,
            "perPage", 10,
            "order", Map.of("createdAt", "desc")
        ))
        .build();

    assertEquals(3, rawMap.get("PAGE"));
    assertEquals(10, rawMap.get("PAGESIZE"));
    assertEquals(Map.of("createdAt", "desc"), rawMap.get("ORDER"));
  }

  @Test
  void clauseShouldRejectNonQueryDslKeywords() {
    DataModelQuery query = Query.from("User");

    assertThrows(IllegalArgumentException.class, () -> query.clause("insert", Map.of("name", "Alice")));
    assertThrows(IllegalArgumentException.class, () -> query.clause("update", Map.of("name", "Alice")));
    assertThrows(IllegalArgumentException.class, () -> query.clause("delete", true));
    assertThrows(IllegalArgumentException.class, () -> query.clause("set", Map.of("name", "Alice")));
    assertThrows(IllegalArgumentException.class, () -> query.clause("as", "alias"));
  }

  @Test
  void populateShouldRenderStringListRawMapAndNestedSpecForms() {
    Map<String, Object> listPopulate = Query.from("Order")
        .populate("user", "orderItems")
        .build();

    assertEquals(List.of("user", "orderItems"), listPopulate.get("POPULATE"));

    Map<String, Object> stringPopulate = Query.from("Order")
        .populate("user, orderItems")
        .build();

    assertEquals("user, orderItems", stringPopulate.get("POPULATE"));

    Map<String, Object> rawConfig = new LinkedHashMap<>();
    rawConfig.put("SELECT", List.of("id", "name"));
    rawConfig.put("WHERE", Map.of("status", "active"));
    rawConfig.put("LIMIT", 10);

    Map<String, Object> mapPopulate = Query.from("Order")
        .populate(Map.of("user", rawConfig))
        .build();

    assertEquals(Map.of("user", rawConfig), mapPopulate.get("POPULATE"));

    Map<String, Object> specPopulate = Query.from("Order")
        .populate(populate("user")
            .select("id", "name")
            .where(field("status").eq("active"))
            .limit(10)
            .populate(populate("department")
                .where(field("code").eq("TECH"))))
        .build();

    assertEquals(Map.of("user", Map.of(
        "SELECT", List.of("id", "name"),
        "WHERE", Map.of("status", "active"),
        "LIMIT", 10,
        "POPULATE", Map.of("department", Map.of("WHERE", Map.of("code", "TECH")))
    )), specPopulate.get("POPULATE"));
  }

  @Test
  void populateWhereShouldRenderExpressionValuesAsRawExpressions() {
    Map<String, Object> rawMap = Query.from("Order")
        .populate(populate("user")
            .where(Map.of("age", Map.of("$gte", field("minAge")))))
        .build();

    assertEquals(Map.of("user", Map.of(
        "WHERE", Map.of("age", Map.of("$gte", List.of("FIELD", "minAge")))
    )), rawMap.get("POPULATE"));
  }
}
