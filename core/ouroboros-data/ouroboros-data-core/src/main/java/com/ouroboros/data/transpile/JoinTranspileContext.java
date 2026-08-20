package com.ouroboros.data.transpile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.querydsl.core.types.Path;

/**
 * JOIN 场景的 TranspileContext
 *
 * <p>职责：
 * <ul>
 *   <li>包装基础 Context</li>
 *   <li>提供 JOIN 表的解析能力</li>
 * </ul>
 *
 * <p>解析顺序：
 * <ol>
 *   <li>先委托给基础 Context，保证主表裸字段语义稳定</li>
 *   <li>若基础 Context 未命中，再在 JOIN 表中按同一优先级查找字段</li>
 *   <li>若多个 JOIN 来源同时命中，则抛出 {@link AmbiguousFieldException}</li>
 * </ol>
 */
public class JoinTranspileContext extends DelegatingTranspileContext {

  private final Map<String, FieldSource> joinTables;

  public JoinTranspileContext(TranspileContext baseContext, Map<String, FieldSource> joinTables) {
    super(baseContext);
    this.joinTables = joinTables;
  }

  @Override
  public Optional<FieldSource> resolveTable(String tableOrAlias) {
    // 先查找 JOIN 表
    FieldSource joinTable = joinTables.get(tableOrAlias);
    if (joinTable != null) {
      return Optional.of(joinTable);
    }

    // 委托给基础 Context
    return getDelegate().resolveTable(tableOrAlias);
  }

  @Override
  public Optional<Path<?>> resolve(String field) {
    Optional<Path<?>> delegatePath = getDelegate().resolve(field);
    if (delegatePath.isPresent()) {
      return delegatePath;
    }

    List<Path<?>> matchedPaths = new ArrayList<>();
    List<String> matchedSources = new ArrayList<>();

    for (Map.Entry<String, FieldSource> entry : joinTables.entrySet()) {
      Optional<Path<?>> path = entry.getValue().getField(field);
      if (path.isPresent()) {
        matchedPaths.add(path.get());
        matchedSources.add(entry.getKey());
      }
    }

    if (matchedPaths.size() > 1) {
      throw new AmbiguousFieldException(field, matchedSources);
    }
    if (matchedPaths.size() == 1) {
      return Optional.of(matchedPaths.get(0));
    }

    return getDelegate().resolve(field);
  }

  @Override
  public Optional<Path<?>> resolve(String tableOrAlias, String field) {
    // 先查找 JOIN 表
    FieldSource joinTable = joinTables.get(tableOrAlias);
    if (joinTable != null) {
      return joinTable.getField(field);
    }

    // 委托给基础 Context
    return getDelegate().resolve(tableOrAlias, field);
  }
}
