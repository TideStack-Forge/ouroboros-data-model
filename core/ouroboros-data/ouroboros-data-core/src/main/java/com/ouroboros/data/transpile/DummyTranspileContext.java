package com.ouroboros.data.transpile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.util.DataServices;
import com.querydsl.core.types.Path;

/**
 * 测试用 TranspileContext 实现
 *
 * <p>替代 DummyPathResolver，用于单元测试和集成测试。
 *
 * @since 1.0.0-beta.2
 */
public class DummyTranspileContext implements TranspileContext {

  private final Map<String, FieldSource> tables = new HashMap<>();

  /**
   * 添加表/别名
   *
   * @param alias  表别名
   * @param source FieldSource
   * @return this（支持链式调用）
   */
  public DummyTranspileContext withTable(String alias, FieldSource source) {
    tables.put(alias, source);
    return this;
  }

  @Override
  public Optional<Path<?>> resolve(String field) {
    // 在所有表中查找
    for (FieldSource source : tables.values()) {
      Optional<Path<?>> path = source.getField(field);
      if (path.isPresent()) {
        return path;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Path<?>> resolve(String tableOrAlias, String field) {
    FieldSource source = tables.get(tableOrAlias);
    if (source != null) {
      return source.getField(field);
    }
    return Optional.empty();
  }

  @Override
  public Optional<FieldSource> resolveTable(String nameOrAlias) {
    return Optional.ofNullable(tables.get(nameOrAlias));
  }

  @Override
  public Optional<SExpressionTranspiler> getSExpressionTranspiler(SExpression<?> expr) {
    return DataServices.getCachedReversedServiceStream(SExpressionTranspiler.class)
        .filter(t -> t.support(expr))
        .findFirst();
  }
}
