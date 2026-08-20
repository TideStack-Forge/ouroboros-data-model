package com.ouroboros.data.model.meta;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.annotation.Field;
import com.ouroboros.data.annotation.Model;
import com.ouroboros.data.annotation.PrimaryKey;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.query.Query;
import com.ouroboros.data.normalize.QueryNormalizeContext;

class TypedModelMetaContractTest {

  @Test
  void handwrittenMetaShouldBuildCanonicalRawMap() {
    UserMeta user = UserMeta.user;

    Map<String, Object> rawMap = Query.from(user)
        .where(user.status.eq(UserStatus.ENABLED))
        .select(user.id, user.username.as("userName"), user.status)
        .build();

    assertEquals(Map.of("user", "User"), rawMap.get("FROM"));
    assertEquals(List.of("user.id", Map.of("userName", "user.username"), "user.status"),
        rawMap.get("SELECT"));
    assertEquals(Map.of("user.status", "ENABLED"), rawMap.get("WHERE"));
  }

  @Test
  void handwrittenMetaEnumConditionsShouldNormalizeFromBuiltRawMap() {
    UserMeta user = UserMeta.user;
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    var result = context.normalizeQuery(Query.from(user)
        .where(user.status.eq(UserStatus.ENABLED))
        .build());

    assertTrue(result.isSuccess(), () -> "Meta enum 条件应可从 builder raw map 归一化: "
        + (result.isFailure() ? result.getCause() : ""));
    SExpression<Boolean> where = result.get().getWhere();
    assertEquals(Operators.EQ, where.getOperator());
    assertField(where.getParamAsSExpression(0), "user", "status");
    assertEquals("ENABLED", where.getParamAsSExpression(1).getParam(0));
  }

  @Test
  void handwrittenMetaFieldsShouldNormalizeInsideRawQueryExpressions() {
    UserMeta user = UserMeta.user;
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    Map<String, Object> rawQuery = Map.of(
        "FROM", user,
        "SELECT", List.of(user.id, user.username.as("userName")),
        "WHERE", Map.of("user.username", Map.of("$eq", user.id))
    );
    var result = context.normalizeQuery(rawQuery);

    assertTrue(result.isSuccess(), () -> "Meta 字段应可在 raw expression 中归一化: "
        + (result.isFailure() ? result.getCause() : ""));

    SExpression<?> columns = result.get().getSelect().get(0);
    assertEquals("User", result.get().getFrom().getTableName());
    assertEquals("user", result.get().getFrom().getAlias());
    assertEquals(Operators.COLUMNS, columns.getOperator());
    assertField(columns.getParamAsSExpression(0), "user", "id");

    SExpression<?> alias = columns.getParamAsSExpression(1);
    assertEquals(Operators.ALIAS, alias.getOperator());
    assertEquals("userName", alias.getParam(1));
    assertEquals("user", alias.getParamAsSExpression(0).getParam(0));
    assertEquals("username", alias.getParamAsSExpression(0).getParam(1));

    SExpression<Boolean> where = result.get().getWhere();
    assertEquals(Operators.EQ, where.getOperator());
    assertEquals("user", where.getParamAsSExpression(0).getParam(0));
    assertEquals("username", where.getParamAsSExpression(0).getParam(1));
    assertEquals("user", where.getParamAsSExpression(1).getParam(0));
    assertEquals("id", where.getParamAsSExpression(1).getParam(1));
  }

  @Test
  void aliasedMetaFieldsShouldNormalizeWithAliasInsideRawQueryExpressions() {
    UserMeta user = UserMeta.user.as("u");
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    assertEquals("u.username", user.username.toRawValue());

    Map<String, Object> rawQuery = Map.of(
        "FROM", user,
        "SELECT", List.of(user.id, user.username.as("userName")),
        "WHERE", Query.and(
            user.id.eq("u1"),
            user.username.startsWith("A")
        ),
        "GROUP_BY", List.of(user.status)
    );
    var result = context.normalizeQuery(rawQuery);

    assertTrue(result.isSuccess(), () -> "as(alias) 生成的 Meta 字段应携带 alias 并可在 raw expression 中归一化: "
        + (result.isFailure() ? result.getCause() : ""));

    SExpression<?> columns = result.get().getSelect().get(0);
    assertEquals("User", result.get().getFrom().getTableName());
    assertEquals("u", result.get().getFrom().getAlias());
    assertEquals(Operators.COLUMNS, columns.getOperator());
    assertEquals(2, columns.getParams().size());
    assertField(columns.getParamAsSExpression(0), "u", "id");

    SExpression<?> fieldAlias = columns.getParamAsSExpression(1);
    assertEquals(Operators.ALIAS, fieldAlias.getOperator());
    assertField(fieldAlias.getParamAsSExpression(0), "u", "username");
    assertEquals("userName", fieldAlias.getParam(1));

    SExpression<Boolean> where = result.get().getWhere();
    assertEquals(Operators.AND, where.getOperator());
    assertEquals(2, where.getParams().size());
    SExpression<?> idCondition = where.getParamAsSExpression(0);
    assertEquals(Operators.EQ, idCondition.getOperator());
    assertField(idCondition.getParamAsSExpression(0), "u", "id");
    assertEquals("u1", idCondition.getParamAsSExpression(1).getParam(0));

    SExpression<?> usernameCondition = where.getParamAsSExpression(1);
    assertEquals(Operators.STARTS_WITH, usernameCondition.getOperator());
    assertField(usernameCondition.getParamAsSExpression(0), "u", "username");
    assertEquals("A", usernameCondition.getParamAsSExpression(1).getParam(0));

    SExpression<?> group = result.get().getGroup();
    assertEquals(Operators.COLUMNS, group.getOperator());
    assertEquals(1, group.getParams().size());
    assertField(group.getParamAsSExpression(0), "u", "status");
  }

  @Test
  void handwrittenMetaShouldOnlyExposeMetadataAndFields() {
    List<String> forbiddenMethods = Arrays.stream(UserMeta.class.getMethods())
        .map(Method::getName)
        .filter(name -> name.equals("execute")
            || name.equals("query")
            || name.equals("repository")
            || name.equals("service"))
        .toList();

    assertTrue(forbiddenMethods.isEmpty());
  }

  private static void assertField(SExpression<?> expression, String... segments) {
    assertEquals(Operators.FIELD, expression.getOperator());
    assertEquals(segments.length, expression.getParams().size());
    for (int i = 0; i < segments.length; i++) {
      assertEquals(segments[i], expression.getParam(i));
    }
  }

  enum UserStatus {
    ENABLED,
    LOCKED
  }

  @Model(fullName = "User")
  static class User {
    @PrimaryKey
    @Field
    private String id;

    @Field
    private String username;

    @Field
    private UserStatus status;
  }

  static final class UserMeta extends TypedModelMeta<User, UserMeta> {
    static final String MODEL_NAME = "User";
    static final String SCHEMA_HASH = "sha256:test";
    static final UserMeta user = new UserMeta("user");

    final StringField<UserMeta> id = stringField("id");
    final StringField<UserMeta> username = stringField("username");
    final EnumField<UserStatus, UserMeta> status = enumField("status", UserStatus.class);

    private UserMeta(String alias) {
      super(MODEL_NAME, User.class, alias);
    }

    @Override
    public UserMeta as(String alias) {
      return new UserMeta(alias);
    }
  }
}
