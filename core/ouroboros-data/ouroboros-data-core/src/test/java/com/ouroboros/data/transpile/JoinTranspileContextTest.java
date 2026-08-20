package com.ouroboros.data.transpile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;

class JoinTranspileContextTest {

  private DummyTranspileContext baseContext;
  private FieldSource joinUserSource;
  private FieldSource joinDeptSource;
  private Path<?> baseUserIdPath;
  private Path<?> joinUserNamePath;
  private Path<?> joinDeptNamePath;

  @BeforeEach
  void setUp() {
    baseContext = new DummyTranspileContext();

    FieldSource baseSource = mock(FieldSource.class);
    baseUserIdPath = Expressions.numberPath(Long.class, "userId");
    doReturn("main").when(baseSource).getName();
    doReturn(Optional.of(baseUserIdPath)).when(baseSource).getField("userId");
    doReturn(Optional.empty()).when(baseSource).getField("name");
    baseContext.withTable("main", baseSource);

    joinUserSource = mock(FieldSource.class);
    joinUserNamePath = Expressions.stringPath("userName");
    doReturn("u").when(joinUserSource).getName();
    doReturn(Optional.of(joinUserNamePath)).when(joinUserSource).getField("name");
    doReturn(Optional.empty()).when(joinUserSource).getField("userId");

    joinDeptSource = mock(FieldSource.class);
    joinDeptNamePath = Expressions.stringPath("deptName");
    doReturn("d").when(joinDeptSource).getName();
    doReturn(Optional.of(joinDeptNamePath)).when(joinDeptSource).getField("name");
    doReturn(Optional.empty()).when(joinDeptSource).getField("userId");
  }

  @Test
  void testResolveFieldReturnsJoinMatchWhenUnique() {
    JoinTranspileContext context = new JoinTranspileContext(baseContext, mapOf("u", joinUserSource));

    Optional<Path<?>> resolved = context.resolve("name");

    assertSame(joinUserNamePath, resolved.orElse(null));
  }

  @Test
  void testResolveFieldThrowsWhenJoinSourcesAreAmbiguous() {
    JoinTranspileContext context = new JoinTranspileContext(baseContext, mapOf(
        "u", joinUserSource,
        "d", joinDeptSource
    ));

    AmbiguousFieldException ex = assertThrows(AmbiguousFieldException.class, () -> context.resolve("name"));
    assertEquals("name", ex.getFieldName());
    assertEquals(2, ex.getMatchedSources().size());
    assertEquals("u", ex.getMatchedSources().get(0));
    assertEquals("d", ex.getMatchedSources().get(1));
  }

  @Test
  void testResolveExplicitTableBypassesAmbiguity() {
    JoinTranspileContext context = new JoinTranspileContext(baseContext, mapOf(
        "u", joinUserSource,
        "d", joinDeptSource
    ));

    Optional<Path<?>> resolved = context.resolve("d", "name");

    assertSame(joinDeptNamePath, resolved.orElse(null));
  }

  @Test
  void testResolveFieldFallsBackToDelegateWhenJoinTablesDoNotMatch() {
    JoinTranspileContext context = new JoinTranspileContext(baseContext, mapOf("u", joinUserSource));

    Optional<Path<?>> resolved = context.resolve("userId");

    assertSame(baseUserIdPath, resolved.orElse(null));
  }

  @Test
  void testResolveFieldPrefersDelegateWhenBaseAndJoinBothMatch() {
    FieldSource collidingJoinSource = mock(FieldSource.class);
    Path<?> joinUserIdPath = Expressions.numberPath(Long.class, "joinUserId");
    doReturn("u").when(collidingJoinSource).getName();
    doReturn(Optional.of(joinUserIdPath)).when(collidingJoinSource).getField("userId");

    JoinTranspileContext context = new JoinTranspileContext(baseContext, mapOf("u", collidingJoinSource));

    Optional<Path<?>> resolved = context.resolve("userId");

    assertSame(baseUserIdPath, resolved.orElse(null),
        "主表字段与 JOIN 字段重名时，应优先解析为 delegate/main 上下文中的字段");
  }

  private Map<String, FieldSource> mapOf(String firstAlias, FieldSource firstSource) {
    Map<String, FieldSource> joinTables = new LinkedHashMap<>();
    joinTables.put(firstAlias, firstSource);
    return joinTables;
  }

  private Map<String, FieldSource> mapOf(
      String firstAlias, FieldSource firstSource,
      String secondAlias, FieldSource secondSource) {
    Map<String, FieldSource> joinTables = new LinkedHashMap<>();
    joinTables.put(firstAlias, firstSource);
    joinTables.put(secondAlias, secondSource);
    return joinTables;
  }
}
