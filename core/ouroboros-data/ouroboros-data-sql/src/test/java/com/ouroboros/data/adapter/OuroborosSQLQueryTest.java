package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.transpile.DefaultOuroborosQueryMetadata;
import com.querydsl.core.JoinType;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.RelationalPathBase;

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
}
