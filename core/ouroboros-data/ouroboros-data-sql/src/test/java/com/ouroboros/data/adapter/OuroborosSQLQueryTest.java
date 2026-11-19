package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.transpile.DefaultOuroborosQueryMetadata;
import com.querydsl.core.JoinType;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.core.QueryModifiers;

@DisplayName("OuroborosSQLQuery 测试")
class OuroborosSQLQueryTest {

  @Test
  @DisplayName("带 FROM 的主查询应用 UNION 时不应触发 QueryDSL 'mix union and from' 异常")
  void shouldAllowUnionMetadataAfterMainFrom() {
    DefaultOuroborosQueryMetadata main = new DefaultOuroborosQueryMetadata();
    main.addJoin(JoinType.DEFAULT, new RelationalPathBase<>(Object.class, "users", "", "users"));
    main.setProjection(Expressions.stringPath("name"));

    DefaultOuroborosQueryMetadata union = new DefaultOuroborosQueryMetadata();
    union.addJoin(JoinType.DEFAULT, new RelationalPathBase<>(Object.class, "departments", "", "departments"));
    union.setProjection(Expressions.stringPath("name"));
    main.addUnion(union);

    String sql = assertDoesNotThrow(
        () -> new OuroborosSQLQuery(H2Templates.builder().build(), main).getSQL().getSQL(),
        "UNION 应在 SQL 构造与序列化阶段被安全应用，而不是因为 metadata 已有 FROM 直接抛错");

    assertTrue(sql.toLowerCase().contains("union"), "生成的 SQL 应包含 UNION 语义");
  }

  @Test
  void mixedUnionKeepsDuplicatesOrderingPaginationAndBoundValues() throws Exception {
    var main = constantRow(1);
    main.addUnion(constantRow(2));
    main.addUnionAll(constantRow(2));
    main.addOrderBy(Expressions.numberPath(Integer.class, "n").asc());
    main.setModifiers(new QueryModifiers(2L, 1L));
    for (var templates : List.of(H2Templates.builder().build(), MySQLTemplates.builder().build(), PostgreSQLTemplates.builder().build())) {
      SQLBindings sql = new OuroborosSQLQuery(templates, main).getSQL();
      assertTrue(sql.getSQL().toLowerCase().contains("union all"));
      assertTrue(sql.getSQL().toLowerCase().contains("order by"));
      assertEquals(List.of(2, 2), execute(sql));
    }
  }

  @Test
  void cteAndUnionUseTheSameSerializerAndBindingPath() throws Exception {
    var main = new DefaultOuroborosQueryMetadata();
    var cte = new RelationalPathBase<>(Object.class, "cte_values", "", "cte_values");
    main.addWith(cte, constantRow(7));
    main.addJoin(JoinType.DEFAULT, cte);
    main.setProjection(Expressions.numberPath(Integer.class, "n"));
    main.addUnionAll(constantRow(9));
    assertEquals(List.of(7, 9), execute(new OuroborosSQLQuery(H2Templates.builder().build(), main).getSQL()));
  }

  @Test
  void cteWithPaginatedUnionKeepsCteAtTheOuterQuery() throws Exception {
    var main = new DefaultOuroborosQueryMetadata();
    var cte = new RelationalPathBase<>(Object.class, "cte_values", "", "cte_values");
    main.addWith(cte, constantRow(7));
    main.addJoin(JoinType.DEFAULT, cte);
    main.setProjection(Expressions.numberPath(Integer.class, "n"));
    main.addUnionAll(constantRow(9));
    main.addOrderBy(Expressions.numberPath(Integer.class, "n").asc());
    main.setModifiers(new QueryModifiers(1L, 1L));
    assertEquals(List.of(9), execute(new OuroborosSQLQuery(H2Templates.builder().build(), main).getSQL()));
  }

  @Test
  void inheritedUnionApiSupportsPaginationAndLiteralSerialization() throws Exception {
    var templates = H2Templates.builder().build();
    var query = new OuroborosSQLQuery(templates);
    query.unionAll(new OuroborosSQLQuery(templates, constantRow(1)), new OuroborosSQLQuery(templates, constantRow(2)));
    query.orderBy(Expressions.numberPath(Integer.class, "n").asc()).limit(1).offset(1);
    query.setUseLiterals(true);
    SQLBindings sql = query.getSQL();
    assertTrue(sql.getNullFriendlyBindings().isEmpty());
    assertEquals(List.of(2), execute(sql));
  }

  private DefaultOuroborosQueryMetadata constantRow(int value) {
    var metadata = new DefaultOuroborosQueryMetadata();
    metadata.setProjection(Expressions.as(Expressions.numberTemplate(Integer.class, "cast({0} as integer)", value), Expressions.numberPath(Integer.class, "n")));
    return metadata;
  }

  private List<Integer> execute(SQLBindings sql) throws Exception {
    try (var connection = DriverManager.getConnection("jdbc:h2:mem:union_adapter_contract");
         var statement = connection.prepareStatement(sql.getSQL())) {
      var bindings = sql.getNullFriendlyBindings();
      for (int i = 0; i < bindings.size(); i++) statement.setObject(i + 1, bindings.get(i));
      try (var result = statement.executeQuery()) {
        var values = new ArrayList<Integer>();
        while (result.next()) values.add(result.getInt(1));
        return values;
      }
    }
  }
}
