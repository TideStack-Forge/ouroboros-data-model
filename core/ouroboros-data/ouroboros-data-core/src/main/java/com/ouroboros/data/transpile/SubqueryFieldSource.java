package com.ouroboros.data.transpile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.Expressions;

/**
 * 子查询的 FieldSource 实现
 *
 * <p>从子查询的投影列（SELECT 列表）提取字段信息，
 * 使外层可通过 {@code alias.column} 解析子查询结果列。
 *
 * @since 1.0.0-beta.2
 */
public class SubqueryFieldSource implements FieldSource {
  private final Path<?> alias;
  private final List<Path<?>> fields;

  public SubqueryFieldSource(String aliasName, QueryMetadata queryMetadata) {
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
      return fac.getArgs().stream().flatMap(SubqueryFieldSource::getPaths);
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
