package com.ouroboros.data.transpile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.Expressions;

/**
 * CTE (Common Table Expression / WITH clause) 上下文
 *
 * <p>为 WITH 子句提供 CTE 别名解析能力。
 *
 * @since 1.0.0-beta.2
 */
public class CTETranspileContext extends DelegatingTranspileContext {
  private final Map<String, CTEFieldSource> cteMap;

  public CTETranspileContext(TranspileContext delegate) {
    super(delegate);
    this.cteMap = new HashMap<>();
  }

  /**
   * 注册 CTE
   *
   * @param alias         CTE 别名
   * @param queryMetadata CTE 查询的 metadata
   * @return 新的 Context（支持链式调用）
   */
  public CTETranspileContext withCTE(String alias, QueryMetadata queryMetadata) {
    var newContext = new CTETranspileContext(getDelegate());
    newContext.cteMap.putAll(this.cteMap);
    newContext.cteMap.put(alias, new CTEFieldSource(alias, queryMetadata));
    return newContext;
  }

  @Override
  public Optional<Path<?>> resolve(String field) {
    // CTE 不支持无表名的字段解析，委托给 delegate
    return getDelegate().resolve(field);
  }

  @Override
  public Optional<Path<?>> resolve(String tableOrAlias, String field) {
    // 先查 CTE
    var cteSource = cteMap.get(tableOrAlias);
    if (cteSource != null) {
      return cteSource.getField(field);
    }
    // 再委托给 delegate
    return getDelegate().resolve(tableOrAlias, field);
  }

  @Override
  public Optional<FieldSource> resolveTable(String nameOrAlias) {
    // 先查 CTE
    var cteSource = cteMap.get(nameOrAlias);
    if (cteSource != null) {
      return Optional.of(cteSource);
    }
    // 再委托给 delegate
    return getDelegate().resolveTable(nameOrAlias);
  }

  /**
   * CTE 的 FieldSource 实现
   */
  private static class CTEFieldSource implements FieldSource {
    private final Path<?> alias;
    private final List<Path<?>> fields;

    public CTEFieldSource(String aliasName, QueryMetadata queryMetadata) {
      this.alias = Expressions.simplePath(Object.class, aliasName);
      this.fields = getPaths(queryMetadata.getProjection())
          .map(f -> Expressions.simplePath(f.getType(), alias, f.getMetadata().getName()))
          .collect(Collectors.toList());
    }

    private static Stream<Path<?>> getPaths(Expression<?> expr) {
      if (expr instanceof Path) {
        return Stream.of((Path<?>) expr);
      }
      if (expr instanceof Operation<?> op && op.getOperator() == Ops.ALIAS) {
        return getPaths(op.getArg(1));
      }
      if (expr instanceof FactoryExpression<?> fac) {
        return fac.getArgs().stream().flatMap(CTEFieldSource::getPaths);
      }
      return Stream.empty();
    }

    @Override
    public Optional<Path<?>> getField(String fieldName) {
      return fields.stream()
          .filter(p -> p.getMetadata().getName().equals(fieldName))
          .findFirst()
          .map(p -> FieldSource.transformParent(p, alias));
    }

    @Override
    public List<Path<?>> getFields() {
      return fields;
    }

    @Override
    public Path<?> getSelfPath() {
      return alias;
    }
  }
}
